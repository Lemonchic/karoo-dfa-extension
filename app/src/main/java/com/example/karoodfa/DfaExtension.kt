package com.example.karoodfa

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.ParcelUuid
import android.util.Log
import io.hammerhead.karooext.KarooSystemService
import io.hammerhead.karooext.extension.KarooExtension
import io.hammerhead.karooext.extension.DataTypeImpl
import io.hammerhead.karooext.internal.Emitter
import io.hammerhead.karooext.models.DataType
import io.hammerhead.karooext.models.Device
import io.hammerhead.karooext.models.DeviceEvent
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class DfaExtension : KarooExtension("karoo-dfa-a1", "1.0") {
    companion object {
        private const val TAG = "DfaExtension"
        private val HR_SERVICE_UUID = UUID.fromString("0000180d-0000-1000-8000-00805f9b34fb")
    }

    private lateinit var karooSystem: KarooSystemService
    private val dfaCalculator = DfaCalculator()
    private lateinit var dfaDataType: DfaDataType
    private val activeSensors = ConcurrentHashMap<String, HrRrSensor>()

    private val bluetoothManager by lazy { getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager }
    private val bluetoothAdapter by lazy { bluetoothManager.adapter }

    override fun onCreate() {
        super.onCreate()
        karooSystem = KarooSystemService(this)
        dfaDataType = DfaDataType(this, dfaCalculator)
    }

    override fun onDestroy() {
        activeSensors.values.forEach { it.close() }
        activeSensors.clear()
        karooSystem.disconnect()
        super.onDestroy()
    }

    override val types: List<DataTypeImpl>
        get() = listOf(dfaDataType)

    override fun startScan(emitter: Emitter<Device>) {
        val scanner = bluetoothAdapter?.bluetoothLeScanner ?: return
        
        val filter = ScanFilter.Builder()
            .setServiceUuid(ParcelUuid(HR_SERVICE_UUID))
            .build()
        
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()
            
        val scanCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult?) {
                result?.device?.let { device ->
                    Log.d(TAG, "Found device during scan: ${device.address} ${device.name}")
                    val deviceName = device.name ?: "Unknown HR"
                    try {
                        emitter.onNext(
                            Device(
                                extension = extension,
                                uid = device.address,
                                dataTypes = listOf(DataType.Type.HEART_RATE),
                                displayName = deviceName
                            )
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to emit device", e)
                    }
                }
            }
        }
        
        try {
            scanner.startScan(listOf(filter), settings, scanCallback)
            emitter.setCancellable {
                try {
                    scanner.stopScan(scanCallback)
                } catch (e: SecurityException) {
                    Log.e(TAG, "SecurityException stopping scan", e)
                }
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException starting scan", e)
            emitter.onError(e)
        }
    }

    override fun connectDevice(uid: String, emitter: Emitter<DeviceEvent>) {
        Log.d(TAG, "Connecting to device: $uid")
        val sensor = HrRrSensor(this, uid, emitter, dfaCalculator)
        activeSensors[uid] = sensor
        
        emitter.setCancellable {
            Log.d(TAG, "Disconnecting from device: $uid")
            activeSensors.remove(uid)?.close()
        }
        
        sensor.start()
    }
}
