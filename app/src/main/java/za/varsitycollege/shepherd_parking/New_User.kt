package za.varsitycollege.shepherd_parking

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject
import za.varsitycollege.shepherd_parking.AppColors
import za.varsitycollege.shepherd_parking.R
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewUserPage(navController: NavController) {
    var name by remember { mutableStateOf("") }
    var surname by remember { mutableStateOf("") }
    var studentNumber by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPopup by remember { mutableStateOf(false) }
    var shouldNavigate by remember { mutableStateOf(false) }
    var nameError by remember { mutableStateOf(false) }
    var surnameError by remember { mutableStateOf(false) }
    var studentNumberError by remember { mutableStateOf(false) }
    var emailError by remember { mutableStateOf(false) }
    var passwordError by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val auth = remember { FirebaseAuth.getInstance() }
    val userManager = remember { UserManager(context) }

    LaunchedEffect(showPopup) {
        if (showPopup) {
            delay(2000)
            showPopup = false
            shouldNavigate = true
        }
    }

    LaunchedEffect(shouldNavigate) {
        if (shouldNavigate) {
            navController.navigate("login")
            shouldNavigate = false
        }
    }

    val isSignUpEnabled = name.isNotBlank() && surname.isNotBlank() &&
            studentNumber.isNotBlank() && email.isNotBlank() &&
            password.isNotBlank()

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
            Spacer(modifier = Modifier.height(16.dp))

            // Header Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
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
                                text = stringResource(R.string.sign_up),
                                fontSize = 18.sp,
                                color = AppColors.DarkGray
                            )
                        }
                        Image(
                            painter = painterResource(id = R.drawable.sheep),
                            contentDescription = stringResource(R.string.varsity_college_logo),
                            modifier = Modifier
                                .size(60.dp)
                                .clip(CircleShape)
                                .background(AppColors.MintGreen)
                        )
                    }
                }
            }

            // Sign-up form Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 1.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it; nameError = false },
                        label = { Text(stringResource(R.string.name)) },
                        isError = nameError,
                        supportingText = { if (nameError) Text(stringResource(R.string.this_field_is_required)) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            focusedBorderColor = AppColors.MintGreen,
                            unfocusedBorderColor = AppColors.DarkGray,
                            errorBorderColor = Color.Red
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = surname,
                        onValueChange = { surname = it; surnameError = false },
                        label = { Text(stringResource(R.string.surname)) },
                        isError = surnameError,
                        supportingText = { if (surnameError) Text(stringResource(R.string.this_field_is_required)) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            focusedBorderColor = AppColors.MintGreen,
                            unfocusedBorderColor = AppColors.DarkGray,
                            errorBorderColor = Color.Red
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = studentNumber,
                        onValueChange = { studentNumber = it; studentNumberError = false },
                        label = { Text(stringResource(R.string.student_number)) },
                        isError = studentNumberError,
                        supportingText = { if (studentNumberError) Text(stringResource(R.string.this_field_is_required)) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            focusedBorderColor = AppColors.MintGreen,
                            unfocusedBorderColor = AppColors.DarkGray,
                            errorBorderColor = Color.Red
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it; emailError = false },
                        label = { Text(stringResource(R.string.email)) },
                        isError = emailError,
                        supportingText = { if (emailError) Text(stringResource(R.string.this_field_is_required)) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            focusedBorderColor = AppColors.MintGreen,
                            unfocusedBorderColor = AppColors.DarkGray,
                            errorBorderColor = Color.Red
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it; passwordError = false },
                        label = { Text(stringResource(R.string.password)) },
                        isError = passwordError,
                        supportingText = { if (passwordError) Text(stringResource(R.string.this_field_is_required)) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            focusedBorderColor = AppColors.MintGreen,
                            unfocusedBorderColor = AppColors.DarkGray,
                            errorBorderColor = Color.Red
                        ),
                        visualTransformation = PasswordVisualTransformation()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            if (isSignUpEnabled) {
                                auth.createUserWithEmailAndPassword(email, password)
                                    .addOnCompleteListener { task ->
                                        if (task.isSuccessful) {
                                            val firebaseUser = auth.currentUser
                                            firebaseUser?.let {
                                                userManager.saveUser(
                                                    firebaseUser,
                                                    name,
                                                    surname,
                                                    studentNumber
                                                )
                                            }
                                            // Call the welcome email function here
                                            sendWelcomeEmail(name, surname, email)
                                            showPopup = true
                                        } else {
                                            // Handle sign-up failure
                                            emailError = true
                                            passwordError = true
                                        }
                                    }
                            } else {
                                nameError = name.isBlank()
                                surnameError = surname.isBlank()
                                studentNumberError = studentNumber.isBlank()
                                emailError = email.isBlank()
                                passwordError = password.isBlank()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AppColors.MintGreen)
                    ) {
                        Text(
                            text = stringResource(R.string.sign_up),
                            color = Color.Black,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = stringResource(R.string.already_have_account),
                            color = Color.Black,
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stringResource(R.string.login),
                            color = Color.Blue,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable {
                                navController.navigate("login")
                            }
                        )
                    }
                }
            }
        }

        // Varsity College logo
        Image(
            painter = painterResource(id = R.drawable.varsity_college_icon),
            contentDescription = stringResource(R.string.varsity_college_logo),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
                .height(50.dp)
                .clickable {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.varsitycollege.co.za/"))
                    context.startActivity(intent)
                }
        )

        // Popup Notification
        AnimatedVisibility(
            visible = showPopup,
            enter = fadeIn() + slideInVertically(),
            exit = fadeOut() + slideOutVertically()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .align(Alignment.TopCenter)
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = AppColors.MintGreen),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.thanks_for_signing_up),
                        color = Color.Black,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    }
}

private fun sendWelcomeEmail(name: String, surname: String, email: String) {
    val url = "https://europe-west1-shepherd-parking-functions.cloudfunctions.net/newUser"

    CoroutineScope(Dispatchers.IO).launch {
        try {
            val connection = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json")
                doOutput = true
            }

            // Prepare the JSON payload
            val jsonInput = JSONObject().apply {
                put("email", email)
                put("first_name", name)
                put("last_name", surname)
            }

            Log.d("SendWelcomeEmail", "Sending JSON payload: $jsonInput")

            // Write the JSON payload to the output stream
            connection.outputStream.use { os: OutputStream ->
                os.write(jsonInput.toString().toByteArray())
            }

            // Check the response code and log results
            val responseCode = connection.responseCode
            val responseMessage = connection.responseMessage

            if (responseCode == HttpURLConnection.HTTP_OK) {
                val response = BufferedReader(InputStreamReader(connection.inputStream)).use { it.readText() }
                Log.i("SendWelcomeEmail", "Email sent successfully: $response")
            } else {
                val errorResponse = BufferedReader(InputStreamReader(connection.errorStream)).use { it.readText() }
                Log.e("SendWelcomeEmail", "Failed to send email: $responseCode - $responseMessage. Server response: $errorResponse")
            }

        } catch (e: Exception) {
            Log.e("SendWelcomeEmail", "Error sending email: ${e.message}", e)
        }
    }
}



@Preview(showBackground = true)
@Composable
fun NewUserPagePreview() {
    val navController = rememberNavController()
    MaterialTheme {
        NewUserPage(navController)
    }
}
