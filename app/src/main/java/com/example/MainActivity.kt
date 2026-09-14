package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.example.data.model.Bill
import com.example.ui.navigation.Screen
import com.example.ui.screens.BillEditScreen
import com.example.ui.screens.BillListScreen
import com.example.ui.screens.CustomerPortalScreen
import com.example.ui.screens.CustomersScreen
import com.example.ui.screens.GalleryScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.NotificationScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.MandapValaTheme
import com.example.ui.viewmodel.MandapViewModel
import java.net.URLDecoder

data class NavTabItem(
    val title: String,
    val route: String,
    val icon: ImageVector,
    val testTag: String
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: MandapViewModel = viewModel()
            val isDarkTheme by viewModel.isDarkTheme.collectAsStateWithLifecycle()

            MandapValaTheme(darkTheme = isDarkTheme) {
                MandapApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun MandapApp(viewModel: MandapViewModel) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val tabs = listOf(
        NavTabItem("Home", Screen.Home.route, Icons.Default.Home, "bottom_nav_home"),
        NavTabItem("Gallery", Screen.Gallery.route, Icons.Default.Collections, "bottom_nav_gallery"),
        NavTabItem("Bills", Screen.Bills.route, Icons.Default.ReceiptLong, "bottom_nav_bills"),
        NavTabItem("Customers", Screen.Customers.route, Icons.Default.People, "bottom_nav_customers"),
        NavTabItem("Settings", Screen.Settings.route, Icons.Default.Settings, "bottom_nav_settings")
    )

    val showBottomBar = tabs.any { it.route == currentRoute }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ) {
                    tabs.forEach { tab ->
                        val selected = currentRoute == tab.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                if (currentRoute != tab.route) {
                                    navController.navigate(tab.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = tab.icon,
                                    contentDescription = tab.title
                                )
                            },
                            label = { Text(tab.title) },
                            modifier = Modifier.testTag(tab.testTag)
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    viewModel = viewModel,
                    onNavigateToNotifications = {
                        navController.navigate(Screen.Notifications.route)
                    },
                    onNavigateToNewBill = {
                        viewModel.startNewBill()
                        navController.navigate(Screen.CreateEditBill.route)
                    },
                    onNavigateToBills = {
                        navController.navigate(Screen.Bills.route)
                    },
                    onNavigateToCustomers = {
                        navController.navigate(Screen.Customers.route)
                    },
                    onNavigateToGallery = {
                        navController.navigate(Screen.Gallery.route)
                    },
                    onSelectCustomerForPortal = { id, name ->
                        navController.navigate(Screen.CustomerPortal.createRoute(id, name))
                    },
                    onEditBill = { bill: Bill ->
                        viewModel.editExistingBill(bill)
                        navController.navigate(Screen.CreateEditBill.route)
                    },
                    paddingValues = innerPadding
                )
            }

            composable(Screen.Gallery.route) {
                GalleryScreen(
                    viewModel = viewModel,
                    onOpenCustomerPortal = { id, name ->
                        navController.navigate(Screen.CustomerPortal.createRoute(id, name))
                    },
                    paddingValues = innerPadding
                )
            }

            composable(Screen.Bills.route) {
                BillListScreen(
                    viewModel = viewModel,
                    onNavigateToNewBill = {
                        viewModel.startNewBill()
                        navController.navigate(Screen.CreateEditBill.route)
                    },
                    onEditBill = { bill ->
                        viewModel.editExistingBill(bill)
                        navController.navigate(Screen.CreateEditBill.route)
                    },
                    paddingValues = innerPadding
                )
            }

            composable(Screen.Customers.route) {
                CustomersScreen(
                    viewModel = viewModel,
                    onNavigateToNewBillForCustomer = { customer ->
                        viewModel.startNewBill(customer = customer)
                        navController.navigate(Screen.CreateEditBill.route)
                    },
                    onOpenCustomerPortal = { id, name ->
                        navController.navigate(Screen.CustomerPortal.createRoute(id, name))
                    },
                    paddingValues = innerPadding
                )
            }

            composable(Screen.Settings.route) {
                SettingsScreen(
                    viewModel = viewModel,
                    paddingValues = innerPadding
                )
            }

            composable(Screen.CreateEditBill.route) {
                BillEditScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                route = Screen.CustomerPortal.route,
                arguments = listOf(
                    navArgument("customerId") { type = NavType.LongType; defaultValue = 0L },
                    navArgument("customerName") { type = NavType.StringType; defaultValue = "Customer" }
                ),
                deepLinks = listOf(
                    navDeepLink { uriPattern = "https://mandapvala.in/view/catalog?biz={biz}&cust={customerId}" },
                    navDeepLink { uriPattern = "http://mandapvala.in/view/catalog?biz={biz}&cust={customerId}" }
                )
            ) { backStackEntry ->
                val custId = backStackEntry.arguments?.getLong("customerId") ?: 0L
                val rawCustName = backStackEntry.arguments?.getString("customerName") ?: "Customer"
                val custName = try {
                    URLDecoder.decode(rawCustName, "UTF-8")
                } catch (e: Exception) {
                    rawCustName
                }

                CustomerPortalScreen(
                    customerId = custId,
                    customerName = custName,
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Screen.Notifications.route) {
                NotificationScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onNavigateToBillDraft = {
                        navController.navigate(Screen.CreateEditBill.route)
                    }
                )
            }
        }
    }
}

