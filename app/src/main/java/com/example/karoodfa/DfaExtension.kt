package com.example.karoodfa

import io.hammerhead.karooext.KarooSystemService
import io.hammerhead.karooext.extension.KarooExtension
import io.hammerhead.karooext.extension.DataTypeImpl

class DfaExtension : KarooExtension("karoo-dfa-a1", "1.0") {
    private lateinit var karooSystem: KarooSystemService
    private val dfaCalculator = DfaCalculator()
    private lateinit var dfaDataType: DfaDataType

    override fun onCreate() {
        super.onCreate()
        karooSystem = KarooSystemService(this)
        dfaDataType = DfaDataType(this, dfaCalculator)
    }

    override fun onDestroy() {
        karooSystem.disconnect()
        super.onDestroy()
    }

    override val types: List<DataTypeImpl>
        get() = listOf(dfaDataType)
}
