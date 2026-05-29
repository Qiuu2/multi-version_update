---
name: data-integration-skill
description: Data-Integration Agent 的技能配置 — Retrofit / 拦截器 / AuthStore / Repository / 迁移。
version: 1.0.0
---

# Data-Integration Agent — Skill

## 1. NetworkModule（Hilt）

```kotlin
// di/NetworkModule.kt
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides @Singleton
    fun provideOkHttpClient(
        authInterceptor: AuthInterceptor,
        baseUrlInterceptor: DynamicBaseUrlInterceptor,
        @ApplicationContext ctx: Context
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(baseUrlInterceptor)   // 第 1 步：改 URL
            .addInterceptor(authInterceptor)       // 第 2 步：加 Auth
            .apply {
                if (BuildConfig.DEBUG) {
                    addInterceptor(HttpLoggingInterceptor().apply {
                        level = HttpLoggingInterceptor.Level.BODY
                    })
                }
            }
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl("http://placeholder.invalid/")   // 关键：占位 baseUrl
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
    }

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    @Provides @Singleton
    fun provideTerminalApi(retrofit: Retrofit): TerminalApi = retrofit.create()

    // ... 其他 ApiService
}
```

## 2. DynamicBaseUrlInterceptor

```kotlin
// data/network/DynamicBaseUrlInterceptor.kt
@Singleton
class DynamicBaseUrlInterceptor @Inject constructor(
    private val authStore: AuthStore
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()

        val newUrl = if (request.url.host == "placeholder.invalid") {
            val serverAddr = runBlocking {
                authStore.serverAddressFlow.first()   // 阻塞读取保证一致性
            } ?: throw ServerAddressNotConfiguredException()
            request.url.newBuilder()
                .scheme("http")   // 明文 HTTP（已知约束）
                .host(serverAddr.host)
                .port(serverAddr.port)
                .build()
        } else request.url

        return chain.proceed(request.newBuilder().url(newUrl).build())
    }
}

class ServerAddressNotConfiguredException : IOException("Server address not configured")
```

## 3. AuthInterceptor + Refresh

```kotlin
// data/network/AuthInterceptor.kt
@Singleton
class AuthInterceptor @Inject constructor(
    private val authStore: AuthStore
) : Interceptor {

    private val refreshMutex = Mutex()

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()

        // Login 请求不附 token
        if (request.url.encodedPath.contains("/authorizations")) {
            return chain.proceed(request)
        }

        val jwt = runBlocking { authStore.jwtFlow.first() }
        val authedRequest = request.newBuilder()
            .header("Authorization", "Bearer $jwt")
            .build()

        val response = chain.proceed(authedRequest)

        // 401 → 刷新 → 重试一次
        if (response.code == 401) {
            response.close()
            val refreshed = runBlocking { refreshTokenSafely() }
            if (refreshed) {
                val newJwt = runBlocking { authStore.jwtFlow.first() }
                return chain.proceed(
                    request.newBuilder()
                        .header("Authorization", "Bearer $newJwt")
                        .build()
                )
            } else {
                // 刷新失败 → 触发登出
                runBlocking { authStore.clearLogin() }
            }
        }

        return response
    }

    private suspend fun refreshTokenSafely(): Boolean = refreshMutex.withLock {
        // 双重检查：是否已被其他线程刷新
        val currentJwt = authStore.jwtFlow.first()
        if (isTokenStillValid(currentJwt)) return true   // 已被其他线程刷新

        authStore.refreshJwt().isSuccess
    }

    private fun isTokenStillValid(jwt: String?): Boolean {
        // 解析 JWT exp claim，判断是否未过期
        return false   // 简化
    }
}
```

## 4. AuthStore

