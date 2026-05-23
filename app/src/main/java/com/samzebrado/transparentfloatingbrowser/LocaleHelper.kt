package com.samzebrado.transparentfloatingbrowser

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import java.util.Locale

object LocaleHelper {

    fun setLocale(context: Context, language: String): Context {
        val locale = Locale(language)
        Locale.setDefault(locale)

        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)

        return context.createConfigurationContext(config)
    }

    fun getLanguage(context: Context): String {
        val prefs = context.getSharedPreferences(AppPrefs.MAIN_PREFS, Context.MODE_PRIVATE)
        return prefs.getString(AppPrefs.KEY_LANGUAGE, Locale.getDefault().language) ?: AppPrefs.LANGUAGE_EN
    }

    fun saveLanguage(context: Context, language: String) {
        val prefs = context.getSharedPreferences(AppPrefs.MAIN_PREFS, Context.MODE_PRIVATE)
        prefs.edit().putString(AppPrefs.KEY_LANGUAGE, language).apply()
    }

    fun getLanguageButtonText(context: Context): String {
        val currentLang = getLanguage(context)
        return if (currentLang == AppPrefs.LANGUAGE_ZH) {
            context.getString(R.string.lang_switch)
        } else {
            context.getString(R.string.lang_switch_chinese)
        }
    }

    fun applyLanguage(activity: Activity) {
        val language = getLanguage(activity)
        LocaleHelper.setLocale(activity, language)
    }
}
