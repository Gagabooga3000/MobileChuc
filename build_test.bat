@echo off
echo Проверка сборки Android проекта...

REM Переходим в корневую папку проекта
cd /d "%~dp0"

REM Проверяем наличие gradlew.bat
if not exist "gradlew.bat" (
    echo Ошибка: gradlew.bat не найден в корневой папке проекта
    pause
    exit /b 1
)

echo Очистка проекта...
gradlew.bat clean

if errorlevel 1 (
    echo Ошибка при очистке проекта
    pause
    exit /b 1
)

echo Попытка сборки проекта...
gradlew.bat assembleDebug

if errorlevel 1 (
    echo Ошибка при сборке проекта
    pause
    exit /b 1
) else (
    echo Проект успешно собран!
)

pause

