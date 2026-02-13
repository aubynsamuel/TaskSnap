# Android Development Guide for AI Agents

This comprehensive guide outlines architectural patterns, coding standards, and best practices for building modern Android applications with Jetpack Compose. Follow these rules strictly to maintain consistency, quality, and maintainability.

## 1. Project Architecture

Follow **Clean Architecture** principles with clear separation of concerns:

### Layer Structure

- **Data Layer** (`com.yourapp.data`):
  - Database entities, DAOs (Room)
  - Repository implementations
  - Data sources (local/remote)
  - Services and Broadcast Receivers
  - Shared Preferences/DataStore

- **Domain Layer** (`com.yourapp.domain`) - *Optional*:
  - Pure Kotlin models (no Android dependencies)
  - Repository interfaces
  - Use cases/interactors (for complex business logic)
  - *Only create this layer if business logic justifies it*

- **Presentation Layer** (`com.yourapp.presentation`):
  - `theme/`: Material 3 design tokens (colors, typography, shapes)
  - `screens/`: Full-screen composables
  - `viewmodel/`: Hilt-injected ViewModels
  - `components/`: Reusable UI components
  - `navigation/`: Navigation configuration
  - `utils/`: Extension functions and helpers

### Key Principles

- **Domain models are pure Kotlin** - no Android framework dependencies
- **Presentation depends on Domain** - never the reverse
- **Data implements Domain interfaces** - dependency inversion

## 2. Modularity & File Organization

### The "One Item Per File" Rule

Each file should contain exactly **one** primary item (Composable, ViewModel, utility function, or data model).

**Exception**: Combine 2-3 items ONLY if they are:

- Extremely small (< 20 lines each)
- Tightly coupled and inseparable
- Not reusable elsewhere

### Examples

✅ **Good - Modular**

```'
presentation/
  components/
    UserCard.kt          // Contains only UserCard composable
    UserAvatar.kt        // Contains only UserAvatar composable
    StatusBadge.kt       // Contains only StatusBadge composable
```

❌ **Bad - Messy**

```'
presentation/
  components/
    UserComponents.kt    // Contains UserCard, UserAvatar, StatusBadge
```

### Import Best Practices

Always use specific imports, never fully-qualified names in code:

✅ **Good**

```kotlin
import androidx.compose.foundation.layout.Spacer
import androidx.compose.ui.Modifier

Spacer(modifier = Modifier.size(4.dp))
```

❌ **Bad**

```kotlin
androidx.compose.foundation.layout.Spacer(modifier = Modifier.size(4.dp))
```

## 3. Navigation 3 Implementation

Use **Navigation 3** library for type-safe, compile-time verified routing.

### Route Definitions

Define routes as `@Serializable` classes/objects implementing `NavKey`:

```kotlin
// File: presentation/navigation/AppRoutes.kt
package com.yourapp.presentation.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

sealed class AppRoutes : NavKey {
    @Serializable
    object Home : NavKey
    
    @Serializable
    data class UserProfile(val userId: String) : NavKey
    
    @Serializable
    data class ProductDetail(
        val productId: String,
        val categoryId: String? = null
    ) : NavKey
    
    @Serializable
    object Settings : NavKey
}
```

### Navigation Extensions

Create extension functions for common navigation patterns:

```kotlin
// File: presentation/navigation/NavigationExtensions.kt
package com.yourapp.presentation.navigation

import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey

/**
 * Navigate to a new screen.
 * If the target screen is already at the top of the stack, replace it instead of adding.
 * 
 * @param targetScreen The destination route
 */
fun NavBackStack<NavKey>.navigate(targetScreen: NavKey) {
    if (this.last() != targetScreen) {
        this.add(targetScreen)
    } else {
        this[this.lastIndex] = targetScreen
    }
}

/**
 * Navigate to a new screen and remove the current screen from backstack.
 * Useful for replacing the current screen without adding to navigation history.
 * 
 * Example: Login -> Home (remove Login from stack)
 * 
 * @param targetScreen The destination route
 */
fun NavBackStack<NavKey>.navigateAndRemoveCurrent(targetScreen: NavKey) {
    this[this.lastIndex] = targetScreen
}

/**
 * Navigate to a screen and clear all other screens from the backstack.
 * Useful for "reset to home" or "logout" scenarios.
 * 
 * Example: Any Screen -> Home (clear entire stack)
 * 
 * @param targetScreen The destination route (typically your home/main screen)
 */
fun NavBackStack<NavKey>.navigateAndRemoveAllOther(targetScreen: NavKey) {
    this.add(targetScreen)
    this.removeAll { navKey -> navKey != targetScreen }
}

/**
 * Clear backstack up to (and including) the target screen.
 * Useful for "back to X" navigation patterns.
 * 
 * Example: Screen A -> B -> C -> D, clearUpTo(B) results in: A -> B
 * 
 * @param targetScreen The screen to navigate back to
 */
fun NavBackStack<NavKey>.clearUpTo(targetScreen: NavKey) {
    val targetIndex = this.indexOf(targetScreen)
    if (targetIndex != -1) {
        this.removeAll { navKey -> this.indexOf(navKey) > targetIndex }
    }
}

/**
 * Pop the backstack if there are multiple entries, otherwise stay on current screen.
 * Prevents accidentally exiting the app when on the root screen.
 * 
 * Use this in "Back" button handlers.
 */
fun NavBackStack<NavKey>.popOrStay() {
    if (this.size > 1) {
        this.removeLastOrNull()
    }
}
```

