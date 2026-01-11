package com.example.freshlist

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.freshlist.ui.theme.FreshListTheme
import com.google.firebase.auth.FirebaseAuth
import kotlin.math.roundToInt

class HomeActivity : ComponentActivity() {

    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (auth.currentUser == null) {
            startActivity(Intent(this, SigninActivity::class.java))
            finish()
            return
        }

        setContent {
            FreshListTheme {
                val userEmail = auth.currentUser?.email ?: "User"

                FreshListApp(
                    userEmail = userEmail,
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

/* ----------------------------- MODELS ----------------------------- */

data class GroceryItem(
    val id: String,
    val name: String,
    val category: String,
    val price: Double,
    val unit: String = "each",
    val icon: @Composable () -> Unit = { Icon(Icons.Default.ShoppingBasket, contentDescription = null) }
)

data class CartLine(
    val item: GroceryItem,
    val qty: Int
)

/* ----------------------------- VIEWMODEL ----------------------------- */

class FreshListViewModel : ViewModel() {

    private val _items = listOf(
        GroceryItem("1", "Milk", "Dairy", 1.35, "1L") { Icon(Icons.Default.LocalCafe, null) },
        GroceryItem("2", "Eggs", "Dairy", 2.10, "12 pack") { Icon(Icons.Default.Egg, null) },
        GroceryItem("3", "Bread", "Bakery", 1.10, "loaf") { Icon(Icons.Default.BakeryDining, null) },
        GroceryItem("4", "Apples", "Fruits", 1.60, "1kg") { Icon(Icons.Default.Favorite, null) },
        GroceryItem("5", "Rice", "Pantry", 2.40, "1kg") { Icon(Icons.Default.Restaurant, null) },
        GroceryItem("6", "Pasta", "Pantry", 1.25, "500g") { Icon(Icons.Default.RestaurantMenu, null) },
        GroceryItem("7", "Tomatoes", "Vegetables", 1.30, "500g") { Icon(Icons.Default.Spa, null) },
        GroceryItem("8", "Chicken Breast", "Meat", 4.80, "500g") { Icon(Icons.Default.LunchDining, null) },
        GroceryItem("9", "Orange Juice", "Drinks", 2.20, "1L") { Icon(Icons.Default.LocalDrink, null) },
        GroceryItem("10", "Cheese", "Dairy", 2.90, "200g") { Icon(Icons.Default.Fastfood, null) },
    )

    var searchQuery by mutableStateOf("")
        private set

    var selectedCategory by mutableStateOf("All")
        private set

    private val cartMap = mutableStateMapOf<String, Int>()

    val categories: List<String> = listOf("All") + _items.map { it.category }.distinct().sorted()

    //  FIX: renamed to avoid JVM setter clash
    fun updateSearchQuery(value: String) { searchQuery = value }

    fun setCategory(value: String) { selectedCategory = value }

    fun filteredItems(): List<GroceryItem> {
        val q = searchQuery.trim().lowercase()
        return _items.filter { item ->
            val matchCategory = (selectedCategory == "All" || item.category == selectedCategory)
            val matchQuery =
                (q.isEmpty() || item.name.lowercase().contains(q) || item.category.lowercase().contains(q))
            matchCategory && matchQuery
        }
    }

    fun qtyInCart(itemId: String): Int = cartMap[itemId] ?: 0

    fun addToCart(item: GroceryItem) {
        val current = cartMap[item.id] ?: 0
        cartMap[item.id] = current + 1
    }

    fun increment(itemId: String) {
        val current = cartMap[itemId] ?: 0
        cartMap[itemId] = current + 1
    }

    fun decrement(itemId: String) {
        val current = cartMap[itemId] ?: 0
        if (current <= 1) cartMap.remove(itemId) else cartMap[itemId] = current - 1
    }

    fun remove(itemId: String) { cartMap.remove(itemId) }
    fun clearCart() { cartMap.clear() }

    fun cartLines(): List<CartLine> {
        val byId = _items.associateBy { it.id }
        return cartMap.entries.mapNotNull { (id, qty) ->
            val item = byId[id] ?: return@mapNotNull null
            CartLine(item, qty)
        }.sortedBy { it.item.name }
    }

    fun subtotal(): Double = cartLines().sumOf { it.item.price * it.qty }
    fun deliveryFee(): Double = if (subtotal() > 0) 1.50 else 0.0
    fun total(): Double = subtotal() + deliveryFee()
}

/* ----------------------------- NAV ----------------------------- */

private sealed class BottomTab(val route: String, val label: String, val icon: @Composable () -> Unit) {
    data object Shop : BottomTab("shop", "Shop", { Icon(Icons.Default.Storefront, null) })
    data object Cart : BottomTab("cart", "Cart", { Icon(Icons.Default.ShoppingCart, null) })
    data object Profile : BottomTab("profile", "Profile", { Icon(Icons.Default.Person, null) })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FreshListApp(
    userEmail: String,
    onLogout: () -> Unit,
    vm: FreshListViewModel = viewModel()
) {
    val navController = rememberNavController()
    val tabs = listOf(BottomTab.Shop, BottomTab.Cart, BottomTab.Profile)

    val gradient = Brush.verticalGradient(
        colors = listOf(Color(0xFFFFD700), Color(0xFFFFF176))
    )

    Scaffold(
        topBar = {
            val backStack by navController.currentBackStackEntryAsState()
            val route = backStack?.destination?.route ?: BottomTab.Shop.route

            TopAppBar(
                title = {
                    Text(
                        text = when (route) {
                            "shop" -> "FreshList"
                            "cart" -> "Your Cart"
                            "profile" -> "Profile"
                            "checkout" -> "Checkout"
                            "payment" -> "Payment"
                            "success" -> "Order Confirmed"
                            else -> "FreshList"
                        },
                        fontWeight = FontWeight.Bold
                    )
                }
            )
        },
        bottomBar = {
            val backStack by navController.currentBackStackEntryAsState()
            val route = backStack?.destination?.route ?: BottomTab.Shop.route
            val showBottomBar = route in setOf("shop", "cart", "profile")

            if (showBottomBar) {
                NavigationBar {
                    tabs.forEach { tab ->
                        val selected = route == tab.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(tab.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = tab.icon,
                            label = { Text(tab.label) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(gradient)
                .padding(padding)
        ) {
            NavHost(navController = navController, startDestination = "shop") {
                composable("shop") { ShopScreen(userEmail, vm) { navController.navigate("cart") } }
                composable("cart") { CartScreen(vm) { navController.navigate("checkout") } }
                composable("profile") { ProfileScreen(userEmail, onLogout) }
                composable("checkout") { CheckoutScreen(vm, { navController.popBackStack() }) { navController.navigate("payment") } }
                composable("payment") {
                    PaymentScreen(
                        total = vm.total(),
                        onPay = {
                            vm.clearCart()
                            navController.navigate("success")
                        },
                        onBack = { navController.popBackStack() }
                    )
                }
                composable("success") { SuccessScreen { navController.navigate("shop") } }
            }
        }
    }
}

/* ----------------------------- SHOP ----------------------------- */

@Composable
private fun ShopScreen(userEmail: String, vm: FreshListViewModel, onGoToCart: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Welcome, $userEmail", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
        Spacer(Modifier.height(10.dp))

        OutlinedTextField(
            value = vm.searchQuery,
            onValueChange = vm::updateSearchQuery, //  FIXED
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Default.Search, null) },
            placeholder = { Text("Search groceries…") },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
        )

        Spacer(Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            vm.categories.forEach { cat ->
                FilterChip(
                    selected = vm.selectedCategory == cat,
                    onClick = { vm.setCategory(cat) },
                    label = { Text(cat) }
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        val items = vm.filteredItems()
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 14.dp)
        ) {
            items(items.size) { index ->
                val item = items[index]
                ShopItemCard(
                    item = item,
                    qtyInCart = vm.qtyInCart(item.id),
                    onAdd = { vm.addToCart(item) },
                    onPlus = { vm.increment(item.id) },
                    onMinus = { vm.decrement(item.id) }
                )
            }
        }
    }
}

@Composable
private fun ShopItemCard(
    item: GroceryItem,
    qtyInCart: Int,
    onAdd: () -> Unit,
    onPlus: () -> Unit,
    onMinus: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(containerColor = Color.White)
    ) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {

            Surface(shape = RoundedCornerShape(14.dp), color = Color(0xFFF4F4F4), modifier = Modifier.size(46.dp)) {
                Box(contentAlignment = Alignment.Center) { item.icon() }
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(item.name, fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${item.category} • ${item.unit}", color = Color.Gray, fontSize = 13.sp)
                Text("£${formatMoney(item.price)}", fontWeight = FontWeight.SemiBold)
            }

            if (qtyInCart <= 0) {
                Button(onClick = onAdd) { Text("Add") }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onMinus) { Icon(Icons.Default.RemoveCircleOutline, null) }
                    Text(qtyInCart.toString(), fontWeight = FontWeight.Bold)
                    IconButton(onClick = onPlus) { Icon(Icons.Default.AddCircleOutline, null) }
                }
            }
        }
    }
}

/* ----------------------------- CART ----------------------------- */

@Composable
private fun CartScreen(vm: FreshListViewModel, onCheckout: () -> Unit) {
    val lines = vm.cartLines()
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {

        if (lines.isEmpty()) {
            Text("Your cart is empty.", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            return
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(lines.size) { i ->
                val line = lines[i]
                CartLineCard(
                    line,
                    onPlus = { vm.increment(line.item.id) },
                    onMinus = { vm.decrement(line.item.id) },
                    onRemove = { vm.remove(line.item.id) }
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        Button(onClick = onCheckout, modifier = Modifier.fillMaxWidth().height(50.dp)) {
            Text("Checkout (£${formatMoney(vm.total())})")
        }
    }
}

@Composable
private fun CartLineCard(line: CartLine, onPlus: () -> Unit, onMinus: () -> Unit, onRemove: () -> Unit) {
    ElevatedCard(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.elevatedCardColors(containerColor = Color.White)) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {

            Column(modifier = Modifier.weight(1f)) {
                Text(line.item.name, fontWeight = FontWeight.Bold)
                Text("£${formatMoney(line.item.price)} • ${line.item.unit}", color = Color.Gray, fontSize = 13.sp)
            }

            IconButton(onClick = onRemove) { Icon(Icons.Default.Delete, null) }
            IconButton(onClick = onMinus) { Icon(Icons.Default.RemoveCircleOutline, null) }
            Text(line.qty.toString(), fontWeight = FontWeight.Bold)
            IconButton(onClick = onPlus) { Icon(Icons.Default.AddCircleOutline, null) }
        }
    }
}

/* ----------------------------- CHECKOUT & PAYMENT ----------------------------- */

@Composable
private fun CheckoutScreen(vm: FreshListViewModel, onBack: () -> Unit, onProceed: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        ElevatedCard(colors = CardDefaults.elevatedCardColors(containerColor = Color.White)) {
            Column(Modifier.fillMaxWidth().padding(14.dp)) {
                Text("Order Total", fontWeight = FontWeight.Bold)
                Text("£${formatMoney(vm.total())}", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(Modifier.height(12.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick = onBack, modifier = Modifier.weight(1f).height(50.dp)) { Text("Back") }
            Button(onClick = onProceed, modifier = Modifier.weight(1f).height(50.dp)) { Text("Pay") }
        }
    }
}

@Composable
private fun PaymentScreen(total: Double, onPay: () -> Unit, onBack: () -> Unit) {
    var cardNumber by remember { mutableStateOf("") }
    var cvv by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Payment (Demo)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Spacer(Modifier.height(12.dp))

        ElevatedCard(colors = CardDefaults.elevatedCardColors(containerColor = Color.White)) {
            Column(Modifier.fillMaxWidth().padding(14.dp)) {
                Text("Total: £${formatMoney(total)}", fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(10.dp))

                OutlinedTextField(
                    value = cardNumber,
                    onValueChange = { cardNumber = it.filter { ch -> ch.isDigit() }.take(16) },
                    label = { Text("Card Number") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )

                Spacer(Modifier.height(10.dp))

                OutlinedTextField(
                    value = cvv,
                    onValueChange = { cvv = it.filter { ch -> ch.isDigit() }.take(3) },
                    label = { Text("CVV") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )

                Spacer(Modifier.height(12.dp))

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = onBack, modifier = Modifier.weight(1f)) { Text("Back") }
                    Button(
                        onClick = { if (cardNumber.length == 16 && cvv.length == 3) onPay() },
                        modifier = Modifier.weight(1f)
                    ) { Text("Pay") }
                }
            }
        }
    }
}

@Composable
private fun SuccessScreen(onContinueShopping: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(shape = RoundedCornerShape(30.dp), color = Color.White, shadowElevation = 6.dp) {
            Column(Modifier.padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF2E7D32), modifier = Modifier.size(52.dp))
                Spacer(Modifier.height(10.dp))
                Text("Payment Successful!", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(Modifier.height(14.dp))
                Button(onClick = onContinueShopping) { Text("Continue Shopping") }
            }
        }
    }
}

/* ----------------------------- PROFILE ----------------------------- */

@Composable
private fun ProfileScreen(userEmail: String, onLogout: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        ElevatedCard(colors = CardDefaults.elevatedCardColors(containerColor = Color.White)) {
            Column(Modifier.fillMaxWidth().padding(14.dp)) {
                Text("Signed in as", color = Color.Gray)
                Text(userEmail, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
        Spacer(Modifier.height(12.dp))
        Button(onClick = onLogout, modifier = Modifier.fillMaxWidth().height(50.dp)) {
            Icon(Icons.Default.Logout, null)
            Spacer(Modifier.width(8.dp))
            Text("Logout")
        }
    }
}

/* ----------------------------- HELPERS ----------------------------- */

private fun formatMoney(value: Double): String {
    val rounded = ((value * 100).roundToInt()) / 100.0
    return String.format("%.2f", rounded)
}
