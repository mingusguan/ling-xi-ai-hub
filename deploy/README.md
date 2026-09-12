# LingXi production release

## Automated release (GitHub Actions)

Since the v1 auto-deploy rollout, pushing to the default branch (`master` / `main`)
triggers fully automated releases via two workflows:

- `.github/workflows/build-image.yml` — backend (`lingxi-admin`)
  Triggered only by backend paths (`lingxi-admin/**`, `lingxi-ai/**`, `lingxi-api/**`,
  `lingxi-auth/**`, `lingxi-common/**`, `lingxi-modules/**`, `lingxi-server/**`,
  `Dockerfile`, `pom.xml`, `deploy/**`). Builds the image, pushes to ACR
  (`lingxi-ai-hub:<full-sha>`, `:<7-sha>`, `:latest`), then SSHes to the server,
  uploads `deploy/docker-compose.prod.yml` + `deploy/deploy.sh`, and runs
  `./deploy.sh <full-sha>` (pull + up + health check).

- `.github/workflows/deploy-lingxi-ui.yml` — frontend (`lingxi-ui`)
  Triggered by any `lingxi-ui/**` change. Builds with `npm ci` +
  `vue-cli-service build` (Node 18, publicPath `/`), uploads `dist` to
  `/data/mingus/nginx/html/lingxi`, then atomically swaps the `dist` directory
  and reloads `mingus-nginx`.

Required GitHub repository secrets:
`SERVER_HOST` (`1.14.43.81`), `SERVER_USERNAME` (`mingus`), `SERVER_SSH_KEY`
(private key matching the server user), `DOCKER_USERNAME`, `DOCKER_PASSWORD`.

The frontend `dist` on the server is served by `lingxi.mingusone.com` with
`root <nginx html>/lingxi/dist`; do not re-introduce a `/lingxi/` path prefix
unless the nginx location changes back to an `alias`-style setup.

## Manual release (fallback)

Manual release remains possible and follows these steps:

1. Push code to GitHub.
2. GitHub Actions builds and pushes the backend Docker image.
3. Confirm the version.
4. SSH to the production server.
5. Update `IMAGE_TAG` in `.env`, or pass it to `deploy.sh`.
6. Run `docker compose pull && docker compose up -d` through `deploy.sh`.
7. Check logs and the health URL.

## GitHub Actions

The workflows push to Aliyun ACR with these settings (edit only if you mirror the
repository to another account):

- `IMAGE_REGISTRY`: `registry.cn-hangzhou.aliyuncs.com`
- `IMAGE_NAMESPACE`: `mingus`
- `BACKEND_IMAGE_NAME`: `lingxi-ai-hub`
- `DOCKER_USERNAME` and `DOCKER_PASSWORD`: GitHub Actions repository secrets, do not commit real secrets

The generated backend image tag is:

```text
registry.cn-hangzhou.aliyuncs.com/mingus/lingxi-ai-hub:${GITHUB_SHA}
registry.cn-hangzhou.aliyuncs.com/mingus/lingxi-ai-hub:${GITHUB_SHA::7}
registry.cn-hangzhou.aliyuncs.com/mingus/lingxi-ai-hub:latest
```

