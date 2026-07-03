# AI-interview

<div align="center">

**智能面试与智能 HR 招聘助手平台**

基于 `InterviewGuide` 二次开发，面向求职者面试训练与 HR 招聘全流程 AI 辅助。

[![Java](https://img.shields.io/badge/Java-21-orange?logo=openjdk)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1-green?logo=springboot)](https://spring.io/projects/spring-boot)
[![Spring AI](https://img.shields.io/badge/Spring%20AI-2.0.0-6DB33F)](https://spring.io/projects/spring-ai)
[![React](https://img.shields.io/badge/React-18.3-blue?logo=react)](https://react.dev/)
[![TypeScript](https://img.shields.io/badge/TypeScript-5.6-blue?logo=typescript)](https://www.typescriptlang.org/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-pgvector-336791?logo=postgresql)](https://www.postgresql.org/)

</div>

---

## 项目介绍

`AI-interview` 是在开源项目 `InterviewGuide` 基础上持续增量开发的智能招聘与面试平台。项目保留了原有的简历分析、文字/语音模拟面试、面试安排、知识库 RAG、多模型配置等能力，并进一步扩展为面向 HR 招聘流程的智能助手。

当前平台重点覆盖两个方向：

- **面试辅助**：帮助求职者或培训场景完成简历分析、模拟面试、语音面试、知识库问答和面试日程管理。
- **招聘助手**：帮助 HR 在招聘前中后期完成 JD 生成与优化、JD 合规审查、简历与 JD 匹配、面试题纲/评分表生成、候选人沟通文案生成与优化。

> 说明：仓库中的 Java 包名、数据库名、对象存储 bucket、运行时配置目录等位置仍可能保留 `interview.guide` 或 `interview-guide` 命名，这是从原项目演进而来的实现细节，不影响当前项目名称 `AI-interview`。

## 核心功能

### 招聘助手

招聘助手位于前端导航中的独立分组，包含四个面向 HR 的增量模块。

#### JD 助手

- 支持创建岗位，岗位名称通过新建弹窗录入。
- 一个岗位 JD 对应一次会话，切换岗位即切换上下文。
- 支持多轮对话生成和优化 JD，AI 回复通过 SSE 流式输出。
- 支持 JD 草稿在线编辑、保存版本、一键复制。
- 支持岗位列表删除。
- 右侧结果区按分页面管理：`JD`、`风险`、`画像`、`版本`。
- 内嵌 JD 合规审查，首版面向中国大陆通用招聘合规规则。
- 支持从 JD 中抽取岗位画像，用于后续匹配、题纲和沟通文案生成。

#### 简历与 JD 匹配

- 支持新增匹配任务，通过弹窗录入候选人简历文本、JD 或岗位上下文。
- 当前首版聚焦单候选人与单 JD 的匹配分析。
- 输出匹配分、匹配结论、优势、风险、追问建议和核验点。
- 左侧提供历史任务、搜索和删除能力。
- 批量候选人排序暂未作为首版能力实现，但接口和信息结构为后续扩展保留了空间。

#### 面试题纲/评分表生成

- 支持基于岗位、JD、简历或匹配结果生成面试题纲。
- 输出结构化题纲与评分表，方便面试官直接使用。
- 页面保留两列展示，不提供 JSON 编辑视图。
- 左侧提供历史题纲、搜索和删除能力。
- 当前不强依赖原有面试安排模块，可独立使用。

#### 候选人沟通文案助手

- 支持生成约面、改期、提醒、拒信、Offer 沟通、后续跟进等招聘沟通文案。
- 支持结合候选人简历、面试情况、匹配结果和当前招聘阶段生成更人性化的文案。
- 支持多轮对话优化文案。
- 支持标题和正文编辑、保存、一键复制。
- 左侧提供草稿管理、搜索和删除能力。
- 当前首版只做复制和保存，邮件/短信发送能力后置。

### 简历管理

- 支持 PDF、DOCX、DOC、TXT 等多格式简历上传。
- 使用 Apache Tika 解析文件内容。
- 基于 Redis Stream 异步分析简历，支持处理状态跟踪。
- 支持内容哈希去重和失败重试。
- 支持导出结构化 PDF 简历分析报告。

### 模拟面试

- 支持文字模拟面试和语音模拟面试。
- 内置多个 Skill 面试方向，由 `SKILL.md` 定义考察范围、难度分布和参考知识库。
- 支持基于简历或岗位信息出题。
- 支持多轮追问、历史题目去重、阶段时长配置。
- 文字面试和语音面试共用统一评估架构。
- 支持异步生成并导出模拟面试评估报告。

### 面试安排

- 支持通过规则和 AI 双引擎解析面试邀请。
- 支持飞书、腾讯会议、Zoom 等常见会议文本格式的信息抽取。
- 支持日历视图、列表视图和面试状态流转。
- 支持面试提醒配置。

### 语音面试

- 基于 WebSocket 实现实时语音面试。
- 使用 DashScope 语音能力进行 ASR/TTS。
- 支持服务端 VAD、实时字幕、暂停/恢复和手动提交。
- 支持句子级并发 TTS，边生成边合成边播放。
- 当前仍存在端到端延迟、弱网断续和无耳机回声等体验边界。

### 知识库/RAG

- 支持 PDF、DOCX、Markdown 等知识库文档上传。
- 支持文档分块、向量化和 pgvector 存储。
- 支持查询改写、相似度阈值、TopK 检索。
- 支持 SSE 流式回答、Markdown 展示和虚拟列表渲染。
- 支持知识库分类、下载、重新向量化、搜索和统计信息展示。

### 多模型与系统设置

- 支持 DashScope、LM Studio、Kimi、DeepSeek、GLM 等 OpenAI 兼容 Provider。
- 支持默认聊天模型、默认向量模型和语音服务配置。
- 支持在设置页测试模型连通性和重新加载 Provider 配置。
- 运行时 Provider 配置默认写入 `~/.interview-guide/`，API Key 支持加密保存。

## 技术栈

### 后端

| 技术 | 版本 | 说明 |
| --- | --- | --- |
| Java | 21 | 开发语言，启用虚拟线程 |
| Spring Boot | 4.1.0 | 后端应用框架 |
| Spring AI | 2.0.0 | LLM、Embedding、结构化输出等 AI 能力 |
| Spring AI Agent Utils | 0.10.0 | Skill 资源加载和 Advisor 能力扩展 |
| PostgreSQL + pgvector | PG16 | 关系数据和向量检索 |
| Redis + Redisson | Redis 7 / Redisson 4 | 缓存、限流和 Redis Stream 异步任务 |
| Apache Tika | 2.9.2 | 简历和知识库文件解析 |
| iText 8 | 8.0.5 | PDF 报告导出 |
| MapStruct | 1.6.3 | DTO/Entity 映射 |
| SpringDoc OpenAPI | 3.0.2 | API 文档 |
| DashScope SDK | 2.22.7 | ASR/TTS 等语音能力 |
| AWS S3 SDK | 2.29.51 | MinIO/RustFS 对象存储 |
| Gradle | 8.14 | 构建工具 |

### 前端

| 技术 | 版本 | 说明 |
| --- | --- | --- |
| React | 18.3 | UI 框架 |
| TypeScript | 5.6 | 类型系统 |
| Vite | 5.4 | 前端构建工具 |
| Tailwind CSS | 4.1 | 样式框架 |
| React Router | 7.11 | 路由管理 |
| Framer Motion | 12.23 | 动效 |
| Recharts | 3.6 | 图表 |
| Lucide React | 0.468 | 图标 |
| React Big Calendar | 1.19 | 面试日历 |
| React Virtuoso | 4.18 | 长列表/聊天虚拟列表 |
| pnpm | 10.26 | 包管理器 |

## 架构与数据存储

后端遵循 `Controller -> Service -> Repository` 分层，业务响应统一使用 `Result<T>`。AI 调用集中通过 `LlmProviderRegistry`、`StructuredOutputInvoker` 和 `resources/prompts/` 下的 StringTemplate 模板完成。

主要基础设施：

- **PostgreSQL + pgvector**：核心业务数据、招聘助手生成物、知识库向量数据。
- **Redis / Redisson**：缓存、限流、异步任务和 Redis Stream 消费。
- **MinIO / RustFS**：简历、知识库文档和导出文件等对象存储。

招聘助手生成物均已入库，核心表包括：

- `recruitment_job_posts`、`recruitment_job_post_versions`
- `recruitment_job_conversations`、`recruitment_job_conversation_messages`
- `recruitment_compliance_risk_items`、`recruitment_job_profiles`
- `recruitment_candidate_job_matches`
- `recruitment_interview_guides`
- `recruitment_candidate_message_drafts`
- `recruitment_candidate_message_conversations`、`recruitment_candidate_message_conversation_messages`

## 项目结构

```text
ai-interview/
├── app/                                      # Spring Boot 后端应用
│   ├── src/main/java/interview/guide/
│   │   ├── App.java                         # 主启动类
│   │   ├── common/                          # 通用能力
│   │   │   ├── ai/                          # LLM Provider、结构化输出、Prompt 安全
│   │   │   ├── annotation/                  # 可重复限流注解
│   │   │   ├── aspect/                      # 限流切面与 Redis Lua
│   │   │   ├── async/                       # Redis Stream 生产者/消费者模板
│   │   │   ├── config/                      # CORS、S3、OpenAPI、Jackson 等配置
│   │   │   ├── evaluation/                  # 文字/语音共用评估引擎
│   │   │   ├── exception/                   # 业务异常与全局异常处理
│   │   │   └── result/                      # 统一响应 Result<T>
│   │   ├── infrastructure/                  # 基础设施
│   │   │   ├── export/                      # PDF 导出
│   │   │   ├── file/                        # 文件解析、校验、清洗、S3 存储
│   │   │   ├── mapper/                      # MapStruct 映射器
│   │   │   └── redis/                       # RedisService、会话缓存
│   │   └── modules/                         # 业务模块
│   │       ├── interview/                   # 模拟面试
│   │       ├── interviewschedule/           # 面试安排
│   │       ├── knowledgebase/               # 知识库/RAG
│   │       ├── llmprovider/                 # 多模型 Provider
│   │       ├── recruitment/                 # 招聘助手
│   │       │   ├── job/                     # JD 助手
│   │       │   ├── match/                   # 简历与 JD 匹配
│   │       │   ├── guide/                   # 面试题纲/评分表
│   │       │   ├── message/                 # 候选人沟通文案
│   │       │   ├── model/                   # DTO、Request、Response、Entity
│   │       │   └── repository/              # JPA Repository
│   │       ├── resume/                      # 简历管理
│   │       └── voiceinterview/              # 语音面试
│   └── src/main/resources/
│       ├── application.yml                  # 应用配置
│       ├── prompts/                         # AI Prompt 模板
│       ├── scripts/                         # Redis Lua 脚本
│       ├── skills/                          # 面试 Skill 定义
│       └── voice-interview-opening.yml      # 语音面试开场配置
│
├── frontend/                                # React 前端应用
│   ├── src/
│   │   ├── api/                             # API 客户端
│   │   │   └── recruitment.ts               # 招聘助手 API
│   │   ├── components/                      # 公共组件
│   │   ├── constants/                       # 路由等常量
│   │   ├── hooks/                           # 业务 Hooks
│   │   ├── pages/                           # 页面组件
│   │   │   ├── JobPostAssistantPage.tsx     # JD 助手
│   │   │   ├── CandidateMatchPage.tsx       # 简历匹配
│   │   │   ├── InterviewGuidePage.tsx       # 面试题纲
│   │   │   └── CandidateMessagePage.tsx     # 沟通文案
│   │   ├── types/                           # 类型定义
│   │   │   └── recruitment.ts               # 招聘助手类型
│   │   └── utils/                           # 工具函数
│   ├── package.json
│   └── vite.config.ts
│
├── docs/                                    # 增量需求、开发计划和架构文档
├── docker-compose.yml                       # 完整部署编排
├── docker-compose.dev.yml                   # 本地开发依赖服务
├── .env.example                             # 环境变量示例
└── README.md
```

## 快速开始

### 环境要求

| 依赖 | 版本 | 必需 | 说明 |
| --- | --- | --- | --- |
| JDK | 21+ | 是 | 后端运行和构建 |
| Node.js | 18+ | 是 | 前端运行和构建 |
| pnpm | 10+ | 推荐 | 项目 packageManager 指定 10.26 |
| Docker | - | 推荐 | 启动 PostgreSQL、Redis、RustFS/MinIO |

如果不用 Docker，需要自行准备 PostgreSQL 14+、pgvector 扩展、Redis 6+ 和 S3 兼容对象存储。

### 1. 配置环境变量

```bash
cp .env.example .env
```

至少需要填写：

```bash
AI_BAILIAN_API_KEY=your_dashscope_api_key
APP_AI_CONFIG_ENCRYPTION_KEY=your_random_long_secret
```

`AI_BAILIAN_API_KEY` 用于默认 DashScope 文本模型、Embedding、ASR 和 TTS。其他 Provider 如 Kimi、DeepSeek、GLM、LM Studio 可以在 `.env` 或设置页中维护。

### 2. 启动本地依赖

```bash
docker compose -f docker-compose.dev.yml up -d
```

默认会启动：

| 服务 | 地址 | 说明 |
| --- | --- | --- |
| PostgreSQL | `localhost:5432` | 业务库，默认库名 `interview_guide` |
| Redis | `localhost:6379` | 缓存、限流、异步任务 |
| RustFS | `localhost:9000` / `localhost:9001` | S3 兼容对象存储与控制台 |

> `docker-compose.dev.yml` 会读取根目录 `.env`。如果已复制 `.env.example`，PostgreSQL 默认密码为 `password`；如果没有 `.env`，dev compose 中 PostgreSQL 默认密码为 `123456`。建议本地开发统一先复制 `.env`，避免应用配置和容器密码不一致。

停止依赖：

```bash
docker compose -f docker-compose.dev.yml down
```

清除依赖数据：

```bash
docker compose -f docker-compose.dev.yml down -v
```

### 3. 启动后端

```bash
./gradlew :app:bootRun
```

后端默认地址：

- API：`http://localhost:8080`
- Swagger UI：`http://localhost:8080/swagger-ui.html`

### 4. 启动前端

```bash
cd frontend
corepack enable
pnpm install
pnpm run dev
```

前端默认地址：

- `http://localhost:5173`

招聘助手页面入口：

- JD 助手：`/recruitment/jobs`
- 简历匹配：`/recruitment/matches`
- 面试题纲：`/recruitment/interview-guides`
- 沟通文案：`/recruitment/messages`

## Docker 完整部署

项目也提供完整 Docker Compose 编排，可一次启动前端、后端、PostgreSQL、Redis 和 MinIO。

```bash
cp .env.example .env
# 编辑 .env，填写 AI_BAILIAN_API_KEY 和 APP_AI_CONFIG_ENCRYPTION_KEY

docker compose up -d --build
```

完整部署默认服务：

| 服务 | 地址 | 默认账号 | 默认密码 |
| --- | --- | --- | --- |
| 前端应用 | `http://localhost` | - | - |
| 后端 API | `http://localhost:8080` | - | - |
| Swagger UI | `http://localhost:8080/swagger-ui.html` | - | - |
| PostgreSQL | `localhost:5432` | `postgres` | `password` |
| Redis | `localhost:6379` | - | - |
| MinIO API | `localhost:9000` | `minioadmin` | `minioadmin` |
| MinIO 控制台 | `http://localhost:9001` | `minioadmin` | `minioadmin` |

常用命令：

```bash
docker compose ps
docker compose logs -f app
docker compose up -d --build
docker compose down
docker compose down -v
```

## 开发命令

后端：

```bash
./gradlew :app:compileJava
./gradlew :app:test --no-daemon
./gradlew :app:bootRun
```

前端：

```bash
cd frontend
pnpm run dev
pnpm run build
```

本地依赖：

```bash
docker compose -f docker-compose.dev.yml up -d
```

## 当前边界与后续规划

- JD 合规审查首版只覆盖中国大陆通用招聘合规规则，AI 输出需要 HR 最终复核。
- 简历与 JD 匹配首版只做单候选人分析，批量排序后续扩展。
- 沟通文案助手当前只支持复制和保存，邮件/短信/企微等发送链路后续接入。
- 暂不支持企业自定义合规规则库。
- 招聘助手各模块已经做了任务历史和草稿管理，但更深层的跨模块编排仍会继续演进，例如从 JD 到简历筛选、面试题纲、沟通文案的完整工作流。
- 语音面试后续可继续探索 WebRTC、客户端 VAD、降噪和更多 TTS 音色。

## 常见问题

### 数据库表什么时候创建？

开发环境默认使用：

```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: update
```

首次启动会自动创建缺失表，后续启动会尽量保留已有数据并增量更新。生产环境不建议依赖自动建表，应使用迁移脚本管理 schema。

### 知识库向量化失败怎么办？

确认 PostgreSQL 已安装 pgvector 扩展，并检查：

```yaml
spring:
  ai:
    vectorstore:
      pgvector:
        initialize-schema: true
        dimensions: 1024
        distance-type: COSINE_DISTANCE
```

开发环境可以开启 `initialize-schema`，生产环境建议手动管理向量表结构。

### AI 生成接口容易超时怎么办？

招聘助手中的简历匹配、题纲生成、沟通文案、JD 合规审查等 AI 任务可能耗时较长。前端针对这些接口已使用较长超时时间；如果仍然超时，请检查模型 Provider 响应速度、网络稳定性和后端日志。

### 设置页新增或切换模型后不生效？

运行时 Provider 配置默认写入：

- `~/.interview-guide/llm-providers.yml`
- `~/.interview-guide/llm-providers.env`

可以在设置页测试连接，或调用 `/api/llm-provider/reload` 重新加载配置。Docker 部署时如需持久化，建议挂载该目录。

### 语音面试无法识别或没有声音？

语音面试默认也使用 `AI_BAILIAN_API_KEY`。请检查浏览器麦克风权限、DashScope 语音服务配置、后端 WebSocket 日志和设置页 ASR/TTS 测试结果。无耳机场景下可能出现回声录入，建议先使用手动提交模式或佩戴耳机测试。

### PDF 导出中文异常？

项目内置中文字体文件：

```text
app/src/main/resources/fonts/ZhuqueFangsong-Regular.ttf
```

如果导出异常，请检查字体文件是否存在、iText 依赖是否正常以及后端日志中的字体加载信息。

## 文档

- [HR AI 助手增量需求讨论](docs/hr-ai-assistant-incremental-requirements.md)
- [HR AI 助手开发计划](docs/hr-ai-assistant-development-plan.md)
- [语音面试架构说明](docs/voice-interview-architecture.md)

## 许可证

AGPL-3.0 License
