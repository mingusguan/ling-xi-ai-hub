@echo off
echo.
echo [��Ϣ] ʹ��Jar��������Modules-Job���̡�
echo.

cd %~dp0
cd ../lingxi-modules/cloud-job/target

set JAVA_OPTS=-Xms512m -Xmx1024m -XX:MetaspaceSize=128m -XX:MaxMetaspaceSize=512m

java -Dfile.encoding=utf-8 %JAVA_OPTS% -jar cloud-modules-job.jar

cd bin
pause