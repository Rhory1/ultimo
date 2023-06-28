package com.lala.gabinete1

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle


import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import android.widget.*
import android.widget.Spinner
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.core.app.ActivityCompat
import java.io.IOException
import java.io.InputStream
import android.os.Handler
import android.os.Message
import androidx.core.app.NotificationCompat
import java.util.*
import android.annotation.SuppressLint as SuppressLint1

const val REQUEST_ENABLE_BT = 1


@Suppress("DEPRECATION")
class MainActivity : AppCompatActivity() {
    lateinit var mBtAdapter: BluetoothAdapter
    var mAddressDevice: ArrayAdapter<String>? = null
    var mNameDevice: ArrayAdapter<String>? = null
    var inputStream: InputStream? = null

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
        val MESSAGE_READ = 1

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
                if (m_bluetoothSocket == null || !m_isConnected) {
                    val IntValSpin = spnDispositivos.selectedItemPosition
                    m_address = mAddressDevice!!.getItem(IntValSpin).toString()
                    Toast.makeText(this, m_address, Toast.LENGTH_LONG).show()
                    mBtAdapter?.cancelDiscovery()
                    val device: BluetoothDevice = mBtAdapter.getRemoteDevice(m_address)
                    m_bluetoothSocket = device.createInsecureRfcommSocketToServiceRecord(m_myUUID)
                    m_bluetoothSocket!!.connect()
                    inputStream = m_bluetoothSocket?.inputStream
                    Toast.makeText(this, "CONEXIION EXITOSA", Toast.LENGTH_LONG).show()
                    Log.i("MainActivity", "CONEXION EXITOSA")
                    inputStream = m_bluetoothSocket?.inputStream
                    val handler = Handler(Handler.Callback { msg ->
                        if (msg.what == MESSAGE_READ) {
                            val readMessage = msg.obj.toString()
                            txtEstado.text = readMessage
                            enviarNotificacion (readMessage)
                        }
                        true
                    })
                    val thread = Thread(Runnable {
                        while (true) {
                            try {
                                val bytesAvailable = inputStream?.available()
                                if (bytesAvailable != null && bytesAvailable > 0) {
                                    val buffer = ByteArray(bytesAvailable)
                                    inputStream?.read(buffer)
                                    handler.obtainMessage(MESSAGE_READ, bytesAvailable, -1, buffer).sendToTarget()
                                }
                            } catch (e: IOException) {
                                break
                            }
                        }
                    })
                    thread.start()
                }
            }catch (e: IOException){
                e.printStackTrace()
                Toast.makeText(this, "ERROR DE CONEXION", Toast.LENGTH_LONG).show()
                Log.i("MainActivity", "ERROR DE CONEXION")
            }
        }
    }
    fun enviarNotificacion(mensaje: String) {
        val channelId = "mi_canal" // Identificador único del canal de notificación
        val channelName = "Mi Canal" // Nombre legible del canal de notificación

        // Crear un intent para abrir la actividad principal cuando se hace clic en la notificación
        val intent = Intent(applicationContext, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        // Crear una notificación
        val builder = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(R.drawable.ic_notification) // Icono pequeño de la notificación
            .setContentTitle("Mensaje recibido") // Título de la notificación
            .setContentText(mensaje) // Contenido de la notificación (mensaje recibido)
            .setContentIntent(pendingIntent) // Intent a abrir cuando se hace clic en la notificación
            .setAutoCancel(true) // Cerrar automáticamente la notificación al hacer clic en ella

        // Obtener el servicio de notificación
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Comprobar si el dispositivo está ejecutando Android Oreo (API 26) o superior
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Crear el canal de notificación para Android Oreo y versiones superiores
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(channelId, channelName, importance)
            notificationManager.createNotificationChannel(channel)
        }

        // Mostrar la notificación
        notificationManager.notify(1, builder.build())
    }
}

