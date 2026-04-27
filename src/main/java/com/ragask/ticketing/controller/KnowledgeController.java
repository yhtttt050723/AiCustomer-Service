package com.ragask.ticketing.controller;

import com.ragask.ticketing.knowledge.AttachmentService;
import com.ragask.ticketing.knowledge.KnowledgeService;
import com.ragask.ticketing.model.dto.AttachmentDto;
import com.ragask.ticketing.model.dto.KnowledgeIngestRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/knowledge")
public class KnowledgeController {

    private final KnowledgeService knowledgeService;
    private final AttachmentService attachmentService;

    public KnowledgeController(KnowledgeService knowledgeService, AttachmentService attachmentService) {
        this.knowledgeService = knowledgeService;
        this.attachmentService = attachmentService;
    }

    @PostMapping("/ingest")
    public String ingest(@Valid @RequestBody KnowledgeIngestRequest request) {
        knowledgeService.ingest(request);
        return "knowledge item ingested";
    }

    @GetMapping("/titles")
    public List<String> titles() {
        return knowledgeService.listTitles();
    }

    @GetMapping("/attachments")
    public List<AttachmentDto> listAttachments(@RequestParam("source") String source) {
        return attachmentService.listBySource(source);
    }

    @PostMapping(value = "/attachments/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public AttachmentDto uploadAttachment(
            @RequestParam("source") String source,
            @RequestParam("file") MultipartFile file
    ) {
        return attachmentService.upload(source, file);
    }
}
