package com.example.hrms.service;

import com.example.hrms.entity.Document;
import com.example.hrms.repository.DocumentRepository;
import org.springframework.core.io.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

@Service
public class DocumentService {

    private final DocumentRepository repository;
    private final Path uploadDir = Paths.get("uploads/documents");

    public DocumentService(DocumentRepository repository) {
        this.repository = repository;
    }

    // ===== GET LIST =====
    public List<Document> getDocuments(String category, String search) {
        List<Document> docs = repository.filterDocuments(category, search);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = null;
        List<String> authorities = List.of();
        if (auth != null && auth.isAuthenticated()) {
            username = auth.getName();
            authorities = auth.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority).toList();
        }

        final String user = username;
        final List<String> userAuthorities = authorities;

        return docs.stream().filter(d -> {
            String vis = d.getVisibility();
            if (vis == null) return true;
            vis = vis.trim();
            if (vis.equalsIgnoreCase("PUBLIC")) return true;
            if (vis.startsWith("ROLE_")) {
                return userAuthorities.contains(vis);
            }
            if (vis.startsWith("USER:")) {
                String u = vis.substring("USER:".length());
                return user != null && user.equals(u);
            }
            return true;
        }).toList();
    }

    // ===== UPLOAD =====
    public Document upload(MultipartFile file, String category, String uploadedBy, String visibility) throws IOException {

        if (!Files.exists(uploadDir)) {
            Files.createDirectories(uploadDir);
        }

        String filename = UUID.randomUUID() + "_" + file.getOriginalFilename();
        Path filePath = uploadDir.resolve(filename);

        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        Document doc = new Document();
        doc.setName(file.getOriginalFilename());
        doc.setFilePath(filePath.toString());
        doc.setFileSize(file.getSize());
        doc.setCategory(category);
        doc.setUploadedBy(uploadedBy);
        doc.setFileType(detectType(file.getOriginalFilename()));
        if (visibility == null || visibility.isBlank()) {
            doc.setVisibility("PUBLIC");
        } else {
            doc.setVisibility(visibility);
        }
        doc.setIsNew(true);

        return repository.save(doc);
    }

    // ===== DOWNLOAD =====
    public Resource download(Long id) {
        Document doc = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found"));
        return new FileSystemResource(doc.getFilePath());
    }

    // ===== DELETE =====
    public void delete(Long id) throws IOException {
        Document doc = repository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found"));

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth != null ? auth.getName() : null;
        List<String> authorities = auth != null ? auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList() : List.of();

        boolean allowed = false;
        if (username != null && username.equals(doc.getUploadedBy())) allowed = true;
        if (authorities.contains("ROLE_ADMIN") || authorities.contains("ROLE_HR")) allowed = true;

        if (!allowed) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not allowed to delete");

        Files.deleteIfExists(Paths.get(doc.getFilePath()));
        repository.delete(doc);
    }

    public Document getDocument(Long id) {
        Document doc = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found"));

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = null;
        List<String> authorities = List.of();
        if (auth != null && auth.isAuthenticated()) {
            username = auth.getName();
            authorities = auth.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority).toList();
        }

        String vis = doc.getVisibility();
        if (vis == null || vis.equalsIgnoreCase("PUBLIC")) return doc;
        if (vis.startsWith("ROLE_")) {
            if (authorities.contains(vis)) return doc;
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }
        if (vis.startsWith("USER:")) {
            String u = vis.substring("USER:".length());
            if (username != null && username.equals(u)) return doc;
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }

        return doc;
    }

    // ===== STATS =====
    public Map<String, Object> getStats() {
        Map<String, Object> map = new HashMap<>();
        map.put("total", repository.count());
        map.put("important",
                repository.findAll().stream().filter(Document::getIsImportant).count());
        map.put("newDocs",
                repository.findAll().stream().filter(Document::getIsNew).count());
        return map;
    }

    // ===== FILE TYPE =====
    private String detectType(String name) {
        name = name.toLowerCase();
        if (name.endsWith(".pdf")) return "pdf";
        if (name.endsWith(".doc") || name.endsWith(".docx")) return "word";
        if (name.endsWith(".xls") || name.endsWith(".xlsx")) return "excel";
        if (name.endsWith(".png") || name.endsWith(".jpg")) return "image";
        return "file";
    }
}