Automatic deploy happens only on pushes to `master`/`main` (see "Automated
release" above). `v*` tags no longer trigger builds. You can still trigger the
first build from GitHub Actions with `Run workflow`.

## Server setup

Copy these files to the production server under `/data/mingus/lingxi/deploy`
(on this server it is the directory where `docker compose ls` shows the `deploy`
project; `deploy.sh`, `docker-compose.prod.yml` and `.env` live together there):

```text
deploy/docker-compose.prod.yml
deploy/deploy.sh
deploy/check.sh
deploy/.env.example
```

> Note: with the automated release the workflow re-uploads
> `docker-compose.prod.yml` and `deploy.sh` to
> `/data/mingus/lingxi/deploy` on every backend deploy, so the repository copies
> are the source of truth. Keep `.env` server-side only (it is git-ignored).

Create the real environment file:

```bash
cp .env.example .env
vi .env
```

Create the shared Docker network once:

```bash
docker network create mingus-net
```

Make sure the MySQL and Redis compose projects also join the same external network:

```yaml
networks:
  default:
    external: true
    name: mingus-net
```

Prepare writable directories for logs and uploads:

```bash
mkdir -p logs uploadPath
chown -R 10001:10001 logs uploadPath
```

Login to the image registry once:

```bash
docker login registry.cn-hangzhou.aliyuncs.com
```

The default `.env.example` assumes the shared network service names are `mysql` and `redis`.
If your compose service names differ, update `MYSQL_URL` and `REDIS_HOST` to match.

The frontend is deployed as static files under the existing Nginx container, not as a Docker image.
With the automated release, a `lingxi-ui/**` push to `master`/`main` builds `dist`
and atomically swaps `/data/mingus/nginx/html/lingxi/dist` (kept as
`/usr/share/nginx/html/lingxi/dist` inside `mingus-nginx`), then reloads nginx.
For a manual fallback: build `lingxi-ui` and copy `dist` to
`/data/mingus/nginx/html/lingxi/dist`, then reload `mingus-nginx`.

## Domain binding

The default production domain is `mingusone.com`.

Create these DNS records after ICP approval:

```text
mingusone.com      A      1.14.43.81
www.mingusone.com  A      1.14.43.81
```

Use `mingusone.com` and `www.mingusone.com` for the system entrance. The Nginx config redirects `/` to `/lingxi/`, serves the frontend from `/lingxi/`, proxies backend requests from `/prod-api/` to `lingxi-admin:8080`, and proxies private file preview requests from `/file/` to `lingxi-admin:8080`.

Tencent COS bucket `lingxi-1313338428` should stay private read/write. Uploaded files are returned as stable `/file/preview/...` URLs. When a logged-in browser opens that URL, the backend generates a short-lived COS signed URL and redirects to COS, so do not enable public read just to make returned file URLs work.

```text
FILE_DOMAIN=http://mingusone.com
TENCENT_COS_DOMAIN=
TENCENT_COS_SIGNED_URL_EXPIRE_SECONDS=600
```

After a TLS certificate is installed on Nginx, switch `FILE_DOMAIN` to `https://mingusone.com`.

## Release

Deploy a specific image tag:

```bash
./deploy.sh abc1234
```

Or edit `.env` manually:

```text
IMAGE_TAG=abc1234
```

Then run:

```bash
./deploy.sh
```

Check current service state and recent logs:

```bash
./check.sh
```

Follow container console logs:

```bash
docker compose --env-file .env -f docker-compose.prod.yml logs -f lingxi-admin
```

Read persisted server log files:

```bash
tail -f logs/all.log
tail -f logs/error.log
```

Rollback is the same command with the previous image tag:

```bash
./deploy.sh previous-good-tag
```

## 生产 .env 同步

`deploy/.env` 是**生产环境真实配置**（数据库与 Redis 密码、身份断言密钥、隐私导出密钥、
管理员初始口令、云存储密钥）。它被 `.gitignore` 忽略，**只存在于本地工作区和服务器两侧，绝不入库**。

本地与服务器各存一份，用 `deploy/sync-env.ps1` 保持一致：

```powershell
# 比对差异（默认动作，只读；值以长度形式打码，不会回显明文）
powershell -File deploy/sync-env.ps1 -Action compare

# 从服务器拉取，覆盖本地（自动备份旧文件到 .secrets/）
powershell -File deploy/sync-env.ps1 -Action pull

# 把本地推送到服务器（自动备份服务器原文件为 .env.bak-<时间戳>）
powershell -File deploy/sync-env.ps1 -Action push
```

约定与注意事项：

1. **`IMAGE_TAG` 由 CI 拥有。** GitHub Actions 每次部署都会把当前 commit SHA 写进服务器
   `.env`，本地那份通常是过期的。`push` 会**保留服务器上的 `IMAGE_TAG`**，不会用本地旧值覆盖，
   否则 `deploy.sh` 会去拉一个旧镜像。需要单独改版本请用 `./deploy.sh <tag>`。
2. **推送后需重启容器才生效**（`.env` 只在容器创建时读取）：
   ```bash
   ssh obsidian-server 'cd /data/mingus/lingxi/deploy && docker compose --env-file .env -f docker-compose.prod.yml up -d lingxi-admin'
   ```
3. **不要在服务器上手工编辑 `.env`。** 本地推送会整份覆盖。若确实在服务器改过，先 `pull` 回来。
   脚本检测到 `.env.swp`（存在编辑器会话）时会拒绝写入，避免覆盖未保存的改动；
   确认是僵尸文件后加 `-Force`。
4. **`.env.example` 是权威键清单**，新增配置项时要同步更新它，让新环境知道需要哪些键。
5. 管理员口令只在**账号不存在时**由 `AdminBootstrapInitializer` 写入；账号已存在后改
   `LINGXI_ADMIN_PASSWORD` 不会重置密码。忘记密码时通过管理后台「管理员管理 → 重置密码」
   或直接在库中改写 `id_admin_account.password_hash`。
6. CI **不上传 `.env`**（只上传 `docker-compose.prod.yml` 与 `deploy.sh`），所以同步只能由本地发起。
   若希望 CI 可部署全新环境，需要把配置改存到 GitHub Actions Secrets。
