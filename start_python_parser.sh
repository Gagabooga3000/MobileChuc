#!/bin/bash

# Скрипт для запуска Python парсера
# Использование: ./start_python_parser.sh

echo "Запуск Python парсера для Android приложения..."

# Проверяем наличие Python
if ! command -v python3 &> /dev/null; then
    echo "Ошибка: Python3 не найден. Установите Python3 для продолжения."
    exit 1
fi

# Переходим в папку парсера
cd parser

# Проверяем наличие requirements.txt
if [ ! -f "requirements.txt" ]; then
    echo "Ошибка: Файл requirements.txt не найден в папке parser/"
    exit 1
fi

# Устанавливаем зависимости
echo "Установка зависимостей..."
pip3 install -r requirements.txt

# Проверяем наличие app.py
if [ ! -f "app.py" ]; then
    echo "Ошибка: Файл app.py не найден в папке parser/"
    exit 1
fi

# Запускаем сервер
echo "Запуск сервера на http://localhost:5000"
echo "Для остановки нажмите Ctrl+C"
echo ""

python3 app.py

