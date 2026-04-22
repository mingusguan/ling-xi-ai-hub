#!/bin/sh

# 复制项目的文件到对应docker路径，便于一键生成镜像。
usage() {
	echo "Usage: sh copy.sh"
	exit 1
}


# copy sql
echo "begin copy sql "
cp ../sql/ry_20250523.sql ./mysql/db
cp ../sql/ry_config_20250902.sql ./mysql/db

# copy html
echo "begin copy html "
cp -r ../cloud-ui/dist/** ./nginx/html/dist


# copy jar
echo "begin copy cloud-gateway "
cp ../cloud-gateway/target/cloud-gateway.jar ./cloud/gateway/jar

echo "begin copy cloud-auth "
cp ../cloud-auth/target/cloud-auth.jar ./cloud/auth/jar

echo "begin copy cloud-visual "
cp ../cloud-visual/cloud-monitor/target/cloud-visual-monitor.jar  ./cloud/visual/monitor/jar

echo "begin copy cloud-modules-system "
cp ../cloud-modules/cloud-system/target/cloud-modules-system.jar ./cloud/modules/system/jar

echo "begin copy cloud-modules-file "
cp ../cloud-modules/cloud-file/target/cloud-modules-file.jar ./cloud/modules/file/jar

echo "begin copy cloud-modules-job "
cp ../cloud-modules/cloud-job/target/cloud-modules-job.jar ./cloud/modules/job/jar

echo "begin copy cloud-modules-gen "
cp ../cloud-modules/cloud-gen/target/cloud-modules-gen.jar ./cloud/modules/gen/jar

