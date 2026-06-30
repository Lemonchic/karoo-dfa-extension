package com.example.karoodfa

import android.content.Context
import android.content.res.Configuration
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
                
                val nightModeFlags = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
                val isDarkMode = nightModeFlags == Configuration.UI_MODE_NIGHT_YES
                
                val bgColor = if (isDarkMode) {
                    when {
                        value < 0.01 -> 0x00000000.toInt() // Transparent (black)
                        value < 0.5 -> 0xFF8F1D1D.toInt()  // Dark Red
                        value < 0.75 -> 0xFFA0522D.toInt() // Dark Orange
                        else -> 0xFF1B5E20.toInt()         // Dark Green
                    }
                } else {
                    when {
                        value < 0.01 -> 0x00000000.toInt() // Transparent (white)
                        value < 0.5 -> 0xFFFF5252.toInt()  // Vibrant Red (matches HR style)
                        value < 0.75 -> 0xFFFFA726.toInt() // Vibrant Orange
                        else -> 0xFF00B074.toInt()         // Vibrant Green
                    }
                }
                
                val textColor = if (isDarkMode && value >= 0.01) {
                    0xFFFFFFFF.toInt() // White text on dark backgrounds
                } else {
                    0xFF000000.toInt() // Black text on light/pastel backgrounds
                }
                
                views.setTextViewText(R.id.dfa_value_text, String.format("%.2f", value))
                views.setInt(R.id.dfa_background, "setBackgroundColor", bgColor)
                views.setTextColor(R.id.dfa_value_text, textColor)
                
                emitter.updateView(views)
                
                delay(2000)
            }
        }
        
        emitter.setCancellable {
            job.cancel()
        }
    }
}
