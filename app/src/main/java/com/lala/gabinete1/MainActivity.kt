package com.lala.gabinete1

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import android.widget.*
import android.widget.Spinner
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.core.app.ActivityCompat
import java.io.IOException
import java.util.*
import android.annotation.SuppressLint as SuppressLint1

const val REQUEST_ENABLE_BT = 1

class MainActivity : AppCompatActivity() {
    lateinit var mBtAdapter: BluetoothAdapter
    var mAddressDevice: ArrayAdapter<String>? = null
    var mNameDevice: ArrayAdapter<String>? = null

    companion object{
        var m_myUUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
        private var m_bluetoothSocket: BluetoothSocket? = null

        var m_isConnected: Boolean = false
        lateinit var m_address: String
    }
    @SuppressLint1("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mAddressDevice = ArrayAdapter(this, android.R.layout.simple_list_item_1)
        mNameDevice = ArrayAdapter(this, android.R.layout.simple_list_item_1)

        var btnActivar = findViewById<Button>(R.id.btnActivar)
        var btnDesactivar = findViewById<Button>(R.id.btnDesactivar)
        var btnBuscar = findViewById<Button>(R.id.btnBuscar)
        var btnConectar = findViewById<Button>(R.id.btnConectar)
        var spnDispositivos = findViewById<Spinner>(R.id.spnDispositivos)
        var txtEstado = findViewById<TextView>(R.id.txtEstado)


        val someActivityResultLauncher = registerForActivityResult(
            StartActivityForResult()
        ) { result ->
            if (result.resultCode == REQUEST_ENABLE_BT) {
                Log.i("MainActivity", "ACTIVIDAD REGISTRADA")
            }
        }
        mBtAdapter = (getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter

        if(mBtAdapter == null){
            Toast.makeText(this, "Bluetooth no esta disponible en este dispositivo", Toast.LENGTH_LONG).show()
        }else{
            Toast.makeText(this, "Bluetooth esta disponible en este dispositivo", Toast.LENGTH_LONG).show()
        }
        btnActivar.setOnClickListener {

            if (mBtAdapter.isEnabled){
                Toast.makeText(this,"Bluetooth ya esta activado", Toast.LENGTH_LONG).show()
            }else{
                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                if (ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
                ){
                    Log.i("MainActivity", "ActivityCompat#requestPermissions")
                }
                someActivityResultLauncher.launch(enableBtIntent)
            }
        }
        btnDesactivar.setOnClickListener {
            if (!mBtAdapter.isEnabled){
                Toast.makeText(this,"Bluetooth ya esta desactivado", Toast.LENGTH_LONG).show()
            }else{
                mBtAdapter.disable()
                Toast.makeText(this, "Se ha desactivado el bluetooth", Toast.LENGTH_LONG).show()
            }
        }
        btnBuscar.setOnClickListener {
            if (mBtAdapter.isEnabled){
                val pairedDevice: Set<BluetoothDevice>? = mBtAdapter?.bondedDevices
                mAddressDevice!!.clear()
                mNameDevice!!.clear()

                if (pairedDevice != null) {
                    pairedDevice.forEach { device ->
                        val deviceName = device.name
                        val deviceHardwareAddress = device.address
                        mAddressDevice!!.add(deviceHardwareAddress)
                        mNameDevice!!.add(deviceName)
                    }
                }
                spnDispositivos.setAdapter(mNameDevice)
            }else{
                val noDevices = "Ningun dispositivo pudo ser emparejado"
                mAddressDevice!!.add(noDevices)
                mNameDevice!!.add(noDevices)
                Toast.makeText(this, "Primero vincule un dispositivo bluetooth", Toast.LENGTH_LONG).show()
            }
        }
        btnConectar.setOnClickListener {

            try{
                if (m_bluetoothSocket == null || !m_isConnected){
                    val IntValSpin = spnDispositivos.selectedItemPosition
                    m_address = mAddressDevice!!.getItem(IntValSpin).toString()
                    Toast.makeText(this, m_address, Toast.LENGTH_LONG).show()
                    mBtAdapter?.cancelDiscovery()
                    val device: BluetoothDevice = mBtAdapter.getRemoteDevice(m_address)
                    m_bluetoothSocket = device.createInsecureRfcommSocketToServiceRecord(m_myUUID)
                    m_bluetoothSocket!!.connect()
                    val Entrada : String = txtEstado.text.toString()
                    sendCommand(Entrada)

                }

                Toast.makeText(this, "CONEXIION EXITOSA", Toast.LENGTH_LONG).show()
                Log.i("MainActivity", "CONEXION EXITOSA")


            }catch (e: IOException){
                e.printStackTrace()
                Toast.makeText(this, "ERROR DE CONEXION", Toast.LENGTH_LONG).show()
                Log.i("MainActivity", "ERROR DE CONEXION")
            }
        }
    }
    private fun sendCommand (imput: String){
        if (m_bluetoothSocket!= null){
            try {
                m_bluetoothSocket!!.inputStream.read(imput.toByteArray())
            }catch (e: IOException){
                e.printStackTrace()
            }
        }
    }
}