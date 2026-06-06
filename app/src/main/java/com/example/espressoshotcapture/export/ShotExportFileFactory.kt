package com.example.espressoshotcapture.export

import com.example.espressoshotcapture.capture.domain.ShotDraft

object ShotExportFileFactory {
    fun create(shotDraft: ShotDraft): ShotExportFile =
        ShotExportFile(
            fileName = "shot-${shotDraft.id}.json",
            mimeType = "application/json",
            content = ShotDraftJsonExporter.export(shotDraft)
        )
}
