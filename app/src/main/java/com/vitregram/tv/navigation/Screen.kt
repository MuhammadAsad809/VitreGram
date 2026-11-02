package com.vitregram.tv.navigation

sealed class Screen(val route: String) {
    object Auth : Screen("auth")
    object Home : Screen("home")
    object Reels : Screen("reels")
    object Stories : Screen("stories")
    object Explore : Screen("explore")
    object Search : Screen("search")
    object Profile : Screen("profile")
    object Notifications : Screen("notifications")
    object Messages : Screen("messages")
    object Settings : Screen("settings")
}
