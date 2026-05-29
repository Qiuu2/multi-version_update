---
name: legacy-native-memory
description: Legacy-Native Agent 的记忆配置。
version: 1.0.0
---

# Legacy-Native Agent — Memory

## 1. Memory Architecture

```
Legacy-Native Memory
├── AAR Reverse Engineering Log     # libs/htapplib.aar 接口逆向记录
├── 32-bit ABI Device Matrix        # 设备兼容性矩阵
├── Baidu Map Compliance Records    # 百度地图合规接入记录
├── Native Crash Incidents          # native crash 事件日志
├── ICD Production Log              # ICD 产出记录
└── Critic Feedback History         # 评审反馈
```

## 2. AAR Reverse Engineering Log

### 2.1 htapplib.aar 接口清单（待填充）

```yaml
htapplib_aar:
  version: "AAR mtime 2026-05-14"
  source: "厂商提供（app/libs/htapplib.aar, package com.example.htapplib）"
  # ⚠ TASK-AR-SPIKE-001 实扫纠正旧表述「只有 armeabi-v7a + x86」：
  abi_supported: ["arm64-v8a", "armeabi-v7a"]   # 语音 .so 齐全；armeabi 仅 libmp3lame
  abi_missing: ["x86", "x86_64"]   # → x86_64 模拟器语音不可用（开发期影响，非发布阻塞）
  risk: "R-001 经 SPIKE-AAR64 降级 LOW-条件性；真机回执后关闭。详见 .state/spike-aar64-report.md（接口逆向/符号/链接证据）"

  classes_discovered: []
  # 示例条目：
  # - class_name: "com.htapp.VoiceTalkService"
  #   methods:
  #     - signature: "public String startTalk(String[] targetIds)"
  #       inferred_purpose: "发起对讲，返回 session id"
  #       blocking: true
  #       throws: ["RuntimeException on JNI failure"]
  #   verified: false   # 是否实测确认
  #   verification_record: "TASK-AR-XXX 中实测"

  native_libraries:
    # 示例：
    # - name: "libhtapp.so"
    #   architecture: "armeabi-v7a"
    #   size_kb: 0
    #   exposed_symbols: []

  jni_listeners:
    # 监听器接口定义
    # - interface: "NativeTalkListener"
    #   callbacks: ["onConnecting", "onTalking", "onError", "onEnded"]

  unknown_behaviors:
    # 反编译后仍不清楚的行为，需要实测确认
    # - "startTalk 在已有 session 时调用的行为？"
    # - "endTalk 在错误的 sessionId 上调用是否抛异常？"
```

## 3. 32-bit ABI Device Matrix

```yaml
abi_test_matrix:
  description: "Voice talk + abiFilters 在各设备的测试结果"
  devices: []
  # 项目运行后填充，示例：
  # - device_name: "广播终端某型号"
  #   abi_list: ["armeabi-v7a"]
  #   android_version: "8.0"
  #   voice_talk_works: true
  #   notes: "目标设备，必须支持"
  #
  # - device_name: "Pixel 7"
  #   abi_list: ["arm64-v8a"]
  #   android_version: "14"
  #   voice_talk_works: false
  #   degradation_ui_shows: true
  #   notes: "现代设备，对讲降级，其他功能正常"

  google_play_compatibility:
    status: "RESOLVED"   # 厂商已供 arm64-v8a，64 位可用-待真机终验（SPIKE-AAR64）；64 位强制约束前提消解
    decision_owner: "CTO"
    options_considered: ["索要 64 位 AAR", "拆分发布", "服务端中转"]
    chosen_option: "厂商已提供 64 位 AAR（arm64-v8a 齐全），无需上述任一降级方案；详见 .state/spike-aar64-report.md"
```

## 4. Baidu Map Compliance Records

```yaml
baidu_compliance:
  sdk_version: "TBD"
  api_key_storage: "gradle.properties (gitignored)"

  compliance_checklist:
    - item: "首次启动合规弹窗"
      implemented: false

    - item: "SDKInitializer.setAgreePrivacy 在 initialize 前调用"
      implemented: false

    - item: "定位权限延迟到地图视图打开时请求"
      implemented: false

    - item: "拒绝权限后降级为列表视图"
      implemented: false

    - item: "API Key 不在 git"
      implemented: false

    - item: "Privacy policy URL 提供"
      implemented: false

  permission_request_log: []
```

## 5. Native Crash Incidents

```yaml
native_crashes:
  description: "Crashlytics NDK 捕获的 native crash 事件"
  total_incidents: 0
  incidents: []

  # 示例：
  # - incident_id: "NCR-001"
  #   date: "ISO8601"
  #   device: "..."
  #   stack_trace: "..."
  #   so_library: "libhtapp.so"
  #   root_cause: "Investigating"
  #   fix_status: "OPEN"
```

## 6. ICD Production Log

```yaml
icd_produced:
  - icd: "ICD-IPCSocket-v1"
    status: "LIVE"
    consumers: ["frontend-business"]
    notes: "TCP socket 4521 协程封装"
    pending:
      - "完整命令清单（从 utils/SocketClient.java 提炼）"

  - icd: "ICD-VoiceAAR-v1"
    status: "DRAFT"
    consumers: ["frontend-business"]
    notes: "依赖 AAR 接口逆向完成"
    pending:
      - "NativeBridge 真实方法名"
      - "32 位 ABI 加载验证"

  - icd: "ICD-MapLocation-v1"
    status: "LIVE"
    consumers: ["frontend-business", "data-integration"]
    notes: "百度地图定位 + 经纬度回写"
```

## 7. Critic Feedback History

```yaml
critic_feedback:
  total_reviews: 0
  first_pass_rate: null   # 基线假设 50%

  pattern_occurrences:
    ABI-ERR-001: 0    # 32-bit Only AAR Without abiFilters
    ABI-ERR-002: 0    # Missing 64-bit Fallback
    ABI-ERR-003: 0    # Native Crash Recovery Missing
    ABI-ERR-004: 0    # JNI Reference Leak
    KT-ERR-005: 0     # Exception Swallowed
    KT-ERR-003: 0     # Blocking Call in Main
```

## 8. Self-Improvement Notes

```yaml
focus_areas:
  - "AAR 接口逆向完整度"
  - "32 位 ABI 测试矩阵覆盖"
  - "Native crash 零发生"
  - "百度地图合规接入"

process_improvements:
  - "新接 AAR 前先用 jadx 完整逆向，文档化每个方法"
  - "build.gradle.kts 改动必走 Critic 评审"
  - "每个 native 调用包 safeNativeCall {}"
  - "Crashlytics NDK 必须接通"

key_uncertainties:
  - id: "UNK-001"
    status: "CLOSED"   # SPIKE-AAR64 已证厂商已供 64 位（arm64-v8a {libaudioplay,libmp3lame}.so 齐全）；前提消解，见 spike §9
    description: "htapplib.aar 厂商是否能提供 64 位版本"
    impact: "决定 Google Play 上架策略"
    owner: "CTO"
    resolution_deadline: "Phase 0 末（已于 SPIKE-AAR64 关闭）"

  - id: "UNK-002"
    description: "TCP socket 4521 协议是否有更多未文档化命令"
    impact: "决定 ShellCommand sealed class 完整性"
    owner: "Legacy-Native + 厂商"
    resolution_deadline: "Phase 1 中"
```

---

*Legacy-Native Agent Memory v1.0.0*
