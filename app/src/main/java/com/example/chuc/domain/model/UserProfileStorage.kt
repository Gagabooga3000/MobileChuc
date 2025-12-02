package com.example.chuc.domain.model

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.gson.Gson

object UserProfileStorage {
    private const val PREFS_NAME = "user_profile_prefs"
    private const val KEY_PROFILE = "user_profile"
    
    fun save(context: Context, profile: UserProfile) {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val gson = Gson()
            val json = gson.toJson(profile)
            prefs.edit()
                .putString(KEY_PROFILE, json)
                .apply()
            Log.d("UserProfileStorage", "Profile saved: $profile")
        } catch (e: Exception) {
            Log.e("UserProfileStorage", "Error saving profile", e)
        }
    }
    
    fun load(context: Context): UserProfile? {
        return try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val json = prefs.getString(KEY_PROFILE, null)
            if (json != null) {
                val gson = Gson()
                gson.fromJson(json, UserProfile::class.java)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("UserProfileStorage", "Error loading profile", e)
            null
        }
    }
    
    fun clear(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .remove(KEY_PROFILE)
            .apply()
    }
}

