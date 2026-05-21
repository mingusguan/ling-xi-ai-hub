# LingXi production release

This deployment keeps production release manual:

1. Push code to GitHub.
2. GitHub Actions builds and pushes the Docker image.
3. Confirm the version.
4. SSH to the production server.
5. Update `IMAGE_TAG` in `.env`, or pass it to `deploy.sh`.
6. Run `docker compose pull && docker compose up -d` through `deploy.sh`.
7. Check logs and the health URL.

## GitHub Actions

Edit `.github/workflows/build-image.yml` before enabling the pipeline:

- `IMAGE_REGISTRY`: default `registry.cn-hangzhou.aliyuncs.com`
- `IMAGE_NAMESPACE`: for example `your-namespace`
- `BACKEND_IMAGE_NAME`: default `lingxi-ai-hub`
- `FRONTEND_IMAGE_NAME`: default `lingxi-ui`
- `DOCKER_USERNAME` and `DOCKER_PASSWORD`: configure them as GitHub Actions repository secrets, do not commit real secrets

The generated image tag is:

```text
registry.cn-hangzhou.aliyuncs.com/your-namespace/lingxi-ai-hub:${GITHUB_SHA}
registry.cn-hangzhou.aliyuncs.com/your-namespace/lingxi-ai-hub:${GITHUB_SHA::7}
registry.cn-hangzhou.aliyuncs.com/your-namespace/lingxi-ui:${GITHUB_SHA}
registry.cn-hangzhou.aliyuncs.com/your-namespace/lingxi-ui:${GITHUB_SHA::7}
```

When a `v*` Git tag triggers the workflow, the image is also pushed with that Git tag.

You can also trigger the first build from GitHub Actions with `Run workflow`.

## Server setup

Copy these files to the production server, usually under `/data/lingxi`:

```text
deploy/docker-compose.prod.yml
deploy/deploy.sh
deploy/check.sh
deploy/.env.example
```

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

## Release

Deploy a specific image tag:

```bash
./deploy.sh abc1234
```

Or edit `.env` manually:

```text
IMAGE_TAG=abc1234
UI_IMAGE_TAG=abc1234
```

Then run:

```bash
./deploy.sh
```

Check current service state and recent logs:

```bash
./check.sh
```

Rollback is the same command with the previous image tag:

```bash
./deploy.sh previous-good-tag
```
