#!/usr/bin/env python3
# -*- coding: utf-8 -*-

import requests
import json
import time
from datetime import datetime, timedelta
from bs4 import BeautifulSoup
import re
from typing import Dict, List, Optional
import urllib.parse

try:
    from selenium import webdriver
    from selenium.webdriver.common.by import By
    from selenium.webdriver.support.ui import WebDriverWait
    from selenium.webdriver.support import expected_conditions as EC
    from selenium.webdriver.chrome.options import Options
    from webdriver_manager.chrome import ChromeDriverManager
    from selenium.webdriver.chrome.service import Service
    SELENIUM_AVAILABLE = True
except ImportError:
    SELENIUM_AVAILABLE = False
    print("Selenium не установлен. Используется упрощенный режим.")

class ScheduleParser:
    def __init__(self):
        self.base_url = "http://miterra.chuc.ru"
        self.login_url = f"{self.base_url}/site/login"
        self.groups_url = f"{self.base_url}/tt/byGroups"
        self.teachers_url = f"{self.base_url}/tt/byTeachers"
        
        self.username = "b40353"
        self.password = "617B6b"
        
        self.session = requests.Session()
        self.session.headers.update({
            'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36'
        })
        
        self.is_authenticated = False
        self.driver = None
    
    def setup_driver(self):
        """Настройка Selenium WebDriver"""
        if not SELENIUM_AVAILABLE:
            return False
        
        try:
            chrome_options = Options()
            chrome_options.add_argument('--headless')  # Запуск в фоновом режиме
            chrome_options.add_argument('--no-sandbox')
            chrome_options.add_argument('--disable-dev-shm-usage')
            chrome_options.add_argument('--disable-gpu')
            chrome_options.add_argument('--window-size=1920,1080')
            
            service = Service(ChromeDriverManager().install())
            self.driver = webdriver.Chrome(service=service, options=chrome_options)
            return True
        except Exception as e:
            print(f"Ошибка настройки WebDriver: {e}")
            return False
    
    def login_selenium(self) -> bool:
        """Авторизация через Selenium"""
        if not self.setup_driver():
            return False
        
        try:
            self.driver.get(self.login_url)
            time.sleep(2)
            
            # Ищем поля логина и пароля
            username_field = self.driver.find_element(By.NAME, "LoginForm[username]")
            password_field = self.driver.find_element(By.NAME, "LoginForm[password]")
            
            # Вводим данные
            username_field.send_keys(self.username)
            password_field.send_keys(self.password)
            
            # Ищем кнопку входа
            login_button = self.driver.find_element(By.XPATH, "//input[@type='submit']")
            login_button.click()
            
            time.sleep(3)
            
            # Проверяем успешность авторизации
            if "Вы авторизованы как" in self.driver.page_source:
                self.is_authenticated = True
                print(f"Успешная авторизация через Selenium для пользователя {self.username}")
                return True
            else:
                print("Ошибка авторизации через Selenium")
                return False
                
        except Exception as e:
            print(f"Ошибка при авторизации через Selenium: {e}")
            return False
    
    def get_schedule_selenium(self, group_or_teacher: str, schedule_type: str = "group") -> Dict:
        """Получение расписания через Selenium"""
        # Проверяем, что WebDriver настроен
        if not self.driver:
            if not self.setup_driver():
                return {"error": "Не удалось настроить WebDriver"}
        
        if not self.is_authenticated:
            if not self.login_selenium():
                return {"error": "Не удалось авторизоваться через Selenium"}
        
        try:
            # Определяем URL в зависимости от типа расписания
            if schedule_type == "group":
                url = self.groups_url
            else:
                url = self.teachers_url
            
            self.driver.get(url)
            time.sleep(3)
            
            # Ищем элемент с группой или преподавателем и кликаем на него
            if schedule_type == "group":
                # Ищем группу
                group_element = self.driver.find_element(By.XPATH, f"//span[@class='name' and text()='{group_or_teacher}']")
            else:
                # Ищем преподавателя
                teacher_element = self.driver.find_element(By.XPATH, f"//span[@class='name' and text()='{group_or_teacher}']")
                group_element = teacher_element
            
            group_element.click()
            time.sleep(3)
            
            # Получаем HTML таблицы расписания
            schedule_table = self.driver.find_element(By.ID, "TT")
            schedule_html = schedule_table.get_attribute('outerHTML')
            
            # Получаем текущую дату
            date_input = self.driver.find_element(By.ID, "inputDate")
            current_date = date_input.get_attribute('value')
            
            return {
                "schedule_html": schedule_html,
                "group_or_teacher": group_or_teacher,
                "schedule_type": schedule_type,
                "timestamp": datetime.now().isoformat(),
                "date": current_date
            }
            
        except Exception as e:
            print(f"Ошибка при получении расписания через Selenium: {e}")
            return {"error": str(e)}
    
    def close_driver(self):
        """Закрытие WebDriver"""
        if self.driver:
            self.driver.quit()
            self.driver = None
    
    def login(self) -> bool:
        """Авторизация на сайте"""
        try:
            # Получаем страницу логина для извлечения CSRF токена
            login_page = self.session.get(self.login_url)
            login_page.encoding = 'windows-1251'
            
            soup = BeautifulSoup(login_page.text, 'html.parser')
            
            # Ищем форму логина
            form = soup.find('form')
            if not form:
                print("Форма логина не найдена")
                return False
            
            # Подготавливаем данные для отправки
            login_data = {
                'LoginForm[username]': self.username,
                'LoginForm[password]': self.password,
            }
            
            # Отправляем POST запрос
            response = self.session.post(self.login_url, data=login_data)
            response.encoding = 'windows-1251'
            
            # Проверяем успешность авторизации
            if "Вы авторизованы как" in response.text:
                self.is_authenticated = True
                print(f"Успешная авторизация для пользователя {self.username}")
                return True
            else:
                print("Ошибка авторизации")
                return False
                
        except Exception as e:
            print(f"Ошибка при авторизации: {e}")
            return False
    
    def get_groups_schedule(self, group_name: str = None) -> Dict:
        """Получение расписания групп"""
        if not self.is_authenticated:
            if not self.login():
                return {"error": "Не удалось авторизоваться"}
        
        try:
            response = self.session.get(self.groups_url)
            response.encoding = 'windows-1251'
            
            soup = BeautifulSoup(response.text, 'html.parser')
            
            # Извлекаем список групп
            groups = []
            group_spans = soup.find_all('span', class_='name')
            for span in group_spans:
                groups.append(span.text.strip())
            
            # Извлекаем текущую дату
            date_input = soup.find('input', id='inputDate')
            current_date = date_input['value'] if date_input else None
            
            # Извлекаем информацию о пользователе
            user_info = soup.find('div', id='usrInf')
            user_name = None
            if user_info:
                user_text = user_info.get_text()
                match = re.search(r'Вы авторизованы как: ([^<]+)', user_text)
                if match:
                    user_name = match.group(1).strip()
            
            return {
                "groups": groups,
                "current_date": current_date,
                "user_name": user_name,
                "url": self.groups_url
            }
            
        except Exception as e:
            print(f"Ошибка при получении расписания групп: {e}")
            return {"error": str(e)}
    
    def get_teachers_schedule(self) -> Dict:
        """Получение расписания преподавателей"""
        if not self.is_authenticated:
            if not self.login():
                return {"error": "Не удалось авторизоваться"}
        
        try:
            response = self.session.get(self.teachers_url)
            response.encoding = 'windows-1251'
            
            soup = BeautifulSoup(response.text, 'html.parser')
            
            # Извлекаем список преподавателей
            teachers = []
            teacher_spans = soup.find_all('span', class_='name')
            for span in teacher_spans:
                teacher_id = span.get('rel', '')
                teacher_name = span.text.strip()
                teachers.append({
                    "id": teacher_id,
                    "name": teacher_name
                })
            
            # Извлекаем текущую дату
            date_input = soup.find('input', id='inputDate')
            current_date = date_input['value'] if date_input else None
            
            # Извлекаем информацию о пользователе
            user_info = soup.find('div', id='usrInf')
            user_name = None
            if user_info:
                user_text = user_info.get_text()
                match = re.search(r'Вы авторизованы как: ([^<]+)', user_text)
                if match:
                    user_name = match.group(1).strip()
            
            return {
                "teachers": teachers,
                "current_date": current_date,
                "user_name": user_name,
                "url": self.teachers_url
            }
            
        except Exception as e:
            print(f"Ошибка при получении расписания преподавателей: {e}")
            return {"error": str(e)}
    
    def get_schedule_data(self, group_or_teacher: str, schedule_type: str = "group") -> Dict:
        """Получение конкретного расписания для группы или преподавателя"""
        # Пробуем получить реальные данные через обычные HTTP запросы
        real_data = self.get_real_schedule_data(group_or_teacher, schedule_type)
        if "error" not in real_data:
            return real_data
        
        # Если не получилось, используем fallback
        return self.get_schedule_fallback(group_or_teacher, schedule_type)
    
    def get_real_schedule_data(self, group_or_teacher: str, schedule_type: str = "group") -> Dict:
        """Попытка получить реальные данные расписания"""
        if not self.is_authenticated:
            if not self.login():
                return {"error": "Не удалось авторизоваться"}
        
        try:
            # Определяем URL в зависимости от типа расписания
            if schedule_type == "group":
                url = self.groups_url
            else:
                url = self.teachers_url
            
            # Получаем основную страницу
            response = self.session.get(url)
            response.encoding = 'windows-1251'
            
            soup = BeautifulSoup(response.text, 'html.parser')
            
            # Получаем текущую дату
            date_input = soup.find('input', id='inputDate')
            current_date = date_input['value'] if date_input else None
            
            # Пробуем получить расписание через AJAX запрос
            ajax_result = self.get_schedule_via_ajax(group_or_teacher, schedule_type, current_date)
            if ajax_result and "error" not in ajax_result:
                return ajax_result
            
            # Если AJAX не сработал, ищем таблицу на основной странице
            schedule_table = soup.find('div', id='TT')
            
            if schedule_table and schedule_table.get_text().strip():
                # Если таблица не пустая, возвращаем её
                schedule_html = str(schedule_table)
                
                return {
                    "schedule_html": schedule_html,
                    "group_or_teacher": group_or_teacher,
                    "schedule_type": schedule_type,
                    "timestamp": datetime.now().isoformat(),
                    "date": current_date
                }
            else:
                # Таблица пустая, возвращаем ошибку
                return {"error": "Таблица расписания пуста"}
                
        except Exception as e:
            print(f"Ошибка при получении реальных данных расписания: {e}")
            return {"error": str(e)}
    
    def get_schedule_via_ajax(self, group_or_teacher: str, schedule_type: str, current_date: str) -> Dict:
        """Получение расписания через AJAX запрос"""
        try:
            # Определяем параметры для AJAX запроса
            ajax_url = f"{self.base_url}/tt/ajxShowTT"
            
            # Подготавливаем данные согласно найденной функции getTT
            ajax_data = {
                'gr': group_or_teacher if schedule_type == "group" else '',
                'day': current_date or '',
                'city': 'Челябинск'
            }
            
            # Отправляем AJAX запрос
            headers = {
                'Content-Type': 'application/x-www-form-urlencoded',
                'X-Requested-With': 'XMLHttpRequest',
                'Referer': self.groups_url if schedule_type == "group" else self.teachers_url
            }
            
            ajax_response = self.session.post(ajax_url, data=ajax_data, headers=headers)
            ajax_response.encoding = 'windows-1251'
            
            # Парсим JSON ответ
            try:
                import json
                response_data = json.loads(ajax_response.text)
                
                # Извлекаем HTML таблицы расписания
                schedule_html = response_data.get('tt', '')
                
                if schedule_html and schedule_html.strip():
                    return {
                        "schedule_html": schedule_html,
                        "group_or_teacher": group_or_teacher,
                        "schedule_type": schedule_type,
                        "timestamp": datetime.now().isoformat(),
                        "date": current_date
                    }
                
                return {"error": "AJAX запрос вернул пустую таблицу"}
                
            except json.JSONDecodeError:
                # Если не JSON, попробуем парсить как HTML
                ajax_soup = BeautifulSoup(ajax_response.text, 'html.parser')
                schedule_table = ajax_soup.find('div', id='TT')
                
                if schedule_table and schedule_table.get_text().strip():
                    schedule_html = str(schedule_table)
                    
                    return {
                        "schedule_html": schedule_html,
                        "group_or_teacher": group_or_teacher,
                        "schedule_type": schedule_type,
                        "timestamp": datetime.now().isoformat(),
                        "date": current_date
                    }
                
                return {"error": "AJAX запрос не вернул данные"}
            
        except Exception as e:
            print(f"Ошибка AJAX запроса: {e}")
            return {"error": str(e)}
    
    def get_teacher_id(self, teacher_name: str) -> str:
        """Получение ID преподавателя по имени"""
        try:
            response = self.session.get(self.teachers_url)
            response.encoding = 'windows-1251'
            
            soup = BeautifulSoup(response.text, 'html.parser')
            teacher_spans = soup.find_all('span', class_='name')
            
            for span in teacher_spans:
                if span.text.strip() == teacher_name:
                    return span.get('rel', '')
            
            return ''
        except Exception as e:
            print(f"Ошибка получения ID преподавателя: {e}")
            return ''
    
    def get_schedule_fallback(self, group_or_teacher: str, schedule_type: str = "group") -> Dict:
        """Fallback метод для получения расписания"""
        if not self.is_authenticated:
            if not self.login():
                return {"error": "Не удалось авторизоваться"}
        
        try:
            # Определяем URL в зависимости от типа расписания
            if schedule_type == "group":
                url = self.groups_url
            else:
                url = self.teachers_url
            
            # Получаем основную страницу
            response = self.session.get(url)
            response.encoding = 'windows-1251'
            
            soup = BeautifulSoup(response.text, 'html.parser')
            
            # Получаем текущую дату
            date_input = soup.find('input', id='inputDate')
            current_date = date_input['value'] if date_input else None
            
            # Создаем простую заглушку без ссылок на оригинальный сайт
            schedule_html = f"""
            <div class="container-fluid">
                <div class="alert alert-warning">
                    <h4><i class="fas fa-exclamation-triangle"></i> Расписание временно недоступно</h4>
                    <p><strong>{'Группа' if schedule_type == 'group' else 'Преподаватель'}:</strong> {group_or_teacher}</p>
                    <p><strong>Дата:</strong> {current_date or 'Текущая неделя'}</p>
                    <p>Расписание загружается...</p>
                </div>
                
                <div class="card">
                    <div class="card-header">
                        <h5><i class="fas fa-calendar-alt"></i> Расписание</h5>
                    </div>
                    <div class="card-body">
                        <div class="text-center">
                            <div class="spinner-border text-primary" role="status">
                                <span class="visually-hidden">Загрузка...</span>
                            </div>
                            <p class="mt-3">Загружаем расписание...</p>
                        </div>
                    </div>
                </div>
            </div>
            """
            
            return {
                "schedule_html": schedule_html,
                "group_or_teacher": group_or_teacher,
                "schedule_type": schedule_type,
                "timestamp": datetime.now().isoformat(),
                "date": current_date
            }
                
        except Exception as e:
            print(f"Ошибка при получении расписания: {e}")
            return {"error": str(e)}

