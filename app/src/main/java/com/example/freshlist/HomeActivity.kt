package com.example.freshlist

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.freshlist.ui.theme.FreshListTheme
import com.google.firebase.auth.FirebaseAuth

class HomeActivity : ComponentActivity() {

    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // If user somehow reaches here without login, send back:
        if (auth.currentUser == null) {
            startActivity(Intent(this, SigninActivity::class.java))
            finish()
            return
        }

        setContent {
            FreshListTheme {
                HomeScreen(
                    userEmail = auth.currentUser?.email ?: "User",
                    onLogout = {
                        auth.signOut()
                        startActivity(Intent(this, SigninActivity::class.java))
                        finish()
                    }
                )
            }
        }
    }
}

@Composable
fun HomeScreen(userEmail: String, onLogout: () -> Unit) {

    val gradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFFFFD700),
            Color(0xFFFFF176)
        )
    )

    val sampleItems = listOf(
        "Milk",
        "Eggs",
        "Bread",
        "Apples",
        "Rice",
        "Pasta",
        "Tomatoes"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(gradient)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                // top padding so content is below status bar / notch
                .padding(start = 16.dp, end = 16.dp, top = 32.dp, bottom = 16.dp)
        ) {

            Text(
                text = "Welcome, $userEmail",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Your Grocery List:",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(12.dp))

            // List takes up remaining space
            LazyColumn(
                modifier = Modifier
                    .weight(1f, fill = true)
            ) {
                items(sampleItems) { item ->
                    GroceryItemCard(itemName = item)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onLogout,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
            ) {
                Text(text = "Logout", fontSize = 18.sp)
            }
        }
    }
}

@Composable
fun GroceryItemCard(itemName: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Box(modifier = Modifier.padding(16.dp)) {
            Text(
                text = itemName,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
