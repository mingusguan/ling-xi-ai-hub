@echo off
echo.
echo [信息] 使用Jar命令运行AI服务。
echo.

cd %~dp0
cd ../lingxi-modules/lingxi-ai/target

set JAVA_OPTS=-Xms256m -Xmx512m -XX:MetaspaceSize=128m -XX:MaxMetaspaceSize=256m

java -jar %JAVA_OPTS% lingxi-ai.jar

cd bin
pause
