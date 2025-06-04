    package com.example.nomnom

import android.app.Activity
import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.os.Build
import androidx.compose.ui.graphics.Color
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.*
import com.google.android.gms.auth.api.signin.*
import com.google.android.gms.common.api.ApiException
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.*
import kotlinx.coroutines.launch
import com.facebook.FacebookSdk
import com.facebook.appevents.AppEventsLogger
import com.facebook.CallbackManager
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.Logout
import com.google.firebase.firestore.ktx.toObject
import android.content.Context
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview

import androidx.lifecycle.viewmodel.compose.viewModel








    sealed class Screen(val route: String) {
        object Home : Screen("home")
        object AddRecipe : Screen("add_recipe")
    }

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "NomNomChannel"
            val descriptionText = "Channel for recipe notifications"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel("NOMNOM_CHANNEL_ID", name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

class MainActivity : ComponentActivity() {
    private lateinit var googleSignInClient: GoogleSignInClient
    lateinit var callbackManager: CallbackManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)
        FacebookSdk.sdkInitialize(applicationContext)
        AppEventsLogger.activateApp(application)
        createNotificationChannel(this)

        callbackManager = CallbackManager.Factory.create()



        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            //.requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        setContent {
            var isDarkTheme by remember { mutableStateOf(false) }
            val user = FirebaseAuth.getInstance().currentUser

            MaterialTheme(colorScheme = if (isDarkTheme) darkColorScheme() else lightColorScheme()) {
                MyApp(
                    startDestination = if (user != null) "home" else "login",
                    isDarkTheme = isDarkTheme,
                    toggleTheme = { isDarkTheme = !isDarkTheme },
                    googleSignInClient = googleSignInClient,
                    callbackManager = callbackManager
                )
            }
        }
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        callbackManager.onActivityResult(requestCode, resultCode, data)
    }

}

@Composable
fun MyApp(
    startDestination: String,
    isDarkTheme: Boolean,
    toggleTheme: () -> Unit,
    googleSignInClient: GoogleSignInClient,
    callbackManager: CallbackManager
) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = startDestination) {
        composable("login") {
            LoginScreen(
                navController = navController,
                toggleTheme = toggleTheme,
                isDarkTheme = isDarkTheme,
                googleSignInClient = googleSignInClient,
                callbackManager = callbackManager
            )
        }
        composable("signup") { SignUpScreen(navController) }
        composable("home") {
            HomeScreen(navController)
        }
        composable("add_recipe") {
            AddRecipeScreen(navController)

    }
}
}



    @Composable
