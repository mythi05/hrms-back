package com.example.hrms.controller;

import com.example.hrms.entity.Document;
import com.example.hrms.service.DocumentService;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;

@RestController
@RequestMapping("/api/documents")
@CrossOrigin(origins = "*")
public class DocumentController {

    private final DocumentService service;

    public DocumentController(DocumentService service) {
        this.service = service;
    }

    // ===== LIST + SEARCH =====
    @GetMapping
    public List<Document> getDocuments(
            @RequestParam(defaultValue = "all") String category,
            @RequestParam(defaultValue = "") String search
    ) {
        return service.getDocuments(category, search);
    }

    // ===== UPLOAD =====
    @PostMapping("/upload")
    public Document upload(
            @RequestParam MultipartFile file,
            @RequestParam String category,
            @RequestParam String uploadedBy,
            @RequestParam(required = false, defaultValue = "PUBLIC") String visibility
    ) throws IOException {
        return service.upload(file, category, uploadedBy, visibility);
    }

    // ===== DOWNLOAD =====
@GetMapping("/{id}/download")
public ResponseEntity<Resource> download(@PathVariable Long id) {
    Document doc = service.getDocument(id);
    Path filePath = Paths.get(doc.getFilePath());
    Resource resource = new FileSystemResource(filePath);

    String contentType = "application/octet-stream";
    try {
        contentType = Files.probeContentType(filePath);
    } catch (IOException ignored) {}

    return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(contentType))
            .header(HttpHeaders.CONTENT_DISPOSITION,
                    "attachment; filename=\"" + doc.getName() + "\"")
            .body(resource);
}
    // ===== VIEW INLINE OR ATTACHMENT =====
@GetMapping("/{id}/view")
public ResponseEntity<Resource> view(@PathVariable Long id) {
    Document doc = service.getDocument(id);
    Path filePath = Paths.get(doc.getFilePath());
    Resource resource = new FileSystemResource(filePath);

    String contentType = "application/octet-stream";
    try {
        contentType = Files.probeContentType(filePath);
    } catch (IOException ignored) {}

    String disposition = contentType.startsWith("application/pdf")
            || contentType.startsWith("image/")
            ? "inline"
            : "attachment";

    return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(contentType))
            .header(HttpHeaders.CONTENT_DISPOSITION,
                    disposition + "; filename=\"" + doc.getName() + "\"")
            .body(resource);
}
    // ===== DELETE =====
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) throws IOException {
        service.delete(id);
        return ResponseEntity.ok().build();
    }

    // ===== STATS =====
    @GetMapping("/stats")
    public Map<String, Object> stats() {
        return service.getStats();
    }
}
