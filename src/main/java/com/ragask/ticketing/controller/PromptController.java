package com.ragask.ticketing.controller;

import com.ragask.ticketing.model.PromptTemplateVersion;
import com.ragask.ticketing.model.dto.PromptRollbackRequest;
import com.ragask.ticketing.model.dto.PromptVersionRequest;
import com.ragask.ticketing.service.PromptTemplateService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/prompts")
public class PromptController {

    private final PromptTemplateService promptTemplateService;

    public PromptController(PromptTemplateService promptTemplateService) {
        this.promptTemplateService = promptTemplateService;
    }

    @GetMapping("/active")
    public String active(@RequestParam String key) {
        return promptTemplateService.getActiveContent(key);
    }

    @GetMapping("/history")
    public List<PromptTemplateVersion> history(@RequestParam String key) {
        return promptTemplateService.history(key);
    }

    @PostMapping("/versions")
    public PromptTemplateVersion create(@Valid @RequestBody PromptVersionRequest request) {
        return promptTemplateService.createVersion(
                request.promptKey(),
                request.content(),
                request.changeNote(),
                request.activate()
        );
    }

    @PostMapping("/rollback")
    public PromptTemplateVersion rollback(@Valid @RequestBody PromptRollbackRequest request) {
        return promptTemplateService.rollback(request.promptKey(), request.targetVersion(), request.changeNote());
    }
}
