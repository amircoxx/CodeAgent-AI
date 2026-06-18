@echo off
set BASE_DIR=%~dp0
set WRAPPER_JAR=%BASE_DIR%\.mvn\wrapper\maven-wrapper.jar
set WRAPPER_PROPS=%BASE_DIR%\.mvn\wrapper\maven-wrapper.properties

if not exist "%WRAPPER_JAR%" (
  for /f "tokens=2 delims==" %%i in ('findstr wrapperUrl "%WRAPPER_PROPS%"') do powershell -NoProfile -ExecutionPolicy Bypass -Command "Invoke-WebRequest -Uri '%%i' -OutFile '%WRAPPER_JAR%'"
)

java -Dmaven.multiModuleProjectDirectory="%BASE_DIR%" -classpath "%WRAPPER_JAR%" org.apache.maven.wrapper.MavenWrapperMain %*