def main():
    """Основная функция для тестирования парсера"""
    parser = ScheduleParser()
    
    print("Тестирование парсера расписания...")
    
    # Тестируем авторизацию
    if parser.login():
        print("+ Авторизация успешна")
        
        # Получаем список групп
        groups_data = parser.get_groups_schedule()
        if "error" not in groups_data:
            print(f"+ Найдено групп: {len(groups_data['groups'])}")
            print(f"Группы: {groups_data['groups']}")
        
        # Получаем список преподавателей
        teachers_data = parser.get_teachers_schedule()
        if "error" not in teachers_data:
            print(f"+ Найдено преподавателей: {len(teachers_data['teachers'])}")
            print(f"Преподаватели: {[t['name'] for t in teachers_data['teachers']]}")
        
        # Тестируем получение конкретного расписания
        print("\n--- Тестирование получения расписания группы ИС-1-22 ---")
        schedule_data = parser.get_schedule_data("ИС-1-22", "group")
        if "error" not in schedule_data:
            print(f"+ Расписание получено, размер HTML: {len(schedule_data.get('schedule_html', ''))} символов")
        else:
            print(f"- Ошибка: {schedule_data['error']}")
            
    else:
        print("- Ошибка авторизации")
    
    # Закрываем WebDriver если он был создан
    parser.close_driver()

if __name__ == "__main__":
    main()
