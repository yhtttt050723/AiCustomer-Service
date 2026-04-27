package com.ragask.ticketing.knowledge;

import com.ragask.ticketing.model.dto.KnowledgeIngestRequest;
import java.util.List;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

@Service
public class KnowledgeService {

    public record KnowledgeChunk(Long id, String title, String content, String source) {
    }

    private final KnowledgeDocumentRepository repository;
    private final PgVectorStoreService pgVectorStoreService;

    public KnowledgeService(
            KnowledgeDocumentRepository repository,
            PgVectorStoreService pgVectorStoreService
    ) {
        this.repository = repository;
        this.pgVectorStoreService = pgVectorStoreService;
    }

    public void ingest(KnowledgeIngestRequest request) {
        KnowledgeDocument document = new KnowledgeDocument();
        document.setTitle(request.title());
        document.setContent(request.content());
        document.setSource("kb://manual/" + request.title().replace(" ", "-"));
        KnowledgeDocument saved = repository.save(document);
        pgVectorStoreService.indexDocument(saved);
    }

    public void ingestFromTicket(Long ticketId, String summary) {
        KnowledgeDocument document = new KnowledgeDocument();
        document.setTitle("ticket-" + ticketId + ": " + summary);
        document.setContent(summary);
        document.setSource("kb://ticket/" + ticketId);
        KnowledgeDocument saved = repository.save(document);
        pgVectorStoreService.indexDocument(saved);
    }

    public List<String> listTitles() {
        return repository.findAll().stream().map(KnowledgeDocument::getTitle).toList();
    }

    public List<KnowledgeChunk> listChunks() {
        return repository.findAll().stream()
                .map(doc -> new KnowledgeChunk(doc.getId(), doc.getTitle(), doc.getContent(), doc.getSource()))
                .toList();
    }

    @EventListener(ApplicationReadyEvent.class)
    public void preload() {
        if (repository.count() > 0) {
            return;
        }
        ingest(new KnowledgeIngestRequest(
                "假期申请流程",
                "员工请假流程：在OA提交请假单，直属主管审批，人事复核后生效。病假超过3天需上传医院证明。"
        ));
        ingest(new KnowledgeIngestRequest(
                "PO号字段说明",
                "PO号是采购订单编号。创建采购申请时在订单明细填写PO号字段，格式为PO-年份-序号。"
        ));
        ingest(new KnowledgeIngestRequest(
                "SKU-8821同步失败",
                "当出现SKU-8821同步失败时，先检查库存服务连接，再检查消息队列堆积，最后重试同步任务。"
        ));
        ingest(new KnowledgeIngestRequest(
                "发版回滚SOP",
                "发版异常时执行回滚：锁定流量，回滚到上一稳定版本，执行数据库兼容脚本，验证核心接口。"
        ));
        ingest(new KnowledgeIngestRequest(
                "账号锁定处理",
                "连续5次密码错误将触发账号锁定。客服可在后台执行解锁，用户需在24小时内修改密码。"
        ));
    }
}
