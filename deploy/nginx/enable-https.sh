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
# 两个必须显式指定的原因：
# 1) webroot 要用宿主机路径 —— nginx 容器把 /data/mingus/certbot/www 以只读方式挂到
#    /var/www/certbot，容器内路径对宿主机上的 certbot 不可写。该路径与服务器上
#    doc.mingusone.com 的 renewal 配置一致（family 那条写的是容器内路径，属历史遗留错误）。
# 2) certbot 默认读写 /etc/letsencrypt、/var/log/letsencrypt；本机的证书实际存放在
#    /data/mingus/letsencrypt（nginx 容器挂载的正是这个目录），且这些目录属 root，
#    因此必须用 sudo 并显式指定 config/work/logs 目录，否则报
#    "Permission denied: /var/log/letsencrypt/.certbot.lock"。
WEBROOT=/data/mingus/certbot/www
CERT_DIR=/data/mingus/letsencrypt
WORK_DIR=/data/mingus/certbot/work
LOGS_DIR=/data/mingus/certbot/logs
if [ ! -d "$WEBROOT" ]; then
  echo "  ✗ 找不到 webroot 目录 $WEBROOT" >&2
  exit 1
fi
CERTBOT="sudo -n certbot --config-dir $CERT_DIR --work-dir $WORK_DIR --logs-dir $LOGS_DIR"
if [ -f "$CERT_DIR/live/$DOMAIN/fullchain.pem" ]; then
  echo "  证书已存在，跳过签发"
else
  $CERTBOT certonly --webroot -w "$WEBROOT" -d "$DOMAIN" \
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

# lingxi-app.conf 只在「证书就绪前」使用；它与 https 配置都声明了
# listen 80 + server_name app.mingusone.com，两者共存时 nginx 会报
# "conflicting server name ... ignored" 并且只采用先加载的那个 server 块。
# 因此启用 https 配置后必须移除它（需要重新引导时再放回即可）。
if [ -f "$HTTP_CONF" ]; then
  mv "$HTTP_CONF" "$HTTP_CONF.bootstrap-disabled"
  echo "  ✓ 已停用 $HTTP_CONF（保留为 .bootstrap-disabled 备份）"
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
