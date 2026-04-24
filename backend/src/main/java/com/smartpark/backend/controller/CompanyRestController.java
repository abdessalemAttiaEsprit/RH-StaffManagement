package com.smartpark.backend.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.smartpark.backend.model.CompanySettings;
import com.smartpark.backend.service.ISettingsService;

import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/api/settings")
@RequiredArgsConstructor
@CrossOrigin("http://localhost:4200")
public class CompanyRestController {
    private final ISettingsService settingsService;

    private final Path uploadsRoot = Paths.get(System.getProperty("user.dir")).resolve("uploads");

    @GetMapping
    public ResponseEntity<CompanySettings> getSettings() {
        return ResponseEntity.ok(settingsService.getSettings());
    }

    @PostMapping
    public ResponseEntity<CompanySettings> saveSettings(@RequestBody CompanySettings settings) {
        return ResponseEntity.ok(settingsService.updateSettings(settings));
    }

    @PostMapping("/signature/upload")
    public ResponseEntity<CompanySettings> uploadSignature(@RequestParam("file") MultipartFile file) {
        try {
            if (file == null || file.isEmpty()) {
                return ResponseEntity.badRequest().build();
            }
            if (!Files.exists(uploadsRoot)) {
                Files.createDirectories(uploadsRoot);
            }

            String safeOriginal = (file.getOriginalFilename() != null) ? file.getOriginalFilename() : "signature.png";
            safeOriginal = safeOriginal.replaceAll("[^a-zA-Z0-9._-]", "_");
            String fileName = System.currentTimeMillis() + "_" + safeOriginal;
            Path target = uploadsRoot.resolve(fileName);
            Files.copy(file.getInputStream(), target);

            CompanySettings settings = settingsService.getSettings();
            if (settings == null) settings = new CompanySettings();
            settings.setSignatureFileName(fileName);
            CompanySettings saved = settingsService.updateSettings(settings);
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/files/{filename:.+}")
    public ResponseEntity<Resource> getFile(@PathVariable String filename) {
        try {
            Path file = uploadsRoot.resolve(filename);
            Resource resource = new UrlResource(file.toUri());
            if (resource.exists() && resource.isReadable()) {
                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                        .body(resource);
            }
            return ResponseEntity.notFound().build();
        } catch (MalformedURLException e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
