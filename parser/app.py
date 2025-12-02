#!/usr/bin/env python3
# -*- coding: utf-8 -*-

from flask import Flask, render_template, jsonify, request
import json
import threading
import time
from datetime import datetime
from parser import ScheduleParser
import urllib.parse

app = Flask(__name__)
app.config['JSON_AS_ASCII'] = False  # Для корректного отображения русских символов в JSON

# Глобальные переменные для хранения данных
schedule_data = {
    "groups": [],
    "teachers": [],
    "last_update": None,
    "error": None
}

parser = ScheduleParser()

def update_schedule_data():
    """Обновление данных расписания в фоновом режиме"""
    global schedule_data
    
    while True:
        try:
            print(f"[{datetime.now()}] Обновление данных расписания...")
            
            # Получаем данные групп
            groups_data = parser.get_groups_schedule()
            if "error" not in groups_data:
                schedule_data["groups"] = groups_data["groups"]
            
            # Получаем данные преподавателей
            teachers_data = parser.get_teachers_schedule()
            if "error" not in teachers_data:
                schedule_data["teachers"] = teachers_data["teachers"]
            
            schedule_data["last_update"] = datetime.now().isoformat()
            schedule_data["error"] = None
            
            print(f"[{datetime.now()}] Данные обновлены успешно")
            
        except Exception as e:
            schedule_data["error"] = str(e)
            print(f"[{datetime.now()}] Ошибка обновления: {e}")
        
        # Ждем 5 минут перед следующим обновлением
        time.sleep(300)

@app.route('/')
def index():
    """Главная страница"""
    return render_template('index.html', 
                         groups=schedule_data["groups"],
                         teachers=schedule_data["teachers"],
                         last_update=schedule_data["last_update"])

@app.route('/api/schedule')
def api_schedule():
    """API для получения данных расписания"""
    return jsonify(schedule_data)

@app.route('/api/groups')
def api_groups():
    """API для получения списка групп"""
    return jsonify({"groups": schedule_data["groups"]})

@app.route('/api/teachers')
def api_teachers():
    """API для получения списка преподавателей"""
    return jsonify({"teachers": schedule_data["teachers"]})

@app.route('/schedule/group/<path:group_name>')
def group_schedule(group_name):
    """Страница расписания конкретной группы"""
    try:
        # Декодируем URL если нужно
        group_name = urllib.parse.unquote(group_name)
        schedule_data_detail = parser.get_schedule_data(group_name, "group")
        if "error" in schedule_data_detail:
            return render_template('error.html', error=schedule_data_detail["error"])
        
        return render_template('schedule.html', 
                             schedule_data=schedule_data_detail,
                             title=f"Расписание группы {group_name}")
    except Exception as e:
        return render_template('error.html', error=str(e))

@app.route('/schedule/teacher/<path:teacher_name>')
def teacher_schedule(teacher_name):
    """Страница расписания конкретного преподавателя"""
    try:
        # Декодируем URL если нужно
        teacher_name = urllib.parse.unquote(teacher_name)
        schedule_data_detail = parser.get_schedule_data(teacher_name, "teacher")
        if "error" in schedule_data_detail:
            return render_template('error.html', error=schedule_data_detail["error"])
        
        return render_template('schedule.html', 
                             schedule_data=schedule_data_detail,
                             title=f"Расписание преподавателя {teacher_name}")
    except Exception as e:
        return render_template('error.html', error=str(e))

@app.route('/api/schedule/groups')
def api_groups_schedule():
    """API для получения расписания групп"""
    return jsonify(schedule_data.get("groups", []))

@app.route('/api/schedule/teachers')
def api_teachers_schedule():
    """API для получения расписания преподавателей"""
    return jsonify(schedule_data.get("teachers", []))

@app.route('/api/update')
def manual_update():
    """Ручное обновление расписания"""
    try:
        # Получаем данные групп
        groups_data = parser.get_groups_schedule()
        if "error" not in groups_data:
            schedule_data["groups"] = groups_data["groups"]
        
        # Получаем данные преподавателей
        teachers_data = parser.get_teachers_schedule()
        if "error" not in teachers_data:
            schedule_data["teachers"] = teachers_data["teachers"]
        
        schedule_data["last_update"] = datetime.now().isoformat()
        schedule_data["error"] = None
        
        return jsonify({"status": "success", "last_update": schedule_data["last_update"]})
    except Exception as e:
        return jsonify({"status": "error", "message": str(e)})

if __name__ == '__main__':
    # Запускаем обновление данных в фоновом потоке
    update_thread = threading.Thread(target=update_schedule_data, daemon=True)
    update_thread.start()
    
    print("Запуск веб-сервера...")
    print("Сайт доступен по адресу: http://localhost:5000")
    
    app.run(debug=True, host='0.0.0.0', port=5000)
