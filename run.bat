@echo off
chcp 65001 > nul
echo [Activity] Запуск тестового клиента Minecraft 1.21.11...
subst X: "%~dp0" > nul 2>&1
if exist X:\ (
    pushd X:\L
    call gradlew.bat runClient
    popd
    subst X: /d > nul 2>&1
) else (
    call gradlew.bat runClient
)
