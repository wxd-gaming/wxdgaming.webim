@echo off

set MY_CURRENT_PATH=%CD%
echo %MY_CURRENT_PATH%

if "%1"=="" (
    cd ..\wxdgaming.boot2
    echo %CD%
    @REM 编译本地服
    call builder\mvnw-install.cmd
    REM 检查errorlevel的值
    if %errorlevel% equ 0 (
        echo "wxdgaming.boot2\builder\mvnw-install.cmd success。"
    ) else (
        echo "wxdgaming.boot2\builder\mvnw-install.cmd error level: %errorlevel%"
        exit
    )
)

cd %MY_CURRENT_PATH%
echo %CD%

call .mvn\mvnw-install.cmd