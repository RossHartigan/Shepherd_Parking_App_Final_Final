package za.varsitycollege.shepherd_parking

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.google.firebase.database.*
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import android.util.Log
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn

data class ParkingSpot(
    val spotId: String,
    val isFull: Boolean,
    val zone: String = ""
)

class ParkingSpotsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                val navController = rememberNavController()
                ParkingStatusScreen(navController)
            }
        }
    }
}

// Function to fetch parking spots data from Realtime Database
fun fetchParkingSpotsRealtime(onDataChange: (List<ParkingSpot>) -> Unit) {
    val database = FirebaseDatabase.getInstance().reference.child("parkingSpots")

    database.addValueEventListener(object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            val parkingSpots = snapshot.children.mapNotNull { spotSnapshot ->
                val spotId = spotSnapshot.key ?: return@mapNotNull null
                val isFull = spotSnapshot.getValue(Int::class.java) == 1
                ParkingSpot(spotId = spotId, isFull = isFull)
            }
            onDataChange(parkingSpots)
        }

        override fun onCancelled(error: DatabaseError) {
            Log.e("RealtimeDatabase", "Failed to read parking spots", error.toException())
        }
    })
}

// Function to fetch zone information from Firestore
suspend fun fetchZoneData(): Map<String, String> {
    val db = FirebaseFirestore.getInstance()
    val zoneData = mutableMapOf<String, String>()

    try {
        val documentSnapshot = db.collection("parking_zones").get().await()

        documentSnapshot.documents.forEach { document ->
            document.data?.forEach { (zoneKey, spotList) ->
                if (spotList is List<*>) {
                    spotList.forEach { spotId ->
                        if (spotId is String) {
                            zoneData[spotId] = zoneKey
                        }
                    }
                }
            }
        }
    } catch (e: Exception) {
        Log.e("Firestore", "Error fetching zone data", e)
    }
    return zoneData
}

// Main Composable function to display parking spot statuses with zone information
@Composable
fun ParkingStatusScreen(navController: NavController) {
    val parkingSpots = remember { mutableStateListOf<ParkingSpot>() }
    val zoneData = remember { mutableStateMapOf<String, String>() }

    // Fetch data from Firebase
    LaunchedEffect(Unit) {
        fetchParkingSpotsRealtime { spots ->
            parkingSpots.clear()
            parkingSpots.addAll(spots)
        }
        zoneData.putAll(fetchZoneData())
    }

    // Organize parking spots by zones in ascending order and sort available spots to the top
    val spotsByZone = parkingSpots
        .groupBy { zoneData[it.spotId] ?: "Unknown Zone" }
        .toSortedMap(compareBy { it }) // Sort zones alphabetically
        .mapValues { (_, spots) ->
            spots.sortedBy { it.isFull } // Sort so that available spots appear first
        }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            // Header Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
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
                                text = stringResource(R.string.parking_spots),
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

            // Display each zone in its own card with sorted spots
            LazyColumn {
                spotsByZone.forEach { (zone, spots) ->
                    item {
                        ZoneParkingCard(zone = zone, parkingSpots = spots)
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Bottom Image for Map
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .aspectRatio(1.5f),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.vc_map),
                contentDescription = stringResource(R.string.parking_zones_map_description),
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(16.dp))
            )
        }
    }
}

// Composable to render a zone card with parking spots
@Composable
fun ZoneParkingCard(zone: String, parkingSpots: List<ParkingSpot>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = zone.replace("zone_", "Zone "),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = AppColors.DarkGray
            )
            Spacer(modifier = Modifier.height(8.dp))

            parkingSpots.forEach { spot ->
                ParkingSpotItem(spot, zone)
            }
        }
    }
}

// Composable to display individual parking spot status
@Composable
fun ParkingSpotItem(spot: ParkingSpot, zone: String) {
    var showDialog by remember { mutableStateOf(false) }

    val statusText = if (spot.isFull) stringResource(R.string.spot_full) else stringResource(R.string.empty)
    val statusColor = if (spot.isFull) Color(0xFFFFCDD2) else Color(0xFFC8E6C9)
    val textColor = if (spot.isFull) Color(0xFFD32F2F) else Color(0xFF388E3C)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { showDialog = true },
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(4.dp),
        colors = CardDefaults.cardColors(containerColor = statusColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Spot ${spot.spotId.replace("Spot", "")}",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
            Text(
                text = statusText,
                fontSize = 16.sp,
                color = textColor
            )
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            confirmButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("OK")
                }
            },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = AppColors.DarkGray
                    )
                    Text(
                        text = "Zone Information",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = AppColors.DarkGray
                    )
                }
            },
            text = {
                Text(
                    text = "Spot ${spot.spotId.replace("Spot", "")} is located in ${zone.replace("zone_", "Zone ")} and is currently ${if (spot.isFull) "Full" else "Empty"}."
                )
            }
        )
    }
}