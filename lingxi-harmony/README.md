# 灵犀伴行 HarmonyOS 客户端（lingxi-harmony）

## 与大版本设计的关系

- 版本设计 [[「V1.0.0」设计方案]] §5.5 与 R05 要求 **PC Web 与 HarmonyOS 功能对等**，使用同一服务端 API、同一状态与权限语义；终端差异只体现在布局与系统能力。
- 本工程与 `lingxi-web/apps/user-web` 使用同一份服务端契约（`http.ts` / `HttpClient.ets` 均按 `code === 'OK'` 判定成功，写接口携带 `Idempotency-Key`，登录/刷新携带 `deviceId`）。

## 当前实现范围（主链路闭环）

| 能力 | 状态 |
| --- | --- |
| 身份断言登录/注册、令牌刷新、deviceId 持久化 | 已实现（`store/SessionStore.ets`） |
| 手机号登录（首次按出生日期自动注册） | 已实现（`SessionStore.loginWithPhone`，与 PC Web 同契约） |
| 今日行动与打卡（含新成就即时提示） | 已实现（`pages/Index.ets` 今日 Tab） |
| 目标列表与进度 | 已实现 |
| 成就列表 | 已实现 |
| 站内消息与已读 | 已实现 |
| 离线命令（`/api/v1/sync/commands`，幂等重放） | 已实现（`offlineCheckIn`） |
| 运行时 bootstrap（升级策略/合规文档/功能开关） | 接口已封装（`LingxiApi.bootstrap`），页面接入待后续批次 |
| 监护（邀请/接受/撤销）、隐私权利、会员、伙伴分享、文件、日历 | 后续批次（与 PC Web 同批推进） |
| Agent 对话与提案确认 | 接口已封装，页面接入待后续批次 |

## 构建与验证状态（重要）

- 本工程**尚未编译、未运行验证**：当前开发机未安装 DevEco Studio / hvigor / ohpm，无法执行 `hvigorw assembleHap`。
- 代码按 HarmonyOS Stage 模型（API 12）组织：`AppScope/app.json5`、`entry/src/main/module.json5`、`entry/src/main/ets/**`。
- 在装有 DevEco Studio 的机器上，导入本目录后执行 `hvigorw assembleHap` 即可；首次构建需要配置签名。
- 未编译验证的结论必须与设计文档、实施交接中的记录一致，不得据此声称已交付。

## 服务端地址

`api/HttpClient.ets` 中的 `ApiConfig.baseUrl` 默认指向 `http://127.0.0.1:8080`；生产环境应改为入口层域名，并通过 `ApiConfig.setBaseUrl()` 注入。
