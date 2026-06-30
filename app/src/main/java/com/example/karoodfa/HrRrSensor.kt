package com.example.karoodfa

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.util.Log
import io.hammerhead.karooext.internal.Emitter
import io.hammerhead.karooext.models.DataPoint
import io.hammerhead.karooext.models.DataType
import io.hammerhead.karooext.models.DeviceEvent
import io.hammerhead.karooext.models.OnDataPoint
import java.util.UUID

class HrRrSensor(
    private val context: Context,
    private val uid: String,
    private val emitter: Emitter<DeviceEvent>,
    private val dfaCalculator: DfaCalculator
) {
    companion object {
        private const val TAG = "HrRrSensor"
        private val HR_SERVICE_UUID = UUID.fromString("0000180d-0000-1000-8000-00805f9b34fb")
        private val HR_CHAR_UUID = UUID.fromString("00002a37-0000-1000-8000-00805f9b34fb")
        private val CLIENT_CHARACTERISTIC_CONFIG = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
    }

    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter = bluetoothManager.adapter
    private var bluetoothGatt: BluetoothGatt? = null

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.d(TAG, "Connected to $uid")
                try {
                    gatt.discoverServices()
                } catch (e: SecurityException) {
                    Log.e(TAG, "SecurityException discovering services", e)
                }
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.d(TAG, "Disconnected from $uid")
                close()
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                val service = gatt.getService(HR_SERVICE_UUID)
                val characteristic = service?.getCharacteristic(HR_CHAR_UUID)
                if (characteristic != null) {
                    try {
                        gatt.setCharacteristicNotification(characteristic, true)
                        val descriptor = characteristic.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG)
                        if (descriptor != null) {
                            descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                            gatt.writeDescriptor(descriptor)
                        }
                    } catch (e: SecurityException) {
                        Log.e(TAG, "SecurityException setting notification", e)
                    }
                }
            }
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            if (characteristic.uuid == HR_CHAR_UUID) {
                parseHrCharacteristic(characteristic.value)
            }
        }
    }

    fun start() {
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) return
        try {
            val device = bluetoothAdapter.getRemoteDevice(uid)
            bluetoothGatt = device.connectGatt(context, true, gattCallback)
        } catch (e: SecurityException) {
            Log.e(TAG, "Missing permissions to connect to $uid", e)
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "Invalid MAC address: $uid", e)
        }
    }

    fun close() {
        try {
            bluetoothGatt?.disconnect()
            bluetoothGatt?.close()
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException closing GATT", e)
        }
        bluetoothGatt = null
    }

    private fun parseHrCharacteristic(value: ByteArray?) {
        if (value == null || value.isEmpty()) return
        
        val flags = value[0].toInt()
        val hr16Bit = (flags and 0x01) != 0
        val eePresent = (flags and 0x08) != 0
        val rrPresent = (flags and 0x10) != 0

        var offset = 1
        val hrValue = if (hr16Bit) {
            val hr = ((value[offset + 1].toInt() and 0xFF) shl 8) or (value[offset].toInt() and 0xFF)
            offset += 2
            hr
        } else {
            val hr = value[offset].toInt() and 0xFF
            offset += 1
            hr
        }

        val dataPoint = DataPoint(DataType.Type.HEART_RATE, mapOf(DataType.Type.HEART_RATE to hrValue.toDouble()), null)
        try {
            emitter.onNext(OnDataPoint(dataPoint))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to emit HR", e)
        }

        if (!rrPresent) return

        if (eePresent) offset += 2

        while (offset + 1 < value.size) {
            val rrValue = ((value[offset + 1].toInt() and 0xFF) shl 8) or (value[offset].toInt() and 0xFF)
            val rrMs = (rrValue.toDouble() / 1024.0) * 1000.0
            
            dfaCalculator.addRrInterval(rrMs)
            offset += 2
        }
    }
}
