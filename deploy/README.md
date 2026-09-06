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