### Navigation Setup

Create the main navigation composable:

```kotlin
// File: presentation/navigation/Navigation.kt
package com.yourapp.presentation.navigation

import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.retain.retain
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay

/**
 * Main navigation component for the application.
 * Uses Navigation 3 library with type-safe routes and Hilt integration.
 * 
 * @param deepLinkData Optional data for handling deep links
 */
@Composable
fun Navigation(deepLinkData: String? = null) {
    // Retain backstack across recompositions
    val backStack = retain { NavBackStack<NavKey>(AppRoutes.Home) }

    // Handle deep links
    LaunchedEffect(deepLinkData) {
        if (deepLinkData != null) {
            // Parse deep link and navigate
            backStack.navigate(AppRoutes.UserProfile(deepLinkData))
        }
    }

    NavDisplay(
        onBack = { backStack.removeLastOrNull() },
        backStack = backStack,
        modifier = Modifier.background(MaterialTheme.colorScheme.background),
        entryProvider = entryProvider {
            // Home screen
            entry<AppRoutes.Home> {
                val viewModel: HomeViewModel = hiltViewModel()
                HomeScreen(
                    backStack = backStack,
                    viewModel = viewModel
                )
            }

            // User profile screen with parameter
            entry<AppRoutes.UserProfile> {
                val viewModel: UserViewModel = hiltViewModel()
                UserProfileScreen(
                    userId = it.userId,
                    backStack = backStack,
                    viewModel = viewModel
                )
            }

            // Product detail with multiple parameters
            entry<AppRoutes.ProductDetail> {
                val viewModel: ProductViewModel = hiltViewModel()
                ProductDetailScreen(
                    productId = it.productId,
                    categoryId = it.categoryId,
                    backStack = backStack,
                    viewModel = viewModel
                )
            }

            // Settings screen
            entry<AppRoutes.Settings> {
                val viewModel: SettingsViewModel = hiltViewModel()
                SettingsScreen(
                    backStack = backStack,
                    viewModel = viewModel
                )
            }
        }
    )
}
```

### Navigation Usage in Screens

```kotlin
@Composable
fun HomeScreen(
    backStack: NavBackStack<NavKey>,
    viewModel: HomeViewModel
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Home") },
                actions = {
                    IconButton(onClick = { backStack.navigate(AppRoutes.Settings) }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding)) {
            items(viewModel.users) { user ->
                UserCard(
                    user = user,
                    onClick = { backStack.navigate(AppRoutes.UserProfile(user.id)) }
                )
            }
        }
    }
}
```

## 4. UI Standards & Best Practices

### The "No Emojis" Rule (CRITICAL)

**Never use emojis in UI text or as visual indicators.**

Use the `Icon` composable with Material Icons instead.

❌ **Bad - Using Emojis**

```kotlin
Text("🔥 Trending")
Text("📊 Dashboard")
Row {
    Text("👤")
    Text(userName)
}
```

✅ **Good - Using Icons**

```kotlin
Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
    Icon(Icons.Default.Whatshot, contentDescription = null)
    Text("Trending")
}

Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
    Icon(Icons.Default.Dashboard, contentDescription = null)
    Text("Dashboard")
}

Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
    Icon(Icons.Default.Person, contentDescription = null)
    Text(userName)
}
```

### Theming & Colors

