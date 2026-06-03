package com.atherion.andromeda.controllers;

import com.atherion.andromeda.services.RagIngestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/rag")
@RequiredArgsConstructor
public class RagController {

    private final RagIngestionService ragIngestionService;

    @PostMapping("/ingest")
    public ResponseEntity<Map<String, Object>> ingest(
            @RequestParam(required = false) Long projectId) {
        int indexed = ragIngestionService.ingest(projectId);
        return ResponseEntity.ok(Map.of(
                "indexed", indexed,
                "projectId", projectId != null ? projectId : "all"
        ));
    }
}
