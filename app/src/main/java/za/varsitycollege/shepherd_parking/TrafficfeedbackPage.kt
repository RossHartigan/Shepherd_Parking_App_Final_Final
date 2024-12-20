package za.varsitycollege.shepherd_parking

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.google.android.gms.location.LocationServices
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

enum class FeedbackCategory {
    Accident, HeavyTraffic, RoadConstruction, Other
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrafficFeedbackPage(navController: NavController, userManager: UserManager) {
    val context = LocalContext.current
    var message by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(FeedbackCategory.Other) }
    var recentUpdates by remember { mutableStateOf(listOf<Map<String, Any>>()) }
    var currentLocation by remember { mutableStateOf<String?>(null) }
    var currentUser by remember { mutableStateOf<User?>(null) }
    var errorMessageKey by remember { mutableStateOf<Int?>(null) }
    var errorMessageArg by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val firestore: FirebaseFirestore = Firebase.firestore
    val scope = rememberCoroutineScope()

    // Permission launcher to request location
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                getCurrentLocation(context) { location ->
                    currentLocation = location ?: "HARDCODED_LOCATION:28.0339,26.2096"
                }
            } else {
                errorMessage = "Location permission is required to submit feedback."
            }
        }
    )

    // Prompt for location permission every time the page loads
    LaunchedEffect(Unit) {
        locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    // Fetch current user details
    LaunchedEffect(Unit) {
        userManager.getCurrentUserEmail()?.let { email ->
            userManager.fetchUserDetails(email) { user ->
                currentUser = user
            }
        }
    }

    // Fetch recent updates from Firestore when the page loads
    LaunchedEffect(Unit) {
        val twentyFourHoursAgo = System.currentTimeMillis() - 24 * 60 * 60 * 1000
        val updates = firestore.collection("traffic_feedback")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .whereGreaterThan("timestamp", twentyFourHoursAgo)
            .limit(10)
            .get()
            .await()
            .documents.mapNotNull { it.data as? Map<String, Any> }
        recentUpdates = updates
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.MintGreen)
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Bubble
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.app_name),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = AppColors.DarkGray
                            )
                            Text(
                                text = stringResource(R.string.traffic_feedback),
                                fontSize = 18.sp,
                                color = AppColors.DarkGray
                            )
                        }
                        Image(
                            painter = painterResource(id = R.drawable.sheep),
                            contentDescription = stringResource(R.string.sheep_logo_description),
                            modifier = Modifier
                                .size(60.dp)
                                .clip(CircleShape)
                                .background(AppColors.MintGreen)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Main Content Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(16.dp))

                    // Message TextField
                    OutlinedTextField(
                        value = message,
                        onValueChange = { message = it },
                        label = { Text(stringResource(R.string.message)) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            focusedBorderColor = AppColors.MintGreen,
                            unfocusedBorderColor = AppColors.DarkGray
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Category Dropdown
                    var expanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded }
                    ) {
                        OutlinedTextField(
                            value = getCategoryString(category),  // Get the localized string
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(stringResource(R.string.category)) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            colors = TextFieldDefaults.outlinedTextFieldColors(
                                focusedBorderColor = AppColors.MintGreen,
                                unfocusedBorderColor = AppColors.DarkGray
                            )
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            FeedbackCategory.values().forEach { feedbackCategory ->
                                DropdownMenuItem(
                                    text = { Text(getCategoryString(feedbackCategory)) },  // Use localized string
                                    onClick = {
                                        category = feedbackCategory
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Submit Button
                    Button(
                        onClick = {
                            if (message.isNotBlank()) {
                                scope.launch {
                                    try {
                                        val feedbackData = hashMapOf(
                                            "message" to message,
                                            "category" to category.name,
                                            "timestamp" to System.currentTimeMillis(),
                                            "stdNumber" to (currentUser?.studentNumber ?: "Unknown"),
                                            "location" to (currentLocation ?: "HARDCODED_LOCATION:28.0339,26.2096")
                                        )

                                        firestore.collection("traffic_feedback")
                                            .add(feedbackData)
                                            .await()

                                        // Add the new update to the list
                                        recentUpdates = listOf(feedbackData as Map<String, Any>) + recentUpdates
                                        // Clear the message field and reset category
                                        message = ""
                                        category = FeedbackCategory.Other
                                        errorMessageKey = null

                                        // Show a toast message to indicate success
                                        Toast.makeText(context, "Feedback submitted successfully!", Toast.LENGTH_SHORT).show()

                                    } catch (e: Exception) {
                                        errorMessageKey = R.string.failed_to_submit_feedback
                                        errorMessageArg = e.message
                                    }
                                }
                            } else {
                                errorMessageKey = R.string.please_enter_message
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = AppColors.MintGreen),
                        enabled = message.isNotBlank()
                    ) {
                        Text(stringResource(R.string.submit), color = MaterialTheme.colorScheme.onPrimary)
                    }

                    // Display error message if any
                    errorMessageKey?.let {
                        Text(
                            text = if (errorMessageArg != null)
                                stringResource(it, errorMessageArg!!)
                            else
                                stringResource(it),
                            color = Color.Red,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }

                // View Updates Button
                OutlinedButton(
                    onClick = { navController.navigate("map_updates") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AppColors.DarkGray)
                ) {
                    Text(stringResource(R.string.view_map_updates))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Recent Updates Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        stringResource(R.string.recent_updates),
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    LazyColumn {
                        items(recentUpdates) { update ->
                            UpdateItem(
                                studentNumber = update["stdNumber"] as? String ?: "Unknown",
                                time = formatTimestamp(update["timestamp"] as? Long),
                                message = update["message"] as? String ?: "No Message",
                                category = update["category"] as? String ?: "Other"
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun UpdateItem(studentNumber: String, time: String, message: String, category: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Student: $studentNumber", fontWeight = FontWeight.Bold)
                Text(text = time, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = message, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = "Category: $category", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f))
        }
    }
}

fun getCurrentLocation(context: Context, onLocationReceived: (String?) -> Unit) {
    val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
    try {
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->
                if (location != null) {
                    onLocationReceived("${location.latitude},${location.longitude}")
                } else {
                    onLocationReceived("HARDCODED_LOCATION:28.0339,26.2096")
                }
            }
            .addOnFailureListener { exception ->
                onLocationReceived("HARDCODED_LOCATION:28.0339,26.2096")
                Log.e("Location", "Error getting location", exception)
            }
    } catch (e: SecurityException) {
        onLocationReceived("HARDCODED_LOCATION:28.0339,26.2096")
        Log.e("Location", "SecurityException when getting location", e)
    }
}

fun formatTimestamp(timestamp: Long?): String {
    if (timestamp == null) return "Unknown Time"
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

@Composable
fun getCategoryString(category: FeedbackCategory): String {
    return when (category) {
        FeedbackCategory.Accident -> stringResource(R.string.accident)
        FeedbackCategory.HeavyTraffic -> stringResource(R.string.heavy_traffic)
        FeedbackCategory.RoadConstruction -> stringResource(R.string.road_construction)
        FeedbackCategory.Other -> stringResource(R.string.other)
    }
}

@Preview(showBackground = true)
@Composable
fun TrafficFeedbackPagePreview() {
    // This is a placeholder NavController for the preview
    val navController = rememberNavController()
    // This is a placeholder UserManager for the preview
    val userManager = UserManager(LocalContext.current)

    MaterialTheme {
        TrafficFeedbackPage(navController, userManager)
    }
}
