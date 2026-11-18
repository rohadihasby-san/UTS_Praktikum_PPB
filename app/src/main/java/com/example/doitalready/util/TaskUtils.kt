package com.example.doitalready.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.roundToLong

/** Kunci Intent antar layar. */
object TaskIntent {
    /** Kunci extra untuk id tugas. */
    const val EXTRA_TASK_ID = "TASK_ID"

    /** Dikirim saat membuka form untuk tugas baru (bukan edit). */
    const val NEW_TASK_ID = -1L
}

/**
 * Helper tanggal. Semua tanggal di database disimpan sebagai epoch milidetik,
 * dan dibandingkan per hari berdasarkan zona waktu perangkat.
 */
object DateUtils {
    private const val DAY_IN_MILLIS = 24L * 60L * 60L * 1000L
    private val localeId: Locale = Locale.forLanguageTag("id-ID")

    /** Jam 00:00 pada hari yang sama dengan [millis]. */
    fun startOfDay(millis: Long): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = millis
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    /** Geser [dayStartMillis] sebanyak [days] hari (boleh negatif). */
    fun addDays(dayStartMillis: Long, days: Int): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = dayStartMillis
        calendar.add(Calendar.DAY_OF_YEAR, days)
        return calendar.timeInMillis
    }

    /** Selisih hari kalender dari [fromMillis] ke [toMillis]. Negatif kalau sudah lewat. */
    fun daysBetween(fromMillis: Long, toMillis: Long): Long {
        val diff = startOfDay(toMillis) - startOfDay(fromMillis)
        return (diff.toDouble() / DAY_IN_MILLIS).roundToLong()
    }

    /** Contoh: "9 Nov 2026". */
    fun formatDate(millis: Long): String =
        SimpleDateFormat("d MMM yyyy", localeId).format(Date(millis))

    /** Contoh: "Sen", "Sel", "Rab". */
    fun shortDayName(millis: Long): String =
        SimpleDateFormat("EEE", localeId).format(Date(millis))

    /**
     * DatePicker Material 3 memakai milidetik UTC pada tengah malam.
     * Fungsi ini mengubahnya jadi jam 00:00 di zona waktu perangkat,
     * dengan tanggal kalender yang sama.
     */
    fun utcPickerToLocalStartOfDay(utcMillis: Long): Long {
        val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        utc.timeInMillis = utcMillis
        val local = Calendar.getInstance()
        local.clear()
        local.set(
            utc.get(Calendar.YEAR),
            utc.get(Calendar.MONTH),
            utc.get(Calendar.DAY_OF_MONTH),
            0, 0, 0
        )
        return local.timeInMillis
    }

    /** Kebalikan dari [utcPickerToLocalStartOfDay], untuk nilai awal DatePicker. */
    fun localToUtcPicker(localMillis: Long): Long {
        val local = Calendar.getInstance()
        local.timeInMillis = localMillis
        val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        utc.clear()
        utc.set(
            local.get(Calendar.YEAR),
            local.get(Calendar.MONTH),
            local.get(Calendar.DAY_OF_MONTH),
            0, 0, 0
        )
        return utc.timeInMillis
    }
}