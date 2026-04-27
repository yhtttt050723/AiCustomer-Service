# AI工单系统技术选型与流程图

## 1. 技术选型

## 1.1 核心技术栈

| 模块 | 技术选型 | 选型理由 |
|---|---|---|
| 后端框架 | Spring Boot 3.5 + Java 21 | 企业级成熟，生态完善，便于服务治理与长期维护 |
| AI编排 | Spring AI 1.1（自定义检索链路） | 保留框架集成便利，同时支持混合检索、精排、降级、溯源的自定义能力 |
| 生成模型 | qwen-plus | 中文问答与指令跟随能力好，适合企业客服场景 |
| 向量模型 | text-embedding-v3 | 语义检索质量稳定，适配RAG链路 |
| 重排模型 | gte-rerank | 精排效果好，可提升最终答案相关性 |
| 主数据库 | PostgreSQL | 稳定可靠，事务与结构化数据管理能力强 |
| 向量检索 | PGVector（HNSW） | 与PostgreSQL一体化，降低运维复杂度 |
| 全文检索 | PostgreSQL GIN + tsvector | 补足精确词、术语、条款号检索能力 |
| 缓存 | Redis | 会话缓存、热点问答缓存、限流与临时状态存储 |
| 文档存储 | MinIO | 文档与附件统一存储，支持版本化与对象管理 |
| 消息异步 | RabbitMQ/Kafka（可选） | 处理索引构建、回流入库、评估任务等异步流程 |
| 可观测性 | Prometheus + Grafana + Loki | 指标、日志、告警全链路监控 |

## 1.2 关键技术策略

- 不使用默认单路问答链路，采用“可控分步式检索编排”
- 检索采用“向量检索 + 全文检索 + RRF 融合”
- 权限控制在检索层强制过滤，不依赖 Prompt
- Reranker 超时自动降级，保障稳定性与响应时延

---

## 2. 技术框架图

```mermaid
flowchart LR
    U[客户/员工] --> C[Web/IM/工单门户]
    C --> G[API网关]
    G --> T[工单服务 Ticket Service]
    G --> A[AI编排服务 RAG Orchestrator]

    subgraph Offline[离线索引管道]
        D1[文档上传]
        D2[文档解析]
        D3[分块Chunk]
        D4[Embedding]
        D5[(PostgreSQL + PGVector)]
        D6[(MinIO 文档存储)]
        D1 --> D2 --> D3 --> D4 --> D5
        D1 --> D6
    end

    subgraph Online[在线查询管道]
        Q1[查询改写]
        Q2[向量检索 TopK]
        Q3[全文检索 TopK]
        Q4[RRF融合]
        Q5[Reranker精排]
        Q6[上下文裁剪]
        Q7[引用溯源]
        Q8[流式生成]
        Q1 --> Q2
        Q1 --> Q3
        Q2 --> Q4
        Q3 --> Q4
        Q4 --> Q5 --> Q6 --> Q7 --> Q8
    end

    subgraph Metrics[效果评估与命中率统计]
        E1[检索命中率 TopK HitRate]
        E2[Rerank提升率 NDCG/Recall]
        E3[答案命中率与解决率]
        E4[看板与告警 Grafana]
        E1 --> E4
        E2 --> E4
        E3 --> E4
    end

    A --> Q1
    Q8 --> T
    T --> C
    Q4 -.统计.-> E1
    Q5 -.统计.-> E2
    Q8 -.统计.-> E3
    T -.工单结果回流.-> E3

    A --> P[Permission Service]
    P --> K[(allowedKbIds)]
    K --> Q2
    K --> Q3

    A --> R[(Redis)]
    A --> M[(Model API: qwen-plus / embedding / gte-rerank)]

    T --> H1[一线人工客服]
    H1 --> H2[二线技术客服]
    H1 --> KB[优质问题入库审核]
    H2 --> KB
    KB --> D1
```

---

## 3. 开发流程图（项目实施）

```mermaid
flowchart TD
    S0[阶段0 需求与数据盘点] --> S1[阶段1 离线索引管道开发]
    S1 --> S2[阶段2 在线查询链路开发]
    S2 --> S3[阶段3 工单流程与转派集成]
    S3 --> S4[阶段4 权限与安全治理]
    S4 --> S5[阶段5 评估压测与灰度发布]
    S5 --> S6[阶段6 运营优化与持续迭代]

    S0 --> O0[输出: PRD/SLA/权限矩阵]
    S1 --> O1[输出: 文档入库、版本、差量更新]
    S2 --> O2[输出: 混合检索+RRF+Rerank+降级]
    S3 --> O3[输出: AI->一线->二线闭环]
    S4 --> O4[输出: 检索层硬权限+审计]
    S5 --> O5[输出: 性能与效果报告]
    S6 --> O6[输出: 入库闭环与成本优化]
```