fun LoginScreen(
    navController: NavController,
    toggleTheme: () -> Unit,
    isDarkTheme: Boolean,
    googleSignInClient: GoogleSignInClient,
    callbackManager: CallbackManager

) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val rememberMeState = RememberMeManager.getRememberMe(context).collectAsState(initial = false)
    val rememberMe = rememberMeState.value
    val auth = FirebaseAuth.getInstance()
    val activity = context as Activity



    DisposableEffect(Unit) {
        val callback = object : FacebookCallback<LoginResult> {
            override fun onSuccess(result: LoginResult) {
                val credential = FacebookAuthProvider.getCredential(result.accessToken.token)
                FirebaseAuth.getInstance().signInWithCredential(credential)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            navController.navigate("home") {
                                popUpTo("login") { inclusive = true }
                            }
                        } else {
                            errorMessage = "Facebook login failed: ${task.exception?.message}"
                        }
                    }
            }

            override fun onCancel() {
                Toast.makeText(context, "Facebook login canceled", Toast.LENGTH_SHORT).show()
            }

            override fun onError(error: FacebookException) {
                errorMessage = "Facebook login error: ${error.message}"
            }
        }

        LoginManager.getInstance().registerCallback(callbackManager, callback)

        onDispose {
            LoginManager.getInstance().unregisterCallback(callbackManager)
        }
    }



    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                val credential = GoogleAuthProvider.getCredential(account.idToken, null)
                FirebaseAuth.getInstance().signInWithCredential(credential)
                    .addOnCompleteListener { authTask ->
                        if (authTask.isSuccessful) {
                            Toast.makeText(context, "Google Sign-In Success!", Toast.LENGTH_SHORT).show()
                            navController.navigate("home") {
                                popUpTo("login") { inclusive = true }
                            }
                        } else {
                            errorMessage = "Firebase Auth failed: ${authTask.exception?.message}"
                        }
                    }
            } catch (e: ApiException) {
                errorMessage = "Google Sign-In failed: ${e.message}"
            }
        } else {
            errorMessage = "Google Sign-In cancelled or failed"
        }
    }


    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.login_headline), style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(16.dp))

            TextField(
                value = email,
                onValueChange = { email = it },
                label = { Text(stringResource(R.string.email_input_login)) },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))

            TextField(
                value = password,
                onValueChange = { password = it },
                label = { Text(stringResource(R.string.password_input_login)) },
                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { showPassword = !showPassword }) {
                        Icon(
                            imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (showPassword) "Hide password" else "Show password"
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Checkbox(
                    checked = rememberMe,
                    onCheckedChange = {
                        scope.launch {
                            RememberMeManager.setRememberMe(context, it)
                        }
                    }
                )


                Text(stringResource(R.string.remember_me_checkbox))
            }

            errorMessage?.let {
                Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (email.isBlank() || password.isBlank()) {
                        errorMessage = "Email and password cannot be empty"
                        return@Button
                    }

                    FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                scope.launch {
                                    RememberMeManager.setRememberMe(context, rememberMe)
                                }
                                navController.navigate("home") {
                                    popUpTo("login") { inclusive = true }
                                }
                            } else {
                                errorMessage = "Login failed: ${task.exception?.message}"
                            }
                        }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.login_button_login))
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(onClick = {
                val signInIntent = googleSignInClient.signInIntent
                launcher.launch(signInIntent)
            }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.sign_in_with_google_login))
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    LoginManager.getInstance().logInWithReadPermissions(
                        activity,
                        listOf("email", "public_profile")
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.login_with_facebook_login))
            }



            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    FirebaseAuth.getInstance().signInAnonymously()
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                navController.navigate("home") {
                                    popUpTo("login") { inclusive = true }
                                }
                            } else {
                                Toast.makeText(
                                    context,
                                    "Anonymous sign-in failed: ${task.exception?.message}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Text(stringResource(R.string.continue_as_guest_button))
            }


            Spacer(modifier = Modifier.height(8.dp))

            TextButton(onClick = { navController.navigate("signup") }) {
                Text(stringResource(R.string.sign_up_button))
            }

            TextButton(onClick = toggleTheme) {
                Text(if (isDarkTheme) stringResource(R.string.switch_to_light_theme) else stringResource(
                    R.string.switch_to_dark_theme
                )
                )
            }
        }
    }
}

