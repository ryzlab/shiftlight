package se.ryz.shiftlight

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import se.ryz.shiftlight.ui.theme.ShiftlightTheme
import android.content.Context
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import com.hoho.android.usbserial.driver.UsbSerialProber
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.TextStyle
import kotlinx.coroutines.delay
import org.json.JSONObject
import android.util.Log
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material3.TextFieldDefaults

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            val ringValues = remember { mutableStateListOf("", "", "", "") }
            var usbDevices by remember { mutableStateOf(listOf<String>()) }
            val offsetValue = remember { mutableStateOf("") }
            var sliderValue by remember { mutableStateOf(0f) }
            var isDisabled by remember { mutableStateOf(false) }
            var statusText by remember { mutableStateOf("") }
            var statusColor by remember { mutableStateOf(Color.White) }

            fun disableAll() {
                isDisabled = true
            }
            fun enableAll() {
                isDisabled = false
            }

            // Build JSON string from current UI state without arguments
            fun buildValuesJson(): String {
                val obj = JSONObject()
                obj.put("ring 1", ringValues.getOrNull(0)?.toIntOrZero() ?: 0)
                obj.put("ring 2", ringValues.getOrNull(1)?.toIntOrZero() ?: 0)
                obj.put("ring 3", ringValues.getOrNull(2)?.toIntOrZero() ?: 0)
                obj.put("ring 4", ringValues.getOrNull(3)?.toIntOrZero() ?: 0)
                obj.put("offset", offsetValue.value.toIntOrZero())
                return obj.toString()
            }

            // Called whenever any edit text changes; logs current values JSON
            fun onAnyFieldChanged() {
                val json = buildValuesJson()
                Log.d("Shiftlight", "Values JSON: $json")
            }

            // Parse JSON string and update edit boxes (expects keys: ring 1..4, offset)
            fun applyValuesJson(json: String) {
                try {
                    val obj = JSONObject(json)
                    val r1 = obj.optInt("ring 1", ringValues.getOrNull(0)?.toIntOrZero() ?: 0).coerceIn(0, 20000)
                    val r2 = obj.optInt("ring 2", ringValues.getOrNull(1)?.toIntOrZero() ?: 0).coerceIn(0, 20000)
                    val r3 = obj.optInt("ring 3", ringValues.getOrNull(2)?.toIntOrZero() ?: 0).coerceIn(0, 20000)
                    val r4 = obj.optInt("ring 4", ringValues.getOrNull(3)?.toIntOrZero() ?: 0).coerceIn(0, 20000)
                    val off = obj.optInt("offset", offsetValue.value.toIntOrZero()).coerceIn(0, 20000)

                    if (ringValues.size >= 4) {
                        ringValues[0] = r1.toString()
                        ringValues[1] = r2.toString()
                        ringValues[2] = r3.toString()
                        ringValues[3] = r4.toString()
                    }
                    offsetValue.value = off.toString()
                } catch (_: Exception) {
                    // ignore malformed JSON
                }
            }

            // Build JSON for slider RPM value without arguments
            fun buildRpmJson(): String {
                val obj = JSONObject()
                obj.put("rpm", sliderValue.toInt())
                return obj.toString()
            }

            // Status message helpers
            fun setErrorMessage(message: String) {
                statusText = message
                statusColor = Color(0xFFFF5555)
            }
            fun setStatusMessage(message: String) {
                statusText = message
                statusColor = Color.White
            }

            LaunchedEffect(Unit) {
                while (true) {
                    delay(2000)
                    enableAll()
                    val json = buildValuesJson()
                    Log.d("Shiftlight", "Values JSON: $json")
                }
            }
            ShiftlightTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = Color(0xFF181A1B) // dark metallic background
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Shiftlight 3000",
                                    color = Color(0xFFFFC300),
                                    style = MaterialTheme.typography.titleLarge,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 12.dp)
                                )
                                for (i in 1..4) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "ring $i",
                                            color = Color(0xFFFFC300), // racing yellow
                                            style = MaterialTheme.typography.bodyLarge,
                                            modifier = Modifier.padding(end = 12.dp)
                                        )
                                        OutlinedTextField(
                                            value = ringValues[i-1],
                                            onValueChange = { newValue ->
                                                val digitsOnly = newValue.filter { it.isDigit() }
                                                if (digitsOnly.isEmpty()) {
                                                    ringValues[i-1] = ""
                                                } else {
                                                    val clamped = digitsOnly.toIntOrNull()?.coerceIn(0, 20000) ?: 0
                                                    ringValues[i-1] = clamped.toString()
                                                }
                                                onAnyFieldChanged()
                                            },
                                            singleLine = true,
                                            enabled = !isDisabled,
                                            modifier = Modifier.fillMaxWidth(),
                                            trailingIcon = { Text("rpm", color = if (isDisabled) Color(0xFF9E9E9E) else Color(0xFFB0B0B0)) },
                                            textStyle = TextStyle(color = if (isDisabled) Color(0xFFBDBDBD) else Color.White),
                                            colors = TextFieldDefaults.colors(
                                                focusedContainerColor = Color(0xFF23272A),
                                                unfocusedContainerColor = Color(0xFF23272A),
                                                disabledContainerColor = Color(0xFF1F2123),
                                                focusedIndicatorColor = Color(0xFFFFC300),
                                                unfocusedIndicatorColor = Color(0xFFB0B0B0),
                                                disabledIndicatorColor = Color(0xFF5A5A5A),
                                                disabledTextColor = Color(0xFFBDBDBD),
                                                cursorColor = Color(0xFFFFC300)
                                            )
                                        )
                                    }
                                }
                                // Offset row
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "offset",
                                        color = if (isDisabled) Color(0xFF777777) else Color(0xFFFFC300),
                                        style = MaterialTheme.typography.bodyLarge,
                                        modifier = Modifier.padding(end = 12.dp)
                                    )
                                    OutlinedTextField(
                                        value = offsetValue.value,
                                        onValueChange = {
                                            val digitsOnly = it.filter { ch -> ch.isDigit() }
                                            if (digitsOnly.isEmpty()) {
                                                offsetValue.value = ""
                                            } else {
                                                val clamped = digitsOnly.toIntOrNull()?.coerceIn(0, 20000) ?: 0
                                                offsetValue.value = clamped.toString()
                                            }
                                            onAnyFieldChanged()
                                        },
                                        singleLine = true,
                                        enabled = !isDisabled,
                                        modifier = Modifier.fillMaxWidth(),
                                        trailingIcon = { Text("rpm", color = if (isDisabled) Color(0xFF9E9E9E) else Color(0xFFB0B0B0)) },
                                        textStyle = TextStyle(color = if (isDisabled) Color(0xFFBDBDBD) else Color.White),
                                        colors = TextFieldDefaults.colors(
                                            focusedContainerColor = Color(0xFF23272A),
                                            unfocusedContainerColor = Color(0xFF23272A),
                                            disabledContainerColor = Color(0xFF1F2123),
                                            focusedIndicatorColor = Color(0xFFFFC300),
                                            unfocusedIndicatorColor = Color(0xFFB0B0B0),
                                            disabledIndicatorColor = Color(0xFF5A5A5A),
                                            disabledTextColor = Color(0xFFBDBDBD),
                                            cursorColor = Color(0xFFFFC300)
                                        )
                                    )
                                }
                                // Slider block (value display and slider) below offset
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Emulated RPM: ${sliderValue.toInt()}",
                                    color = if (isDisabled) Color(0xFF9E9E9E) else Color(0xFFFFC300),
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                                Slider(
                                    value = sliderValue,
                                    onValueChange = {
                                        sliderValue = it
                                        val rpmJson = buildRpmJson()
                                        Log.d("Shiftlight", "RPM JSON: $rpmJson")
                                    },
                                    valueRange = 0f..20000f,
                                    steps = 19999,
                                    enabled = !isDisabled,
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = SliderDefaults.colors(
                                        thumbColor = Color(0xFFFFC300),
                                        activeTrackColor = Color(0xFFFFC300),
                                        disabledThumbColor = Color(0xFF5A5A5A),
                                        disabledActiveTrackColor = Color(0xFF5A5A5A),
                                        disabledInactiveTrackColor = Color(0xFF3A3A3A)
                                    )
                                )
                                // Status text field below the slider
                                OutlinedTextField(
                                    value = statusText,
                                    onValueChange = { statusText = it },
                                    singleLine = true,
                                    readOnly = true,
                                    enabled = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 24.dp),
                                    textStyle = TextStyle(color = statusColor),
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = Color(0xFF23272A),
                                        unfocusedContainerColor = Color(0xFF23272A),
                                        disabledContainerColor = Color(0xFF23272A),
                                        focusedIndicatorColor = Color.Transparent,
                                        unfocusedIndicatorColor = Color.Transparent,
                                        disabledIndicatorColor = Color.Transparent,
                                        disabledTextColor = statusColor,
                                        cursorColor = statusColor
                                    )
                                )
                            }
                            Button(
                                onClick = {
                                    usbDevices = listUsbSerialDevices(context)
                                    ringValues[0] = usbDevices.joinToString(", ")
                                    disableAll()
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp),
                                enabled = !isDisabled,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFB22222), // engine red
                                    contentColor = Color.White,
                                    disabledContainerColor = Color(0xFF5A1F1F),
                                    disabledContentColor = Color(0xFFDDDDDD)
                                )
                            ) {
                                Text("Connect", style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun listUsbSerialDevices(context: Context): List<String> {
        val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
        val availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(usbManager)
        return availableDrivers.map { driver ->
            val device = driver.device
            val vendor = device.vendorId.toString(16).padStart(4, '0')
            val product = device.productId.toString(16).padStart(4, '0')
            "USB VID:PID ${vendor}:${product}"
        }
    }

    // Safely parse an Int from a String; returns 0 if blank or invalid
    private fun String.toIntOrZero(): Int = this.toIntOrNull() ?: 0

    // Removed the parameterized buildValuesJson; now using a no-arg version inside setContent
}