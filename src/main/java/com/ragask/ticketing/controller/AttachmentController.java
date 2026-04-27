package com.ragask.ticketing.controller;

import com.ragask.ticketing.knowledge.AttachmentService;
import com.ragask.ticketing.knowledge.KnowledgeAttachment;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/attachments")
public class AttachmentController {

    private final AttachmentService attachmentService;

    public AttachmentController(AttachmentService attachmentService) {
        this.attachmentService = attachmentService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<Resource> download(@PathVariable("id") Long id) {
        KnowledgeAttachment meta = attachmentService.getMeta(id);
        Resource resource = attachmentService.loadAsResource(id);

        boolean inline = meta.getContentType() != null
                && (meta.getContentType().startsWith("image/") || "application/pdf".equals(meta.getContentType()));

        ContentDisposition disposition = (inline
                ? ContentDisposition.inline()
                : ContentDisposition.attachment())
                .filename(meta.getFileName())
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentType(MediaType.parseMediaType(meta.getContentType()))
                .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(meta.getSizeBytes()))
                .body(resource);
    }
}

