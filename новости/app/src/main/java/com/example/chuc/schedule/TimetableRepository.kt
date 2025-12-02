package com.example.chuc.schedule

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class TimetableRepository {
    suspend fun load(groupQuery: String? = null): List<TimetableDay> = withContext(Dispatchers.IO) {
        TimetableParser.fetchAndParse(groupQuery)
    }
}


