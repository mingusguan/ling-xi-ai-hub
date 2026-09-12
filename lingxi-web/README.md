# 灵犀伴行 PC Web（lingxi-web）

## 工程结构

```text
lingxi-web/
├─ apps/user-web/          # PC 用户端：Vue 3 + TypeScript + Vite + Pinia
└─ packages/api-client/    # 与服务端契约一致的类型与接口封装（用户端与管理端共享）
```

- 后端是模块化单体（`lingxi-server`），用户端 API 前缀固定 `/api/v1`；开发环境由 Vite 代理到 `http://127.0.0.1:8080`，生产环境由入口层按同一前缀转发，因此客户端不区分环境硬编码后端地址。
- 管理端当前仍由 `lingxi-ui`（Vue 2 + Element UI）承载，尚未迁入本工程；本工程按设计预留 `apps/admin-web` 位置与 `packages` 共享包。

## 契约来源

`packages/api-client` 的类型与端点严格来自 `lingxi-server` 各模块 Controller 与 `api` 包 record 定义，重点约束：

| 约束 | 说明 |
| --- | --- |
| 成功判定 | 统一响应 `{ code, message, data, requestId }`，成功固定 `code === 'OK'`；不得依赖 HTTP 2xx 推断成功 |
| 幂等 | 创建目标、保存/确认计划、打卡、完成复盘、创建会话、启动运行、隐私请求、监护邀请、绑定日历等写接口必须携带 `Idempotency-Key` |
| 鉴权 | `Authorization: Bearer <opaqueToken>`；access 15 分钟、refresh 30 天且刷新后轮换，刷新需同时提交 `sessionFamilyId + refreshToken + deviceId` |
| 设备标识 | C 端没有 `X-Device-Id` 请求头，`deviceId` 只出现在登录/刷新请求体，必须由客户端持久化 |
| 错误处理 | 按业务错误码分支（`*_ACCESS_DENIED` 403、`AUTH_*` 401、`*_CONFLICT` 409、`AUTH_RECENT_AUTHENTICATION_REQUIRED` 400） |
| 分页 | `PageResult{ items, total, page, pageSize }`，页码从 1 开始，`pageSize ≤ 200` |

## 已实现页面（主链路闭环）

| 路由 | 能力 |
| --- | --- |
| `/login` | 手机号登录/注册（首次登录需出生日期，服务端强制 14+ 准入）；身份断言登录保留为身份桥接层联调入口 |
| `/` | 今日行动、7 天实例、打卡（含本次新成就即时提示） |
| `/goals` | 目标列表与创建草稿目标 |
| `/goals/:goalId` | 里程碑/行动编排、直接确认计划、仅保存草案、激活已保存草案 |
| `/achievements` | 成就墙与类型筛选（目标达成 / 里程碑完成 / 连续打卡） |
| `/companion` | 会话创建、Agent 运行、SSE 事件流、T2/T3 提案确认、长期记忆查看与删除 |
| `/notifications` | 站内消息、已读、增量同步与实时变更流 |
| `/guardian` | 青少年发起监护邀请、成人接受邀请、关系查询/撤销、争议提交 |
| `/privacy` | 导出/更正/删除/注销请求、进度查询、冷静期撤销、导出下载、年龄申诉、客服工单 |

## 命令

```bash
npm install                       # 安装工作区依赖
npm run dev --workspace @lingxi/user-web    # 开发服务器（默认 http://127.0.0.1:5173）
npm run build --workspace @lingxi/user-web  # 类型检查 + 生产构建
```

## 验证状态

- `vue-tsc --noEmit` 与 `vite build` 已通过（构建产物 `apps/user-web/dist`）。
- 服务端联调：已用真实 MySQL + 真实 HTTP 完成注册→登录→创建目标→确认计划→打卡→成就授予的端到端验证（见实施交接）。
- 尚未完成：真实身份桥接层（手机号/华为账号）接入、浏览器端完整回归、会员/伙伴分享/模板/文件/日历页面。
