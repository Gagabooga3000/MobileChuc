package com.example.chuc

import android.content.Context
import android.util.Log
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

/**
 * Диагностический инструмент для анализа проблем с парсингом расписания
 */
class ScheduleDiagnosticTool(private val context: Context) {
    
    companion object {
        private const val TAG = "ScheduleDiagnostic"
        private const val DIAGNOSTIC_DIR = "schedule_diagnostics"
    }
    
    /**
     * Полный анализ HTML расписания с сохранением результатов
     */
    fun analyzeScheduleHtml(html: String, groupName: String): DiagnosticResult {
        Log.d(TAG, "=== STARTING DIAGNOSTIC ANALYSIS FOR GROUP: $groupName ===")
        
        val timestamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault()).format(Date())
        val fileName = "schedule_${groupName}_$timestamp.html"
        
        // Сохраняем HTML для анализа
        saveHtmlToFile(html, fileName)
        
        val doc = Jsoup.parse(html)
        val result = DiagnosticResult(groupName, timestamp)
        
        // Анализируем структуру HTML
        analyzeHtmlStructure(doc, result)
        
        // Анализируем таблицы
        analyzeTables(doc, result)
        
        // Анализируем строки с парами
        analyzeLessonRows(doc, result)
        
        // Анализируем маркеры дней
        analyzeDayMarkers(doc, result)
        
        // Сохраняем результаты анализа
        saveAnalysisResult(result, fileName)
        
