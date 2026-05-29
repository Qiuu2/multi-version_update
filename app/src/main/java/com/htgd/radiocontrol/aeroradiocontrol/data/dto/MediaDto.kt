package com.htgd.radiocontrol.aeroradiocontrol.data.dto

import com.google.gson.annotations.SerializedName

/**
 * Wire DTOs for the media (点播 media library) list endpoints, mirroring the
 * legacy server payload (ICD-MediaDto-v1 §12 LIVE).
 *
 * Envelope: both endpoints return `{ "data": [ ... ] }` (AR-001 §10.3), same
 * shape as the terminal/serverstate endpoints. Gson with `setLenient()`
 * (NetworkModule.provideGson) tolerates unknown/extra fields → no crash on a
 * field the backend adds (R-003).
 *
 * Field source (REAL contract under Plan A, v3 跑通即真值):
 *   - MediaDto         ←1:1← v3 `model.responseModel.MusicInfoModel`
 *   - MediaFolderDto   ←1:1← v3 `model.responseModel.MusicFolderInfoModel`
 * the POJOs `MusicInfosRsp` / `MusicFolderInfosRsp` deserialize into. All fields
 * nullable so a partial/renamed payload degrades to null rather than NPE-ing.
 *
 * NOTE: this covers the LIST half only (selection). The 点播 CAST action is the
 * HTIntf AAR control plane (legacy-native's cast-seam), NOT a REST endpoint, so
 * there is no request DTO here. See V3MediaRepository / PA-07 deliverable.
 */

/** `{ "data": [ MediaDto ] }` — response of GET /terminal/mediainfo (all media). */
data class MediaEnvelopeDto(
    @SerializedName("data") val data: List<MediaDto>? = null,
)

/** `{ "data": [ MediaFolderDto ] }` — response of GET /terminal/mediafolderinfo. */
data class MediaFolderEnvelopeDto(
    @SerializedName("data") val data: List<MediaFolderDto>? = null,
)

/**
 * One media file. Mirrors legacy `MusicInfoModel`.
 *
 * `isFile` is a String in v3 ("true"/"false" — folder rows are reused in the same
 * tree model); the media-list endpoint returns real files. [folderId] links the
 * file to its [MediaFolderDto]. All nullable for safety.
 */
data class MediaDto(
    @SerializedName("mediaid") val mediaId: Int? = null,
    @SerializedName("folderid") val folderId: Int? = null,
    @SerializedName("name") val name: String? = null,
    @SerializedName("format") val format: String? = null,
    @SerializedName("size") val size: Int? = null,
    @SerializedName("bitrate") val bitrate: Int? = null,
    @SerializedName("length") val length: Int? = null,
    @SerializedName("isFile") val isFile: String? = null,
)

/**
 * One media folder. Mirrors legacy `MusicFolderInfoModel`.
 *
 * [parentId] forms the tree (root = id 3, Constant.FALG_MUSIC, "点播媒体库").
 * `count`/`all` are the folder's media counts the v4 library header can show.
 */
data class MediaFolderDto(
    @SerializedName("folderid") val folderId: Int? = null,
    @SerializedName("parentid") val parentId: Int? = null,
    @SerializedName("name") val name: String? = null,
    @SerializedName("count") val count: Int? = null,
    @SerializedName("all") val all: Int? = null,
    @SerializedName("state") val state: Int? = null,
)
