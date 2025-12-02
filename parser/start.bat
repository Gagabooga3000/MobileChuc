@echo off
chcp 65001 >nul
echo Запуск парсера расписания...
echo.
echo Установка зависимостей...
pip install -r requirements.txt
echo.
echo Запуск веб-сервера...
echo Сайт будет доступен по адресу: http://localhost:5000
echo Для остановки нажмите Ctrl+C
echo.
python app.py
pause