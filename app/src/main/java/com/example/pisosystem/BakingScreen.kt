@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.pisosystem

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

fun formatTime(seconds: Int): String {

    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val secs = seconds % 60

    return String.format(
        "%02d:%02d:%02d",
        hours,
        minutes,
        secs
    )
}

fun showNotification(
    context: Context,
    title: String,
    message: String
) {

    val channelId = "piso_wifi_channel"

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

        val channel = NotificationChannel(
            channelId,
            "PISO WIFI",
            NotificationManager.IMPORTANCE_HIGH
        )

        channel.enableVibration(true)

        val manager =
            context.getSystemService(
                NotificationManager::class.java
            )

        manager.createNotificationChannel(channel)
    }

    val builder =
        NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)

    if (
        ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) != PackageManager.PERMISSION_GRANTED
    ) {
        return
    }

    NotificationManagerCompat
        .from(context)
        .notify(
            System.currentTimeMillis().toInt(),
            builder.build()
        )
}

@Composable
fun BakingScreen() {

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val notificationPermissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { }

    val code = remember {
        mutableStateOf("")
    }

    val status = remember {
        mutableStateOf("")
    }

    val loading = remember {
        mutableStateOf(false)
    }

    val timeRemaining = remember {
        mutableIntStateOf(0)
    }

    val sessionRunning = remember {
        mutableStateOf(false)
    }

    val notified10Min = remember {
        mutableStateOf(false)
    }

    val notified5Min = remember {
        mutableStateOf(false)
    }

    val notified1Min = remember {
        mutableStateOf(false)
    }

    Scaffold(

        topBar = {

            TopAppBar(

                title = {

                    Text(
                        text = "PISO SYSTEM",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },

                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF5E35B1)
                )
            )
        }

    ) { padding ->

        Column(

            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFF3E5F5),
                            Color.White
                        )
                    )
                )
                .padding(padding)
                .padding(20.dp),

            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            Text(
                text = "Welcome",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF5E35B1)
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Enter Piso WiFi Voucher",
                fontSize = 16.sp,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(30.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = 10.dp
                )
            ) {

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {

                    OutlinedTextField(

                        enabled = !sessionRunning.value,

                        value = code.value,

                        onValueChange = {
                            code.value = it
                        },

                        label = {
                            Text("Voucher Code")
                        },

                        modifier = Modifier.fillMaxWidth(),

                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(

                        enabled =
                            !sessionRunning.value &&
                                    !loading.value,

                        onClick = {

                            if (sessionRunning.value) {

                                Toast.makeText(
                                    context,
                                    "⚠️ Session already active",
                                    Toast.LENGTH_SHORT
                                ).show()

                                return@Button
                            }

                            if (code.value.isEmpty()) {

                                Toast.makeText(
                                    context,
                                    "Please enter voucher",
                                    Toast.LENGTH_SHORT
                                ).show()

                                return@Button
                            }

                            if (
                                Build.VERSION.SDK_INT >=
                                Build.VERSION_CODES.TIRAMISU
                            ) {

                                notificationPermissionLauncher.launch(
                                    Manifest.permission.POST_NOTIFICATIONS
                                )
                            }

                            loading.value = true

                            status.value = ""
                            timeRemaining.intValue = 0

                            notified10Min.value = false
                            notified5Min.value = false
                            notified1Min.value = false

                            scope.launch {

                                try {

                                    val response =
                                        RetrofitInstance.api.connectVoucher(
                                            VoucherRequest(code.value)
                                        )

                                    loading.value = false

                                    Log.d(
                                        "API_RESPONSE",
                                        response.body().toString()
                                    )

                                    if (
                                        response.isSuccessful &&
                                        response.body() != null
                                    ) {

                                        val data = response.body()!!

                                        if (data.success == true) {

                                            status.value = "✅ Connected"

                                            sessionRunning.value = true

                                            timeRemaining.intValue =
                                                (data.minutes ?: 0) * 60

                                            showNotification(
                                                context,
                                                "PISO SYSTEM",
                                                "✅ Connected Successfully"
                                            )

                                        } else {

                                            status.value =
                                                data.message
                                                    ?: "Invalid Voucher"
                                        }

                                    } else {

                                        status.value =
                                            "Server Error"
                                    }

                                } catch (e: Exception) {

                                    loading.value = false

                                    Log.e(
                                        "REAL_ERROR",
                                        e.stackTraceToString()
                                    )

                                    status.value =
                                        e.localizedMessage
                                            ?: "Unknown Error"

                                    return@launch
                                }

                                while (
                                    sessionRunning.value &&
                                    timeRemaining.intValue > 0
                                ) {

                                    delay(1000)

                                    timeRemaining.intValue--

                                    // 10 MINUTES
                                    if (
                                        timeRemaining.intValue == 600 &&
                                        !notified10Min.value
                                    ) {

                                        notified10Min.value = true

                                        showNotification(
                                            context,
                                            "PISO SYSTEM",
                                            "⚠️ 10 Minutes remaining"
                                        )
                                    }

                                    // 5 MINUTES
                                    if (
                                        timeRemaining.intValue == 300 &&
                                        !notified5Min.value
                                    ) {

                                        notified5Min.value = true

                                        showNotification(
                                            context,
                                            "PISO SYSTEM",
                                            "⚠️ 5 Minutes remaining"
                                        )
                                    }

                                    // 1 MINUTE
                                    if (
                                        timeRemaining.intValue == 60 &&
                                        !notified1Min.value
                                    ) {

                                        notified1Min.value = true

                                        showNotification(
                                            context,
                                            "PISO SYSTEM",
                                            "⚠️ 1 Minute remaining"
                                        )
                                    }
                                }

                                if (sessionRunning.value) {

                                    status.value =
                                        "⌛ Session Expired"

                                    showNotification(
                                        context,
                                        "PISO SYSTEM",
                                        "❌ Internet Session Expired"
                                    )

                                    sessionRunning.value = false

                                    code.value = ""

                                    timeRemaining.intValue = 0
                                }
                            }
                        },

                        modifier = Modifier
                            .fillMaxWidth()
                            .height(55.dp),

                        shape = RoundedCornerShape(14.dp),

                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF5E35B1)
                        )

                    ) {

                        Text(
                            text = "CONNECT",
                            fontSize = 18.sp,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    if (loading.value) {

                        CircularProgressIndicator()

                    } else {

                        Text(
                            text = status.value,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )

                        if (timeRemaining.intValue > 0) {

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = "⏳ Time Remaining: ${
                                    formatTime(
                                        timeRemaining.intValue
                                    )
                                }",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF5E35B1)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(30.dp))

            Text(
                text = "Powered by PISO SYSTEM",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }
    }
}