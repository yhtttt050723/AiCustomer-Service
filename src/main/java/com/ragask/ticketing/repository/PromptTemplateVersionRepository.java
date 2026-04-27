package com.ragask.ticketing.repository;

import com.ragask.ticketing.model.PromptTemplateVersion;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PromptTemplateVersionRepository extends JpaRepository<PromptTemplateVersion, Long> {
    Optional<PromptTemplateVersion> findFirstByPromptKeyAndActiveTrueOrderByVersionNoDesc(String promptKey);
    Optional<PromptTemplateVersion> findFirstByPromptKeyOrderByVersionNoDesc(String promptKey);
    Optional<PromptTemplateVersion> findByPromptKeyAndVersionNo(String promptKey, Integer versionNo);
    List<PromptTemplateVersion> findByPromptKeyOrderByVersionNoDesc(String promptKey);
}