Never hardcode colors. Always use theme colors:

❌ **Bad**

```kotlin
Box(modifier = Modifier.background(Color(0xFF6200EE)))
Text("Hello", color = Color.Red)
```

✅ **Good**

```kotlin
Box(modifier = Modifier.background(MaterialTheme.colorScheme.primary))
Text("Hello", color = MaterialTheme.colorScheme.error)
```

### Custom Theme Extensions

For domain-specific colors, create extension properties:

```kotlin
// File: presentation/theme/CustomColors.kt
package com.yourapp.presentation.theme

import androidx.compose.ui.graphics.Color

// Define custom colors
val SuccessGreen = Color(0xFF4CAF50)
val WarningOrange = Color(0xFFFF9800)
val InfoBlue = Color(0xFF2196F3)
```

For enum-based UI logic (like Quadrant icons/colors), use extension properties:

```kotlin
// File: presentation/utils/EnumExtensions.kt
package com.yourapp.presentation.utils

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.yourapp.domain.model.Priority

val Priority.icon: ImageVector
    get() = when (this) {
        Priority.HIGH -> Icons.Default.PriorityHigh
        Priority.MEDIUM -> Icons.Default.Remove
        Priority.LOW -> Icons.Default.ArrowDownward
    }

val Priority.color: Color
    get() = when (this) {
        Priority.HIGH -> Color(0xFFE53935)
        Priority.MEDIUM -> Color(0xFFFB8C00)
        Priority.LOW -> Color(0xFF43A047)
    }
```

## 5. Dependency Injection with Hilt

### Application Setup

```kotlin
@HiltAndroidApp
class MyApplication : Application()
```

### Activity Setup

```kotlin
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyAppTheme {
                Navigation()
            }
        }
    }
}
```

### ViewModel with Hilt

```kotlin
@HiltViewModel
class UserViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {
    // ViewModel implementation
}
```

### Repository Injection

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {
    @Provides
    @Singleton
    fun provideUserRepository(
        userDao: UserDao
    ): UserRepository = UserRepositoryImpl(userDao)
}
```

### ViewModel Usage in Composables

Always use `hiltViewModel()` from the correct import:

```kotlin
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun MyScreen() {
    val viewModel: MyViewModel = hiltViewModel()
    // Use viewModel
}
```

## 6. Code Quality Checklist

Before submitting any code, verify:

- [ ] **Architecture**: Is the file in the correct layer (Data/Domain/Presentation)?
- [ ] **Modularity**: One primary item per file?
- [ ] **Imports**: All imports specific, no fully-qualified names in code?
- [ ] **Navigation**: Using `AppRoutes` for all navigation?
- [ ] **Icons**: No emojis, only `Icon` composables?
- [ ] **Colors**: No hardcoded colors, using theme?
- [ ] **Hilt**: ViewModels properly annotated and injected?
- [ ] **Documentation**: Public functions have KDoc comments?

## 7. Common Patterns

### Loading States

```kotlin
@Composable
fun DataScreen(viewModel: DataViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    
    when (uiState) {
        is UiState.Loading -> LoadingIndicator()
        is UiState.Success -> SuccessContent(uiState.data)
        is UiState.Error -> ErrorMessage(uiState.message)
    }
}
```

### Dialog Management

```kotlin
@Composable
fun MyScreen() {
    var showDialog by remember { mutableStateOf(false) }
    
    Button(onClick = { showDialog = true }) {
        Text("Show Dialog")
    }
    
    if (showDialog) {
        MyDialog(onDismiss = { showDialog = false })
    }
}
```

### List with Empty State

```kotlin
@Composable
fun ItemList(items: List<Item>) {
    if (items.isEmpty()) {
        EmptyState()
    } else {
        LazyColumn {
            items(items, key = { it.id }) { item ->
                ItemCard(item = item)
            }
        }
    }
}
```

## 8. Summary for AI Agents

When working on this codebase:

1. **Check layer placement**: Data, Domain, or Presentation?
2. **One file, one purpose**: Modular organization
3. **Type-safe navigation**: Use `AppRoutes` sealed class
4. **Icons over emojis**: Always use `Icon` composable
5. **Theme colors only**: No hardcoded colors
6. **Hilt everywhere**: ViewModels and repositories
7. **Specific imports**: Never use fully-qualified names
8. **Document public APIs**: Add KDoc to public functions

Following these guidelines ensures a maintainable, scalable, and professional Android application.