```kotlin
// data/auth/AuthStore.kt
interface AuthStore {
    val serverAddressFlow: StateFlow<ServerAddress?>
    val jwtFlow: StateFlow<String?>
    val refreshTokenFlow: StateFlow<String?>
    val accountFlow: StateFlow<String?>

    suspend fun saveLogin(addr: ServerAddress, account: String, jwt: String, refresh: String)
    suspend fun clearLogin()
    suspend fun refreshJwt(): Result<String>
}

@Singleton
class AuthStoreImpl @Inject constructor(
    @ApplicationContext ctx: Context,
    private val authApi: AuthApi
) : AuthStore {

    private val masterKey = MasterKey.Builder(ctx)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val secureSP = EncryptedSharedPreferences.create(
        ctx, "auth_secure", masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    private val normalSP = ctx.getSharedPreferences("auth_normal", Context.MODE_PRIVATE)

    private val _serverAddress = MutableStateFlow<ServerAddress?>(loadServerAddress())
    override val serverAddressFlow = _serverAddress.asStateFlow()

    private val _jwt = MutableStateFlow<String?>(secureSP.getString("jwt", null))
    override val jwtFlow = _jwt.asStateFlow()

    private val _refresh = MutableStateFlow<String?>(secureSP.getString("refresh", null))
    override val refreshTokenFlow = _refresh.asStateFlow()

    private val _account = MutableStateFlow<String?>(normalSP.getString("account", null))
    override val accountFlow = _account.asStateFlow()

    override suspend fun saveLogin(
        addr: ServerAddress, account: String, jwt: String, refresh: String
    ) {
        normalSP.edit()
            .putString("server_host", addr.host)
            .putInt("server_port", addr.port)
            .putString("account", account)
            .apply()
        secureSP.edit()
            .putString("jwt", jwt)
            .putString("refresh", refresh)
            .apply()
        _serverAddress.value = addr
        _account.value = account
        _jwt.value = jwt
        _refresh.value = refresh
    }

    override suspend fun clearLogin() {
        secureSP.edit().clear().apply()
        // 保留 server_host 和 account 方便重新登录
        _jwt.value = null
        _refresh.value = null
    }

    override suspend fun refreshJwt(): Result<String> {
        val currentRefresh = _refresh.value ?: return Result.failure(IllegalStateException("No refresh token"))
        return runCatching {
            val response = authApi.refresh(currentRefresh)
            secureSP.edit().putString("jwt", response.jwt).apply()
            _jwt.value = response.jwt
            response.jwt
        }
    }

    private fun loadServerAddress(): ServerAddress? {
        val host = normalSP.getString("server_host", null) ?: return null
        val port = normalSP.getInt("server_port", 0).takeIf { it > 0 } ?: return null
        return ServerAddress(host, port)
    }
}

data class ServerAddress(val host: String, val port: Int) {
    companion object {
        fun parse(input: String): Result<ServerAddress> = runCatching {
            // 支持 "192.168.1.1:8080" 或 "192.168.1.1"（默认 80）
            val parts = input.trim().split(":")
            val host = parts[0]
            val port = parts.getOrNull(1)?.toInt() ?: 80
            require(host.isNotBlank())
            require(port in 1..65535)
            ServerAddress(host, port)
        }
    }
}
```

## 5. 21 个 REST 端点 ApiService 模板

```kotlin
// data/api/TerminalApi.kt
interface TerminalApi {
    @GET("/terminal/terminalinfo")
    suspend fun getAllTerminals(): List<TerminalDto>

    @GET("/terminal/zoneterminal")
    suspend fun getAllZones(): List<ZoneDto>

    @GET("/terminal/terzone")
    suspend fun getTerminalsByZone(@Query("zoneId") zoneId: String): List<TerminalDto>

    @POST("/terminal/urgentplay")
    suspend fun urgentPlay(@Body request: UrgentPlayRequest): UrgentPlayResponse

    // ... 更多端点（按 constant/Constant.java 完整列出）
}

// data/api/AuthApi.kt
interface AuthApi {
    @POST  // 注意：登录用 @Url 因为此时 serverAddress 尚未存入
    suspend fun login(@Url url: String, @Body request: LoginRequest): LoginResponse

    @POST("/authorizations/refresh")
    suspend fun refresh(@Body refreshToken: String): RefreshResponse
}

// data/api/TaskApi.kt  (5 个 /task/* 端点)
interface TaskApi { /* ... */ }

// data/api/ServerApi.kt
interface ServerApi {
    @GET("/server/serverstate")
    suspend fun getServerState(): ServerStateDto
}
```

