# AGENTS.md

## 项目简介
Vortex 是一个可视化数据集成平台项目（类 n8n），目标是通过可视化编排实现企业数据从内部系统到外部平台的稳定分发与追溯。

当前仓库处于项目初始化阶段，已完成 MVP 设计文档沉淀，并创建前后端分离目录。

## 目录结构

```text
Vortex/
├─ back/                                # 后端代码目录（Java 25）
├─ web/                                 # 前端代码目录（React）
├─ docs/
│  └─ plans/
│     └─ 2026-03-09-vortex-mvp-design.md  # 已评审的 MVP 设计文档
├─ design.md                            # 产品需求文档（PRD）
├─ .claude/                             # 本地工具/会话相关目录
└─ .codex/                              # 本地工具/会话相关目录
```

## 项目内容说明

### 1) 产品需求（PRD）
- 文件：`design.md`
- 内容：定义平台核心能力、流程节点、运行状态、指标与追溯要求。
- 关键约束：`1 Flow = 1 目标平台`。

### 2) MVP 设计（已评审）
- 文件：`docs/plans/2026-03-09-vortex-mvp-design.md`
- 内容：
  - 技术栈：前后端分离，`React + Java 25`
  - 范围：同步闭环 MVP（含画布、过滤/转换、拆分、推送、提取器、追溯）
  - 稳定性目标：连续 7 天无丢失、无重复成功写入

### 3) 代码目录职责
- `back/`：后端服务、执行引擎、节点插件、幂等与追溯相关实现。
- `web/`：可视化画布、节点配置页、运行监控与追溯 UI。

## 技术基线（强制）

### 1) JDK 与运行时
- 统一使用 JDK 25。
- 本机 JDK 路径：`D:\env\jdk\corretto-25.0.1`。
- 本仓库所有 Java 构建、测试命令都应在该 JDK 下执行。

### 2) 虚拟线程（JDK 25 特性）使用约定
- 后端并发模型默认采用虚拟线程优先。
- 新增异步任务执行器时，优先使用 `Executors.newVirtualThreadPerTaskExecutor()`。
- 禁止先引入复杂固定线程池方案；确需平台线程池时必须说明原因。
- 重点关注“可能导致 pinning 的阻塞段”（如长时间持有同步锁 + 阻塞 I/O），避免抵消虚拟线程收益。

### 3) Spring Boot 版本与配置约定
- Spring Boot 统一使用最新稳定版本（当前基线：`4.0.3`）。
- 健康检查与管理端点统一使用依赖：`spring-boot-starter-actuator`。
- Spring Boot 配置文件统一使用 YAML 风格（`application.yml`）。
- 开启虚拟线程配置：
  - `spring.threads.virtual.enabled=true`
  - `spring.main.keep-alive=true`（涉及调度任务时必须显式配置）
- 任务执行/调度默认走 Spring Boot 的虚拟线程自动配置；如需自定义执行器，必须保持虚拟线程语义一致。

### 4) 数据库与持久化约定
- 当前默认数据库：`MySQL`。
- 后续目标：逐步支持多数据库平台（如 PostgreSQL/SQL Server 等），设计上避免强绑定 MySQL 方言。
- 表结构初始化与版本演进统一使用 `Flyway` 管理（`back/src/main/resources/db/migration`）。
- 单表增删改查统一使用 `MyBatis-Plus`（官方站点：`https://baomidou.com/`）。
- MVP 阶段优先使用 MyBatis-Plus 提供的基础能力，避免过早引入复杂 ORM 抽象层。

### 5) 文档查询约定
- 对于不清楚、易变化或需要实时确认的技术信息，优先通过 `Context7` 查询官方文档后再实现。

## 当前状态
- 已初始化 Git 仓库。
- 代码尚未开始实现，`back/` 与 `web/` 为待开发目录。

## 后续建议（落地顺序）
1. 在 `back/` 建立基础工程与模块骨架（flow-definition、execution-engine、node-plugins 等）。
2. 在 `web/` 建立 React 工程与基础画布框架。
3. 按 `docs/plans/2026-03-09-vortex-mvp-design.md` 拆解实现计划并执行。
