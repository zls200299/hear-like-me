@CHCP 65001 >NUL
@echo off
cd /d "%~dp0"
echo [Spring-Generator] 使用 mvn clean javafx:run 启动（勿直接运行 Main）
mvn -e clean javafx:run
if errorlevel 1 pause
