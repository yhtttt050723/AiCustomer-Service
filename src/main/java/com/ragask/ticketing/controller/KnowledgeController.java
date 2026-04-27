package com.ragask.ticketing.controller;

import com.ragask.ticketing.common.api.Result;
import com.ragask.ticketing.knowledge.AttachmentService;
import com.ragask.ticketing.knowledge.KnowledgeService;
import com.ragask.ticketing.model.dto.AttachmentDto;
import com.ragask.ticketing.model.dto.KnowledgeIngestRequest;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
public class KnowledgeController {

    private final KnowledgeService knowledgeService;
    private final AttachmentService attachmentService;

    @PostMapping("/ingest")
    public Result<String> ingest(@Valid @RequestBody KnowledgeIngestRequest request) {
        knowledgeService.ingest(request);
        return Result.ok("knowledge item ingested");
    }

    @GetMapping("/titles")
    public Result<List<String>> titles() {
        return Result.ok(knowledgeService.listTitles());
    }

    @GetMapping("/attachments")
    public Result<List<AttachmentDto>> listAttachments(@RequestParam("source") String source) {
        return Result.ok(attachmentService.listBySource(source));
    }

    @PostMapping(value = "/attachments/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<AttachmentDto> uploadAttachment(
            @RequestParam("source") String source,
            @RequestParam("file") MultipartFile file
    ) {
        return Result.ok(attachmentService.upload(source, file));
    }
}
