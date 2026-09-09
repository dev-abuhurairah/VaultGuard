package com.vaultguard.app

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.LockClock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.fragment.app.FragmentActivity
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.vaultguard.app.core.repository.VaultRepository
import com.vaultguard.app.ui.screens.ItemDetailEditScreen
import com.vaultguard.app.ui.screens.MasterLockScreen
import com.vaultguard.app.ui.screens.OnboardingPermissionsScreen
import com.vaultguard.app.ui.screens.PasswordGeneratorScreen
import com.vaultguard.app.ui.screens.SecurityAuditScreen
import com.vaultguard.app.ui.screens.SettingsScreen
import com.vaultguard.app.ui.screens.TotpAuthenticatorScreen
import com.vaultguard.app.ui.screens.VaultDashboardScreen
import com.vaultguard.app.ui.theme.VaultGuardTheme

class MainActivity : FragmentActivity() {

    private lateinit var repository: VaultRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repository = VaultRepository.getInstance(applicationContext)

        // Apply FLAG_SECURE to prevent screenshots and task switcher previews
        updateScreenProtection()

        setContent {
            VaultGuardTheme {
                MainAppNavigation(repository)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updateScreenProtection()
    }

    private fun updateScreenProtection() {
        if (repository.isScreenProtectionEnabled()) {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE
            )
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }
}

@Composable
fun MainAppNavigation(repository: VaultRepository) {
    val navController = rememberNavController()
    val isUnlocked by repository.isUnlocked.collectAsState()
    val isSetup = remember { repository.isVaultSetup() }

    val startDestination = when {
        !isSetup -> "onboarding"
        !isUnlocked -> "lock"
        else -> "main_flow"
    }

    // Auto navigate to lock screen whenever vault locks
    LaunchedEffect(isUnlocked) {
        if (!isUnlocked && isSetup) {
            navController.navigate("lock") {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    NavHost(navController = navController, startDestination = startDestination) {
        composable("onboarding") {
            OnboardingPermissionsScreen(
                onPermissionsComplete = {
                    navController.navigate("lock") {
                        popUpTo("onboarding") { inclusive = true }
                    }
                }
            )
        }

        composable("lock") {
            MasterLockScreen(
                repository = repository,
                onUnlocked = {
                    navController.navigate("main_flow") {
                        popUpTo("lock") { inclusive = true }
                    }
                }
            )
        }

        composable("main_flow") {
            MainBottomNavContainer(
                repository = repository,
                onAddItem = {
                    navController.navigate("edit_item/new")
                },
                onEditItem = { itemId ->
                    navController.navigate("edit_item/$itemId")
                },
                onManagePermissions = {
                    navController.navigate("onboarding")
                },
                onLockVault = {
                    repository.lock()
                }
            )
        }

        composable(
            route = "edit_item/{itemId}",
            arguments = listOf(navArgument("itemId") { type = NavType.StringType })
        ) { backStackEntry ->
            val itemId = backStackEntry.arguments?.getString("itemId")
            ItemDetailEditScreen(
                repository = repository,
                itemId = if (itemId == "new") null else itemId,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}

@Composable
fun MainBottomNavContainer(
    repository: VaultRepository,
    onAddItem: () -> Unit,
    onEditItem: (String) -> Unit,
    onManagePermissions: () -> Unit,
    onLockVault: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = Color(0xFFFFFFFF),
                contentColor = Color(0xFF0F172A)
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.Key, contentDescription = "Vault") },
                    label = { Text("Vault") },
                    colors = navItemColors()
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.Shield, contentDescription = "Generator") },
                    label = { Text("Generator") },
                    colors = navItemColors()
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.LockClock, contentDescription = "2FA") },
                    label = { Text("2FA") },
                    colors = navItemColors()
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = { Icon(Icons.Default.Security, contentDescription = "Audit") },
                    label = { Text("Audit") },
                    colors = navItemColors()
                )
                NavigationBarItem(
                    selected = selectedTab == 4,
                    onClick = { selectedTab = 4 },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                    label = { Text("Settings") },
                    colors = navItemColors()
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFFF8FAFC))
        ) {
            when (selectedTab) {
                0 -> VaultDashboardScreen(
                    repository = repository,
                    onAddItem = onAddItem,
                    onEditItem = onEditItem,
                    onLockVault = onLockVault
                )
                1 -> PasswordGeneratorScreen()
                2 -> TotpAuthenticatorScreen(
                    repository = repository,
                    onEditItem = onEditItem
                )
                3 -> SecurityAuditScreen(
                    repository = repository,
                    onEditItem = onEditItem
                )
                4 -> SettingsScreen(
                    repository = repository,
                    onManagePermissions = onManagePermissions,
                    onLockVault = onLockVault
                )
            }
        }
    }
}

@Composable
private fun navItemColors() = NavigationBarItemDefaults.colors(
    selectedIconColor = Color(0xFFDC2626),
    selectedTextColor = Color(0xFFDC2626),
    indicatorColor = Color(0xFFFEE2E2),
    unselectedIconColor = Color(0xFF64748B),
    unselectedTextColor = Color(0xFF64748B)
)
