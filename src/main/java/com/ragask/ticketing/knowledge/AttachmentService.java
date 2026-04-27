package com.ragask.ticketing.knowledge;

import com.ragask.ticketing.model.dto.AttachmentDto;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class AttachmentService {

    private static final Set<String> ALLOWED_EXT = Set.of("pdf", "docx", "png", "jpeg", "jpg");

    private final KnowledgeAttachmentRepository repository;
    private final Path storageDir;

    public AttachmentService(
            KnowledgeAttachmentRepository repository,
            @Value("${attachments.storage-dir:data/attachments}") String storageDir
    ) {
        this.repository = repository;
        this.storageDir = Paths.get(storageDir);
    }

    public AttachmentDto upload(String source, MultipartFile file) {
        if (source == null || source.isBlank()) {
            throw new IllegalArgumentException("source is required");
        }
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("file is required");
        }

        String originalName = StringUtils.cleanPath(file.getOriginalFilename() == null ? "" : file.getOriginalFilename());
        String ext = extensionLower(originalName);
        if (!ALLOWED_EXT.contains(ext)) {
            throw new IllegalArgumentException("unsupported file type: " + ext + ", allowed=" + ALLOWED_EXT);
        }

        String contentType = normalizedContentType(ext, file.getContentType());
        String storedName = UUID.randomUUID() + "_" + safeFileName(originalName);
        Path target = storageDir.resolve(storedName).normalize();

        try {
            Files.createDirectories(storageDir);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("failed to store attachment", e);
        }

        KnowledgeAttachment att = new KnowledgeAttachment();
        att.setSource(source);
        att.setFileName(originalName.isBlank() ? storedName : originalName);
        att.setContentType(contentType);
        att.setStoragePath(target.toAbsolutePath().toString());
        att.setSizeBytes(file.getSize());
        KnowledgeAttachment saved = repository.save(att);
        return toDto(saved);
    }

    public List<AttachmentDto> listBySource(String source) {
        if (source == null || source.isBlank()) {
            return List.of();
        }
        return repository.findBySourceOrderByCreatedAtDesc(source).stream()
                .map(this::toDto)
                .toList();
    }

    public Resource loadAsResource(Long id) {
        KnowledgeAttachment att = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("attachment not found: " + id));
        return new FileSystemResource(att.getStoragePath());
    }

    public KnowledgeAttachment getMeta(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("attachment not found: " + id));
    }

    private AttachmentDto toDto(KnowledgeAttachment att) {
        return new AttachmentDto(
                att.getId(),
                att.getFileName(),
                att.getContentType(),
                att.getSizeBytes(),
                "/api/attachments/" + att.getId()
        );
    }

    private String safeFileName(String originalName) {
        if (originalName == null || originalName.isBlank()) {
            return "file";
        }
        String name = originalName.replace("\\", "_").replace("/", "_");
        return name.length() > 180 ? name.substring(name.length() - 180) : name;
    }

    private String extensionLower(String fileName) {
        if (fileName == null) {
            return "";
        }
        int idx = fileName.lastIndexOf('.');
        if (idx < 0 || idx == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(idx + 1).toLowerCase(Locale.ROOT);
    }

    private String normalizedContentType(String ext, String incoming) {
        return switch (ext) {
            case "pdf" -> "application/pdf";
            case "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "png" -> "image/png";
            case "jpeg", "jpg" -> "image/jpeg";
            default -> (incoming == null || incoming.isBlank()) ? "application/octet-stream" : incoming;
        };
    }
}

