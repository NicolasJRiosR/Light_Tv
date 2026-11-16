package com.example.rgbcontroller.hardware

import android.content.Context
import android.hardware.ConsumerIrManager

sealed interface IrStatus {
    data class Ready(val ranges: List<IntRange>) : IrStatus
    data object UnsupportedApi : IrStatus
    data object ServiceUnavailable : IrStatus
    data object NoEmitter : IrStatus
    data class Error(val message: String) : IrStatus
}

class IrTransmitter(context: Context) {
    private val manager = context.getSystemService(Context.CONSUMER_IR_SERVICE) as? ConsumerIrManager

    fun diagnose(): IrStatus {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.KITKAT) return IrStatus.UnsupportedApi
        val ir = manager ?: return IrStatus.ServiceUnavailable
        return try {
            if (!ir.hasIrEmitter()) return IrStatus.NoEmitter
            val ranges = ir.carrierFrequencies?.map { it.minFrequency..it.maxFrequency }.orEmpty()
            IrStatus.Ready(ranges)
        } catch (e: RuntimeException) {
            IrStatus.Error(e.message ?: "No se pudo consultar el servicio infrarrojo.")
        }
    }

    fun transmit(frequencyHz: Int, patternMicros: IntArray): Result<Unit> {
        val ir = manager ?: return Result.failure(IllegalStateException("Servicio IR no disponible."))
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.KITKAT) {
            return Result.failure(UnsupportedOperationException("ConsumerIrManager requiere Android 4.4 o posterior."))
        }
        if (!ir.hasIrEmitter()) return Result.failure(IllegalStateException("Este dispositivo no informa un emisor IR."))
        if (frequencyHz !in 10_000..500_000) return Result.failure(IllegalArgumentException("Frecuencia fuera del rango admitido por Android."))
        if (patternMicros.isEmpty() || patternMicros.any { it <= 0 }) {
            return Result.failure(IllegalArgumentException("El patrón debe contener duraciones positivas de marca/espacio."))
        }
        if (patternMicros.sumOf { it.toLong() } >= 2_000_000L) {
            return Result.failure(IllegalArgumentException("Android solo transmite patrones menores a 2 segundos."))
        }
        val supported = ir.carrierFrequencies
        if (supported != null && supported.none { frequencyHz in it.minFrequency..it.maxFrequency }) {
            return Result.failure(IllegalArgumentException("La frecuencia no está dentro de los rangos informados por el emisor."))
        }
        return try {
            ir.transmit(frequencyHz, patternMicros)
            Result.success(Unit)
        } catch (e: RuntimeException) {
            Result.failure(e)
        }
    }
}
