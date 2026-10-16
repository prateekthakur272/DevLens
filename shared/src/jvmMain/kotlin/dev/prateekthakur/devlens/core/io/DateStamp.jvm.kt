package dev.prateekthakur.devlens.core.io

actual fun todayStamp(): String = java.time.LocalDate.now().toString()
