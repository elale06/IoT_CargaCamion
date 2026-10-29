package com.example.myapplication

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    AppNavigation()
                }
            }
        }
    }
}

// Funciones de Notificación
fun crearCanalNotificacion(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = NotificationChannel(
            "CANAL_SOBRECARGA",
            "Alertas de Sobrecarga",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Notifica cuando un camión excede el límite de carga"
        }
        val notificationManager: NotificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }
}

@SuppressLint("MissingPermission")
fun lanzarNotificacionAlerta(context: Context, peso: Float) {
    val builder = NotificationCompat.Builder(context, "CANAL_SOBRECARGA")
        .setSmallIcon(android.R.drawable.ic_dialog_alert)
        .setContentTitle("¡PELIGRO: SOBRECARGA DETECTADA!")
        .setContentText("El camión registró ${"%.1f".format(peso)} kg.")
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setDefaults(NotificationCompat.DEFAULT_ALL)

    with(NotificationManagerCompat.from(context)) {
        notify(101, builder.build())
    }
}

// Modelo de Datos
data class RegistroCarga(
    val pesoRegistrado: Float,
    val esSobrecarga: Boolean,
    val fechaHora: Long = System.currentTimeMillis()
)

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val historialRegistros = remember { mutableStateListOf<RegistroCarga>() }

    NavHost(navController = navController, startDestination = "login") {
        composable("login") { LoginScreen(navController) }
        composable("monitor") { MonitorScreen(navController, historialRegistros) }
        composable("historial") { HistorialScreen(historialRegistros) }
    }
}

@Composable
fun LoginScreen(navController: NavController) {
    val context = LocalContext.current
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Acceso al Sistema IoT", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Usuario") },
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Contraseña") },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(onClick = {
            if (username.isNotBlank() && password.isNotBlank()) {
                navController.navigate("monitor") {
                    popUpTo("login") { inclusive = true }
                }
            } else {
                Toast.makeText(context, "Por favor, ingrese sus credenciales", Toast.LENGTH_SHORT).show()
            }
        }) {
            Text("Ingresar")
        }
    }
}

@Composable
fun MonitorScreen(navController: NavController, historialRegistros: MutableList<RegistroCarga>) {
    val context = LocalContext.current
    var pesoActual by remember { mutableFloatStateOf(0f) }
    var inputPeso by remember { mutableStateOf("") }

    val limite = 20.0f
    val esSobrecarga = pesoActual > limite
    val colorEstado = if (esSobrecarga) Color.Red else Color.Green
    val mensajeEstado = if (esSobrecarga) "¡ALERTA: Límite superado! ($limite kg)" else "Carga Permitida"

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    LaunchedEffect(Unit) {
        crearCanalNotificacion(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Monitor IoT - Modo Simulación Manual", fontSize = 14.sp, color = Color.Gray)
        Spacer(modifier = Modifier.height(32.dp))

        Box(
            modifier = Modifier
                .size(150.dp)
                .background(colorEstado, shape = MaterialTheme.shapes.medium),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "${"%.1f".format(pesoActual)} kg",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text(mensajeEstado, color = colorEstado, fontSize = 20.sp, fontWeight = FontWeight.Bold)

        if (esSobrecarga) {
            LaunchedEffect(pesoActual) {
                Toast.makeText(context, "Alerta Visual Activada", Toast.LENGTH_SHORT).show()
            }
        }

        Spacer(modifier = Modifier.height(48.dp))

        OutlinedTextField(
            value = inputPeso,
            onValueChange = { inputPeso = it },
            label = { Text("Ingresar peso simulado (kg)") },
            modifier = Modifier.fillMaxWidth(0.8f),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Button(onClick = {
                val pesoSimulado = inputPeso.toFloatOrNull()

                if (pesoSimulado != null) {
                    pesoActual = pesoSimulado
                    val sobrecargaDetectada = pesoActual > limite

                    historialRegistros.add(RegistroCarga(pesoActual, sobrecargaDetectada))
                    inputPeso = ""

                    if (sobrecargaDetectada) {
                        lanzarNotificacionAlerta(context, pesoActual)
                    }
                } else {
                    Toast.makeText(context, "Ingrese un número válido", Toast.LENGTH_SHORT).show()
                }
            }) {
                Text("Probar Carga")
            }

            Button(onClick = { navController.navigate("historial") }) {
                Text("Ver Historial")
            }
        }
    }
}

@Composable
fun HistorialScreen(registros: List<RegistroCarga>) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            text = "Historial de Cargas",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (registros.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No hay registros almacenados.", fontSize = 16.sp, color = Color.Gray)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(registros.reversed()) { registro ->
                    TarjetaRegistro(registro)
                }
            }
        }
    }
}

@Composable
fun TarjetaRegistro(registro: RegistroCarga) {
    val colorEstado = if (registro.esSobrecarga) Color(0xFFFFCDD2) else Color(0xFFC8E6C9)
    val colorTexto = if (registro.esSobrecarga) Color(0xFFB71C1C) else Color(0xFF1B5E20)
    val icono = if (registro.esSobrecarga) Icons.Default.Warning else Icons.Default.CheckCircle

    val formatoFecha = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
    val fechaFormateada = formatoFecha.format(Date(registro.fechaHora))

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = colorEstado)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("${registro.pesoRegistrado} kg", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = colorTexto)
                Spacer(modifier = Modifier.height(4.dp))
                Text(fechaFormateada, fontSize = 14.sp, color = Color.DarkGray)
            }
            Icon(imageVector = icono, contentDescription = "Estado", tint = colorTexto, modifier = Modifier.size(32.dp))
        }
    }
}