## 6. Repository 模板

```kotlin
// data/repository/TerminalRepository.kt
interface TerminalRepository {
    fun observeZones(): Flow<List<ZoneDto>>
    fun observeTerminals(): Flow<List<TerminalDto>>
    suspend fun refreshAll(): Result<Unit>
    suspend fun urgentPlay(terminalIds: List<String>, mediaId: String): Result<Unit>
}

@Singleton
class TerminalRepositoryImpl @Inject constructor(
    private val terminalApi: TerminalApi,
    private val terminalDao: TerminalDao
) : TerminalRepository {

    override fun observeZones(): Flow<List<ZoneDto>> {
        return terminalDao.observeAllZones().map { entities ->
            entities.map { it.toDto() }
        }
    }

    override fun observeTerminals(): Flow<List<TerminalDto>> {
        return terminalDao.observeAllTerminals().map { entities ->
            entities.map { it.toDto() }
        }
    }

    override suspend fun refreshAll(): Result<Unit> = runCatching {
        val zones = terminalApi.getAllZones()
        val terminals = terminalApi.getAllTerminals()
        terminalDao.replaceZones(zones.map { it.toEntity() })
        terminalDao.replaceTerminals(terminals.map { it.toEntity() })
    }

    override suspend fun urgentPlay(
        terminalIds: List<String>, mediaId: String
    ): Result<Unit> = runCatching {
        terminalApi.urgentPlay(UrgentPlayRequest(terminalIds, mediaId))
    }
}
```

## 7. 旧栈迁移流程

```yaml
migration_workflow:
  step_1_audit:
    - "列出 httptask/*Method.java 全部文件"
    - "对每个文件，找出调用方"
    - "标注是否在 v4 仍需使用"

  step_2_per_module:
    - "选定要迁移的 module（如：终端相关）"
    - "用 Retrofit 重写所有 endpoint"
    - "添加 Repository"
    - "更新调用方为 Repository"
    - "保留旧栈 24h 验证一致性（dual-run）"
    - "删除旧栈文件"
    - "Commit + PR + ICD 更新"

  step_3_verify:
    - "新栈调用日志对比旧栈日志（dual-run 期间）"
    - "无差异 → 删除旧栈"
    - "有差异 → 调查根因 → 修复 → 再 dual-run"

  forbidden:
    - "同一 endpoint 在新旧栈都被调用（指向不同 OkHttpClient）"
    - "迁移到一半的 module（必须整体）"
    - "未删除旧栈直接 commit 新栈（必须配套）"
```

## 8. Room 缓存

```kotlin
// data/db/AeroDatabase.kt
@Database(
    entities = [ZoneEntity::class, TerminalEntity::class, TaskEntity::class],
    version = 1
)
abstract class AeroDatabase : RoomDatabase() {
    abstract fun terminalDao(): TerminalDao
    abstract fun taskDao(): TaskDao
}
```

## 9. 提交流程

```yaml
submission_workflow:
  before_submit:
    - "OkHttpClient 单例（grep newBuilder() 检查）"
    - "拦截器顺序正确（baseUrl → auth → logging）"
    - "DTO @SerialName 与后端对齐（人工对比）"
    - "AuthStore 单元测试覆盖：保存、清除、刷新、并发刷新"
    - "Repository 单元测试覆盖：缓存 + 网络一致性"
    - "ICD 更新到 references/icd-contracts.md"
    - "若改 httptask：通知 PM 协调 legacy-native"
```

---

*Data-Integration Agent Skill v1.0.0*
