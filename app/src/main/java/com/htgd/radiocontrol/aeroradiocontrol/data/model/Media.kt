package com.htgd.radiocontrol.aeroradiocontrol.data.model

/**
 * Domain model for the 点播 (cast) media library, as the broadcast Tab's media
 * picker consumes it (ICD-MediaRepository-v1).
 *
 * Repository OUTPUT types — distinct from the wire `MediaDto` / `MediaFolderDto`.
 * [MediaFolder] nests its [media] (consistent with [Zone] nesting terminals —
 * fe's preference, saves a combine in the ViewModel). The tree is built from the
 * wire's folderid/parentid (root = id "3", Constant.FALG_MUSIC).
 *
 * This is the LIST/selection half only. Casting the chosen media to terminals is
 * the HTIntf AAR cast-seam (legacy-native), not part of this contract.
 */
data class Media(
    val id: String,
    val name: String,
    val folderId: String,
    val format: String? = null,
    val durationSeconds: Int? = null,
    val sizeBytes: Int? = null,
)

/**
 * One media folder, with its nested [media]. Mirrors the legacy folder tree
 * (folderid/parentid). [parentId] is null for the root folder.
 */
data class MediaFolder(
    val id: String,
    val name: String,
    val parentId: String? = null,
    val media: List<Media> = emptyList(),
)