---

## 4. 业务逻辑图（工单闭环）

```mermaid
flowchart TD
    A[客户提交问题] --> B[AI理解问题并检索知识库]
    B --> C{是否可高置信回答?}

    C -- 是 --> D[AI给出答案+引用来源]
    D --> E{用户是否解决?}
    E -- 是 --> F[工单关闭]
    E -- 否 --> G[转一线人工客服]

    C -- 否 --> G[转一线人工客服]
    G --> H{一线是否可解决?}
    H -- 是 --> I[一线回复并结案]
    H -- 否 --> J[升级二线技术客服]
    J --> K[二线定位修复并回复]
    K --> L[工单结案]

    I --> M{是否高价值问题?}
    L --> M
    F --> M
    M -- 是 --> N[入库审核]
    N --> O[写入知识库并版本化]
    O --> P[优化后续AI召回与回答]
    M -- 否 --> Q[仅归档]
```

---

## 5. 运行时处理流程（接口视角）

```mermaid
sequenceDiagram
    participant User as 客户
    participant Ticket as 工单服务
    participant RAG as AI编排服务
    participant DB as PG/PGVector
    participant Model as 模型API
    participant CS1 as 一线客服
    participant CS2 as 二线技术客服

    User->>Ticket: 提交问题
    Ticket->>RAG: 发起AI处理
    RAG->>DB: 向量检索 + 全文检索
    RAG->>RAG: RRF融合 + Rerank(含超时降级)
    RAG->>Model: 生成答案(附引用)
    Model-->>RAG: 返回答案
    RAG-->>Ticket: AI结果+置信度

    alt 高置信
        Ticket-->>User: 直接回复
    else 低置信/未解决
        Ticket->>CS1: 转一线
        alt 一线可解
            CS1-->>User: 人工回复并结案
        else 一线不可解
            CS1->>CS2: 升级二线
            CS2-->>User: 技术处理结果
        end
    end
```

---

## 6. 建议的里程碑与验收指标

- M1（第2周）：离线索引管道可用，文档可入库可检索
- M2（第5周）：在线RAG链路可用，支持混合检索与降级
- M3（第7周）：工单转派闭环可用（AI/一线/二线）
- M4（第10周）：灰度发布，完成评估与运营看板

建议核心指标：

- 自动解决率 >= 35%
- Top5 检索命中率 >= 85%
- 首次响应 P95 <= 10s
- 转人工准确率 >= 90%
- 越权召回 = 0
- 高价值问题入库率 >= 80%

---

## 7. 开发周期图（甘特图）

```mermaid
gantt
    title AI工单系统开发周期（10周）
    dateFormat  YYYY-MM-DD
    axisFormat  %m/%d

    section 阶段0 需求与规划
    需求梳理与范围冻结           :a1, 2026-05-04, 5d
    SLA/权限模型设计            :a2, after a1, 2d

    section 阶段1 离线索引管道
    文档解析与分块               :b1, 2026-05-11, 5d
    Embedding与PGVector入库      :b2, after b1, 5d
    版本管理与差量更新           :b3, after b2, 4d

    section 阶段2 在线查询链路
    查询改写+混合检索+RRF        :c1, 2026-05-25, 5d
    Reranker+超时降级            :c2, after c1, 4d
    引用溯源+流式输出            :c3, after c2, 3d

    section 阶段3 工单流程集成
    工单状态机与转派规则         :d1, 2026-06-08, 5d
    AI/一线/二线闭环联调         :d2, after d1, 4d

    section 阶段4 安全治理
    检索层权限硬过滤             :e1, 2026-06-17, 3d
    审计日志与脱敏               :e2, after e1, 2d

    section 阶段5 发布与评估
    压测与效果评估               :f1, 2026-06-22, 4d
    灰度发布与复盘               :f2, after f1, 4d
```

---

## 8. 里程碑图（Milestone Timeline）

```mermaid
timeline
    title AI工单系统关键里程碑（10周）

    第1-2周 : M1 离线索引能力完成
            : 文档上传、解析、分块、向量入库可用

    第3-5周 : M2 在线RAG链路完成
            : 混合检索、RRF、Reranker、降级策略上线

    第6-7周 : M3 工单闭环完成
            : AI回答 -> 一线客服 -> 二线技术客服全链路打通

    第8周   : M4 安全与权限完成
            : 检索层硬过滤、审计日志、脱敏规则生效

    第9-10周: M5 灰度与验收完成
            : 性能评估、效果评估、灰度发布、运营看板上线
```
