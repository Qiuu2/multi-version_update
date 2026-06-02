package com.htgd.radiocontrol.aeroradiocontrol.data.repository

import com.htgd.radiocontrol.aeroradiocontrol.data.dto.MediaDto
import com.htgd.radiocontrol.aeroradiocontrol.data.dto.MediaFolderDto
import com.htgd.radiocontrol.aeroradiocontrol.data.model.Media
import com.htgd.radiocontrol.aeroradiocontrol.data.model.MediaFolder

/**
 * DTO -> domain mappers for the media domain.
 *
 * Kept separate from the Repository (like [TerminalMapper] / [ServerStateMapper])
 * so the wire->domain translation is unit-testable in isolation. A DTO missing
 * its id is dropped (a media/folder with no identity is unusable) — the Repository
 * filters nulls out and attaches each folder's media after mapping.
 */

/** Root folder id (Constant.FALG_MUSIC = 3, "点播媒体库"). A folder whose parent
 *  is the root — or which has no parent — surfaces as a top-level folder. */
private const val ROOT_FOLDER_ID = 3

/** Maps a [MediaDto] to a [Media], or null if it has no usable mediaid. */
fun MediaDto.toMediaOrNull(): Media? {
    val realId = mediaId ?: return null
    return Media(
        id = realId.toString(),
        name = name.orEmpty(),
        folderId = folderId?.toString().orEmpty(),
        format = format,
        durationSeconds = length,
        sizeBytes = size,
    )
}

/**
 * Maps a [MediaFolderDto] to a [MediaFolder] (without media), or null if it has
 * no usable folderid. The Repository attaches each folder's media after mapping.
 *
 * [MediaFolder.parentId] is null when the wire parent is the root (id 3) or
 * absent, so the root sits at the top of the tree fe renders.
 */
fun MediaFolderDto.toMediaFolderOrNull(): MediaFolder? {
    val realId = folderId ?: return null
    return MediaFolder(
        id = realId.toString(),
        name = name.orEmpty(),
        parentId = parentId
            ?.takeIf { it != ROOT_FOLDER_ID && it != 0 }
            ?.toString(),
        media = emptyList(),
    )
}