@Composable
fun SignUpScreen(navController: NavController) {
    val context = LocalContext.current
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Box(modifier = Modifier
        .fillMaxSize()
        .padding(16.dp), contentAlignment = Alignment.Center) {
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            TextField(
                value = email,
                onValueChange = { email = it },
                label = { Text(stringResource(R.string.email_input_sign_up)) },
                modifier = Modifier.fillMaxWidth()
            )
            TextField(
                value = password,
                onValueChange = { password = it },
                label = { Text(stringResource(R.string.password_1_sign_up)) },
                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { showPassword = !showPassword }) {
                        Icon(
                            imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = null
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
            TextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = { Text(stringResource(R.string.confirm_password_sign_up)) },
                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )

            errorMessage?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
            }

            Button(
                onClick = {
                    if (email.isBlank() || password.isBlank() || confirmPassword.isBlank()) {
                        errorMessage = "All fields must be filled"
                        return@Button
                    }
                    if (password != confirmPassword) {
                        errorMessage = "Passwords do not match"
                        return@Button
                    }
                    FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                navController.navigate("home")
                            } else {
                                errorMessage = "Signup failed: ${task.exception?.message}"
                            }
                        }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.sign_up_button_register))
            }
        }
    }
}


    @Composable
    fun HomeScreen(
        navController: NavController
    ) {
        val context = LocalContext.current
        val factory = RecipeViewModelFactory(context.applicationContext as Application)
        val viewModel: RecipeViewModel = viewModel(factory = factory)

        val auth = FirebaseAuth.getInstance()
        val currentUserId = auth.currentUser?.uid.orEmpty()

        val recipes by viewModel.recipes.collectAsState()
        val isLoading by viewModel.isLoading.collectAsState()
        val errorMessage by viewModel.errorMessage.collectAsState()

        var selectedTabIndex by remember { mutableStateOf(0) }

        val tabTitles = listOf("All Recipes", "Favorites")

        Scaffold(
            floatingActionButton = {
                FloatingActionButton(onClick = {
                    navController.navigate("add_recipe")
                }) {
                    Icon(Icons.Default.Add, contentDescription = "Add Recipe")
                }
            },
            floatingActionButtonPosition = FabPosition.End,
        ) { paddingValues ->

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Tabs
                TabRow(selectedTabIndex = selectedTabIndex) {
                    tabTitles.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index },
                            text = { Text(title) }
                        )
                    }
                }

                // Content
                Box(modifier = Modifier.fillMaxSize()) {
                    when {
                        isLoading -> {
                            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                        }

                        errorMessage != null -> {
                            Text(
                                text = "Error loading recipes: $errorMessage",
                                modifier = Modifier.align(Alignment.Center)
                            )
                        }

                        else -> {
                            val filteredRecipes = when (selectedTabIndex) {
                                0 -> recipes
                                1 -> recipes.filter { currentUserId in it.favoritedBy }
                                else -> recipes
                            }

                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(filteredRecipes) { recipe ->
                                    RecipeItem(
                                        recipe = recipe,
                                        currentUserId = currentUserId,
                                        onFavoriteClick = { selectedRecipe ->
                                            viewModel.toggleFavorite(selectedRecipe, currentUserId)
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Sign Out button
                    FloatingActionButton(
                        onClick = {
                            auth.signOut()
                            Toast.makeText(context, context.getString(R.string.signed_out_popup), Toast.LENGTH_SHORT).show()
                            navController.navigate("login") {
                                popUpTo("home") { inclusive = true }
                            }
                        },
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(16.dp),
                        containerColor = MaterialTheme.colorScheme.error
                    ) {
                        Icon(Icons.Default.Logout, contentDescription = "Sign Out")
                    }
                }
            }
        }
    }





    @Composable
    fun AddRecipeScreen(navController: NavController) {
        val context = LocalContext.current
        val titleState = remember { mutableStateOf("") }
        val descriptionState = remember { mutableStateOf("") }

        val db = FirebaseFirestore.getInstance()

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(stringResource(R.string.add_a_new_recipe), style = MaterialTheme.typography.headlineMedium)
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = titleState.value,
                    onValueChange = { titleState.value = it },
                    label = { Text(stringResource(R.string.title_recipe)) },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = descriptionState.value,
                    onValueChange = { descriptionState.value = it },
                    label = { Text(stringResource(R.string.description_recipe)) },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(20.dp))

                Button(onClick = {
                    val newId = db.collection("recipes").document().id
                    val recipe = Recipes(
                        id = newId,
                        title = titleState.value,
                        description = descriptionState.value
                    )

                    db.collection("recipes")
                        .document(newId)
                        .set(recipe)
                        .addOnSuccessListener {
                            Toast.makeText(context,
                                context.getString(R.string.recipe_added_popup), Toast.LENGTH_SHORT).show()
                            navController.popBackStack()
                        }
                        .addOnFailureListener {
                            Toast.makeText(context,
                                context.getString(R.string.failed_to_add_recipe_popup), Toast.LENGTH_SHORT).show()
                        }
                }) {
                    Text(stringResource(R.string.save_button_recipe))
                }
            }
        }
    }

    @Composable
    fun RecipeItem(
        recipe: Recipes,
        currentUserId: String,
        onFavoriteClick: (Recipes) -> Unit
    ) {
        val isFavorited = recipe.favoritedBy.contains(currentUserId)

        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = recipe.title,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f)
                    )

                    IconButton(onClick = { onFavoriteClick(recipe) }) {
                        Icon(
                            imageVector = if (isFavorited) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = if (isFavorited) "Unfavorite" else "Favorite",
                            tint = if (isFavorited) Color.Red else LocalContentColor.current
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = recipe.description, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }




