@echo off
REM Скрипт для запуска Python парсера на Windows
REM Использование: start_python_parser.bat

echo Запуск Python парсера для Android приложения...

REM Проверяем наличие Python
python --version >nul 2>&1
if errorlevel 1 (
    echo Ошибка: Python не найден. Установите Python для продолжения.
    pause
    exit /b 1
)

REM Переходим в папку парсера
cd parser

REM Проверяем наличие requirements.txt
if not exist "requirements.txt" (
    echo Ошибка: Файл requirements.txt не найден в папке parser/
    pause
    exit /b 1
)

REM Устанавливаем зависимости
echo Установка зависимостей...
pip install -r requirements.txt

REM Проверяем наличие app.py
if not exist "app.py" (
    echo Ошибка: Файл app.py не найден в папке parser/
    pause
    exit /b 1
)

REM Запускаем сервер
echo Запуск сервера на http://localhost:5000
echo Для остановки нажмите Ctrl+C
echo.

python app.py

pause

