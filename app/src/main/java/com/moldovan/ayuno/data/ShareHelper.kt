package com.moldovan.ayuno.data

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ShareHelper {

    fun createShareIntent(context: Context, session: FastingSession): Intent {
        val bitmap = generateShareImage(session)
        val file   = saveBitmapToCache(context, bitmap)
        val uri    = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val durationMs  = (session.endTime ?: System.currentTimeMillis()) - session.startTime
        val durationH   = durationMs / 3_600_000
        val durationMin = (durationMs % 3_600_000) / 60_000

        val shareText = buildString {
            appendLine("Acabo de completar un ayuno de ${durationH}h ${"%02d".format(durationMin)}m 🌙")
            appendLine()
            appendLine("Mira mi último ayuno. Si quieres mejorar tu salud únete tú también 👇")
            append(STORE_URL)
        }

        return Intent(Intent.ACTION_SEND).apply {
            type = "image/*"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, shareText)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    private fun generateShareImage(session: FastingSession): Bitmap {
        val size   = 1080
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint  = Paint(Paint.ANTI_ALIAS_FLAG)

        // ── Fondo ──────────────────────────────────────────────────────────
        canvas.drawColor(Color.parseColor("#2D4A3E"))

        // ── Barra superior ─────────────────────────────────────────────────
        paint.color = Color.parseColor("#8FBF9F")
        canvas.drawRect(0f, 0f, size.toFloat(), 12f, paint)

        // ── Nombre app ─────────────────────────────────────────────────────
        paint.apply {
            color     = Color.parseColor("#8FBF9F")
            textSize  = 52f
            typeface  = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("AYUNO", size / 2f, 110f, paint)

        // ── Línea divisoria ────────────────────────────────────────────────
        paint.apply {
            color       = Color.parseColor("#8FBF9F")
            alpha       = 80
            strokeWidth = 2f
        }
        canvas.drawLine(80f, 140f, (size - 80f), 140f, paint)
        paint.alpha = 255

        // ── Duración principal ─────────────────────────────────────────────
        val durationMs  = (session.endTime ?: System.currentTimeMillis()) - session.startTime
        val durationH   = durationMs / 3_600_000
        val durationMin = (durationMs % 3_600_000) / 60_000
        paint.apply {
            color    = Color.parseColor("#F5F0E8")
            textSize = 130f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("${durationH}h ${"%02d".format(durationMin)}m", size / 2f, 310f, paint)

        // ── Check + objetivo ───────────────────────────────────────────────
        paint.apply {
            color    = Color.parseColor("#8FBF9F")
            textSize = 42f
            typeface = Typeface.DEFAULT
        }
        canvas.drawText("✓  Objetivo de ${session.goalHours}h completado", size / 2f, 385f, paint)

        // ── Línea divisoria ────────────────────────────────────────────────
        paint.apply {
            color       = Color.parseColor("#8FBF9F")
            alpha       = 80
            strokeWidth = 2f
        }
        canvas.drawLine(80f, 430f, (size - 80f), 430f, paint)
        paint.alpha = 255

        // ── Fases completadas ──────────────────────────────────────────────
        paint.apply {
            color    = Color.parseColor("#8FBF9F")
            textSize = 36f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.LEFT
        }
        canvas.drawText("Fases completadas", 80f, 490f, paint)

        paint.apply {
            color    = Color.parseColor("#F5F0E8")
            textSize = 38f
            typeface = Typeface.DEFAULT
        }
        var yPhase = 555f
        session.completedPhases.take(4).forEach { phase ->
            canvas.drawText("✓   $phase", 80f, yPhase, paint)
            yPhase += 62f
        }

        // ── Frase motivacional de la última fase ───────────────────────────
        val lastPhase = FASTING_PHASES
            .lastOrNull { phase -> session.completedPhases.contains(phase.name) }
        if (lastPhase != null) {
            paint.apply {
                color    = Color.parseColor("#E8C96A")
                textSize = 34f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
                textAlign = Paint.Align.CENTER
                alpha    = 200
            }
            canvas.drawText("\"${lastPhase.motivation}\"", size / 2f, 880f, paint)
            paint.alpha = 255
        }

        // ── Fecha ──────────────────────────────────────────────────────────
        val dateStr = SimpleDateFormat("dd MMMM yyyy", Locale("es"))
            .format(Date(session.startTime))
        paint.apply {
            color    = Color.parseColor("#8FBF9F")
            textSize = 34f
            typeface = Typeface.DEFAULT
            textAlign = Paint.Align.CENTER
            alpha    = 180
        }
        canvas.drawText(dateStr, size / 2f, 960f, paint)
        paint.alpha = 255

        // ── Barra inferior ─────────────────────────────────────────────────
        paint.apply {
            color = Color.parseColor("#8FBF9F")
            alpha = 255
        }
        canvas.drawRect(0f, (size - 12f).toFloat(), size.toFloat(), size.toFloat(), paint)

        return bitmap
    }

    private fun saveBitmapToCache(context: Context, bitmap: Bitmap): File {
        val dir  = File(context.cacheDir, "share").also { it.mkdirs() }
        val file = File(dir, "ayuno_share.png")
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        return file
    }

    // Actualizar con la URL real cuando la app esté publicada en Play Store
    private const val STORE_URL =
        "https://play.google.com/store/apps/details?id=com.moldovan.ayuno&pcampaignid=web_share"
}