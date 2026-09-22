package com.moldovan.ayuno

import android.app.Application
import android.content.Context
import com.moldovan.ayuno.data.LanguagePreference

class AyunoApplication : Application() {

    override fun attachBaseContext(base: Context) {
        LanguagePreference.ensureDefaultLanguage(base)
        super.attachBaseContext(LanguagePreference.applyLanguage(base))
    }
}
