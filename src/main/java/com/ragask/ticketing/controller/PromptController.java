package com.ragask.ticketing.controller;

import com.ragask.ticketing.common.api.Result;
import com.ragask.ticketing.model.PromptTemplateVersion;
import com.ragask.ticketing.model.dto.PromptRollbackRequest;
import com.ragask.ticketing.model.dto.PromptVersionRequest;
import com.ragask.ticketing.service.PromptTemplateService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/prompts")
@RequiredArgsConstructor
public class PromptController {

    private final PromptTemplateService promptTemplateService;

    @GetMapping("/active")
    public Result<String> active(@RequestParam String key) {
        return Result.ok(promptTemplateService.getActiveContent(key));
    }

    @GetMapping("/history")
    public Result<List<PromptTemplateVersion>> history(@RequestParam String key) {
        return Result.ok(promptTemplateService.history(key));
    }

    @PostMapping("/versions")
    public Result<PromptTemplateVersion> create(@Valid @RequestBody PromptVersionRequest request) {
        return Result.ok(promptTemplateService.createVersion(
                request.getPromptKey(),
                request.getContent(),
                request.getChangeNote(),
                request.isActivate()
        ));
    }

    @PostMapping("/rollback")
    public Result<PromptTemplateVersion> rollback(@Valid @RequestBody PromptRollbackRequest request) {
        return Result.ok(promptTemplateService.rollback(request.getPromptKey(), request.getTargetVersion(), request.getChangeNote()));
    }
}
