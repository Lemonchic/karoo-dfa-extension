package com.example.karoodfa

import android.content.Context
import io.hammerhead.karooext.extension.DataTypeImpl
import io.hammerhead.karooext.internal.Emitter
import io.hammerhead.karooext.internal.ViewEmitter
import io.hammerhead.karooext.models.DataPoint
import io.hammerhead.karooext.models.DataType
import io.hammerhead.karooext.models.StreamState
import io.hammerhead.karooext.models.ViewConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class DfaDataType(
    private val extensionContext: Context,
    private val dfaCalculator: DfaCalculator
) : DataTypeImpl("karoo-dfa-a1", "Alpha1") {

    override fun startStream(emitter: Emitter<StreamState>) {
        val job = CoroutineScope(Dispatchers.Default).launch {
            while (isActive) {
                val value = dfaCalculator.compute()
                val dataPoint = DataPoint(dataTypeId, mapOf(DataType.Field.SINGLE to value), "karoo-dfa-source")
                emitter.onNext(StreamState.Streaming(dataPoint))
                delay(1000)
            }
        }
        emitter.setCancellable { job.cancel() }
    }
}