        Log.d(TAG, "=== DIAGNOSTIC ANALYSIS COMPLETED ===")
        return result
    }
    
    /**
     * Анализ общей структуры HTML
     */
    private fun analyzeHtmlStructure(doc: Document, result: DiagnosticResult) {
        Log.d(TAG, "Analyzing HTML structure...")
        
        result.htmlLength = doc.html().length
        result.totalElements = doc.select("*").size
        result.totalTables = doc.select("table").size
        result.totalRows = doc.select("tr").size
        result.totalCells = doc.select("td").size
        
        // Проверяем наличие ключевых элементов
        result.hasLoginForm = doc.html().contains("LoginForm") || 
                             doc.html().contains("авторизаци") || 
                             doc.html().contains("login")
        
        result.hasScheduleData = doc.select("table").isNotEmpty() && 
                                doc.select("tr").isNotEmpty()
        
        Log.d(TAG, "HTML Structure: ${result.totalElements} elements, ${result.totalTables} tables, ${result.totalRows} rows")
    }
    
    /**
     * Анализ таблиц
     */
    private fun analyzeTables(doc: Document, result: DiagnosticResult) {
        Log.d(TAG, "Analyzing tables...")
        
        val tables = doc.select("table")
        result.tableAnalysis = mutableListOf()
        
        for ((index, table) in tables.withIndex()) {
            val tableInfo = TableInfo(
                index = index,
                rowCount = table.select("tr").size,
                cellCount = table.select("td").size,
                hasHeaders = table.select("th").isNotEmpty(),
                firstRowCells = table.select("tr").firstOrNull()?.select("td, th")?.map { it.text().trim() } ?: emptyList()
            )
            
            result.tableAnalysis.add(tableInfo)
            Log.d(TAG, "Table $index: ${tableInfo.rowCount} rows, ${tableInfo.cellCount} cells")
            Log.d(TAG, "Table $index first row: ${tableInfo.firstRowCells}")
        }
    }
    
    /**
     * Анализ строк с парами
     */
    private fun analyzeLessonRows(doc: Document, result: DiagnosticResult) {
        Log.d(TAG, "Analyzing lesson rows...")
        
        val timePattern = Regex("\\d{1,2}:\\d{2}")
        val allRows = doc.select("tr")
        result.lessonAnalysis = mutableListOf()
        
        for ((rowIndex, row) in allRows.withIndex()) {
            val cells = row.select("td")
            if (cells.size < 2) continue
            
            val cellTexts = cells.map { it.text().trim() }
            
            // Ищем время
            var time = ""
            var timeIndex = -1
            for ((index, text) in cellTexts.withIndex()) {
                if (timePattern.matches(text)) {
                    time = text
                    timeIndex = index
                    break
                }
            }
            
            if (time.isEmpty()) continue
            
            // Ищем предмет
            var subject = ""
            for ((index, text) in cellTexts.withIndex()) {
                if (index != timeIndex && 
                    text.isNotEmpty() && 
                    text != time &&
                    text.length > 3 &&
                    text.matches(Regex(".*[а-яёА-ЯЁ].*")) &&
                    !text.matches(Regex("^[\\s\\-—+]+$"))) {
                    subject = text
                    break
                }
            }
            
            if (subject.isEmpty()) continue
            
            val lessonInfo = LessonInfo(
                rowIndex = rowIndex,
                time = time,
                subject = subject,
                allCells = cellTexts,
                cellCount = cells.size
            )
            
            result.lessonAnalysis.add(lessonInfo)
            Log.d(TAG, "Row $rowIndex: $time - $subject")
        }
        
        Log.d(TAG, "Found ${result.lessonAnalysis.size} lessons")
    }
    
    /**
     * Анализ маркеров дней недели
     */
    private fun analyzeDayMarkers(doc: Document, result: DiagnosticResult) {
        Log.d(TAG, "Analyzing day markers...")
        
        val dayPatterns = mapOf(
            "понедельник" to 0, "вторник" to 1, "среда" to 2,
            "четверг" to 3, "пятница" to 4, "суббота" to 5,
            "пн" to 0, "вт" to 1, "ср" to 2,
            "чт" to 3, "пт" to 4, "сб" to 5
        )
        
        val allElements = doc.select("*")
        result.dayMarkers = mutableListOf()
        
        for ((index, element) in allElements.withIndex()) {
            val text = element.text().trim().lowercase()
            for ((pattern, dayIndex) in dayPatterns) {
                if (text.contains(pattern)) {
                    val marker = DayMarker(
                        elementIndex = index,
                        dayIndex = dayIndex,
                        text = text,
                        tagName = element.tagName()
                    )
                    result.dayMarkers.add(marker)
                    Log.d(TAG, "Found day marker '$pattern' at element $index: $text")
                    break
                }
            }
        }
        
        Log.d(TAG, "Found ${result.dayMarkers.size} day markers")
    }
    
    /**
     * Сохранение HTML в файл
     */
    private fun saveHtmlToFile(html: String, fileName: String) {
        try {
            val file = File(context.getExternalFilesDir(DIAGNOSTIC_DIR), fileName)
            file.parentFile?.mkdirs()
            
            FileWriter(file).use { writer ->
                writer.write("<!-- Schedule HTML for analysis -->\n")
                writer.write("<!-- Generated at: ${Date()} -->\n")
                writer.write(html)
            }
            
            Log.d(TAG, "HTML saved to: ${file.absolutePath}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save HTML", e)
        }
    }
    
    /**
     * Сохранение результатов анализа
     */
    private fun saveAnalysisResult(result: DiagnosticResult, fileName: String) {
        try {
            val analysisFileName = fileName.replace(".html", "_analysis.txt")
            val file = File(context.getExternalFilesDir(DIAGNOSTIC_DIR), analysisFileName)
            
            FileWriter(file).use { writer ->
                writer.write("=== SCHEDULE DIAGNOSTIC ANALYSIS ===\n")
                writer.write("Group: ${result.groupName}\n")
                writer.write("Timestamp: ${result.timestamp}\n")
                writer.write("HTML Length: ${result.htmlLength}\n")
                writer.write("Total Elements: ${result.totalElements}\n")
                writer.write("Total Tables: ${result.totalTables}\n")
                writer.write("Total Rows: ${result.totalRows}\n")
                writer.write("Total Cells: ${result.totalCells}\n")
                writer.write("Has Login Form: ${result.hasLoginForm}\n")
                writer.write("Has Schedule Data: ${result.hasScheduleData}\n")
                writer.write("\n=== TABLES ANALYSIS ===\n")
                
                for (table in result.tableAnalysis) {
                    writer.write("Table ${table.index}: ${table.rowCount} rows, ${table.cellCount} cells\n")
                    writer.write("First row cells: ${table.firstRowCells}\n")
                }
                
                writer.write("\n=== LESSONS ANALYSIS ===\n")
                writer.write("Total lessons found: ${result.lessonAnalysis.size}\n")
                
                for (lesson in result.lessonAnalysis) {
                    writer.write("Row ${lesson.rowIndex}: ${lesson.time} - ${lesson.subject}\n")
                    writer.write("All cells: ${lesson.allCells}\n")
                }
                
                writer.write("\n=== DAY MARKERS ANALYSIS ===\n")
                writer.write("Total day markers: ${result.dayMarkers.size}\n")
                
                for (marker in result.dayMarkers) {
                    writer.write("Element ${marker.elementIndex}: Day ${marker.dayIndex} - ${marker.text}\n")
                }
            }
            
            Log.d(TAG, "Analysis result saved to: ${file.absolutePath}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save analysis result", e)
        }
    }
}

/**
 * Результат диагностического анализа
 */
data class DiagnosticResult(
    val groupName: String,
    val timestamp: String,
    var htmlLength: Int = 0,
    var totalElements: Int = 0,
    var totalTables: Int = 0,
    var totalRows: Int = 0,
    var totalCells: Int = 0,
    var hasLoginForm: Boolean = false,
    var hasScheduleData: Boolean = false,
    var tableAnalysis: MutableList<TableInfo> = mutableListOf(),
    var lessonAnalysis: MutableList<LessonInfo> = mutableListOf(),
    var dayMarkers: MutableList<DayMarker> = mutableListOf()
)

/**
 * Информация о таблице
 */
data class TableInfo(
    val index: Int,
    val rowCount: Int,
    val cellCount: Int,
    val hasHeaders: Boolean,
    val firstRowCells: List<String>
)

/**
 * Информация о паре
 */
data class LessonInfo(
    val rowIndex: Int,
    val time: String,
    val subject: String,
    val allCells: List<String>,
    val cellCount: Int
)

/**
 * Маркер дня недели
 */
data class DayMarker(
    val elementIndex: Int,
    val dayIndex: Int,
    val text: String,
    val tagName: String
)
