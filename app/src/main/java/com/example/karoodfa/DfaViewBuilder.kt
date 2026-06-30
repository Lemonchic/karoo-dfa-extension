package com.example.karoodfa

import android.content.Context
import android.widget.RemoteViews
import io.hammerhead.karooext.internal.ViewEmitter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class DfaViewBuilder {
    fun buildView(context: Context, emitter: ViewEmitter, dfaCalculator: DfaCalculator) {
        val views = RemoteViews(context.packageName, R.layout.dfa_data_field)
        
        val job = CoroutineScope(Dispatchers.Main).launch {
            while (isActive) {
                val value = dfaCalculator.compute()
                views.setTextViewText(R.id.dfa_value_text, String.format("%.2f", value))
                emitter.updateView(views)
                
                delay(2000)
            }
        }
        
        emitter.setCancellable {
            job.cancel()
        }
    }
}
