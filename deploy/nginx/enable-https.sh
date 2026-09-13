#!/bin/sh
# ============================================================================
#  灵犀伴行 PC 用户端：签发 HTTPS 证书并启用 443 vhost
#  ---------------------------------------------------------------------------
#  前置条件：app.mingusone.com 的 A 记录已指向本机公网 IP，且 80 端口可从公网访问。
#  用法（在服务器 /data/mingus/lingxi/deploy/nginx 或任意目录）：
#    sh enable-https.sh
#  幂等：证书已存在时跳过签发，只做配置启用与 reload。
# ============================================================================
set -eu

DOMAIN=app.mingusone.com
CONF_DIR=/data/mingus/nginx/conf.d
HTTP_CONF="$CONF_DIR/lingxi-app.conf"
HTTPS_CONF="$CONF_DIR/lingxi-app-https.conf"

echo "[1/5] 检查 DNS 解析"
RESOLVED=$(getent hosts "$DOMAIN" | awk '{print $1}' | head -1 || true)
if [ -z "$RESOLVED" ]; then
  echo "  ✗ $DOMAIN 尚未解析，请先在 DNS 添加 A 记录后重试" >&2
  exit 1
fi
echo "  ✓ $DOMAIN -> $RESOLVED"

echo "[2/5] 申请/续期证书（webroot 校验）"
if [ -f "/etc/letsencrypt/live/$DOMAIN/fullchain.pem" ]; then
  echo "  证书已存在，跳过签发"
else
  certbot certonly --webroot -w /var/www/certbot -d "$DOMAIN" \
    --non-interactive --agree-tos --register-unsafely-without-email \
    --keep-until-expiring
fi

echo "[3/5] 启用 443 vhost"
if [ -f "$HTTPS_CONF" ]; then
  echo "  $HTTPS_CONF 已启用"
elif [ -f "$HTTPS_CONF.disabled" ]; then
  mv "$HTTPS_CONF.disabled" "$HTTPS_CONF"
  echo "  ✓ 已启用 $HTTPS_CONF"
else
  echo "  ✗ 找不到 $HTTPS_CONF(.disabled)" >&2
  exit 1
fi

echo "[4/5] 校验并重载 nginx"
docker exec mingus-nginx nginx -t
docker exec mingus-nginx nginx -s reload
echo "  ✓ 已重载"

echo "[5/5] 校验 HTTPS"
CODE=$(curl -s -o /dev/null -w '%{http_code}' --max-time 15 "https://$DOMAIN/" || echo "000")
echo "  https://$DOMAIN/ -> $CODE"
CODE_API=$(curl -s -o /dev/null -w '%{http_code}' --max-time 15 "https://$DOMAIN/api/v1/goals" || echo "000")
echo "  https://$DOMAIN/api/v1/goals -> $CODE_API （401 = 反代正常，未登录）"
echo "完成。"
