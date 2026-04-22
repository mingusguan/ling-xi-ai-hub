@echo off
echo.
echo [��Ϣ] ʹ��Jar��������Auth���̡�
echo.

cd %~dp0
cd ../cloud-auth/target

set JAVA_OPTS=-Xms512m -Xmx1024m -XX:MetaspaceSize=128m -XX:MaxMetaspaceSize=512m

java -Dfile.encoding=utf-8 %JAVA_OPTS% -jar cloud-auth.jar

cd bin
pause