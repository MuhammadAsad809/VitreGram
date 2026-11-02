package com.vitregram.tv.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import com.vitregram.tv.TvApplication
import com.vitregram.tv.domain.usecase.*
import com.vitregram.tv.presentation.auth.AuthScreen
import com.vitregram.tv.presentation.auth.AuthViewModel
import com.vitregram.tv.presentation.explore.ExploreScreen
import com.vitregram.tv.presentation.explore.ExploreViewModel
import com.vitregram.tv.presentation.home.HomeScreen
import com.vitregram.tv.presentation.home.HomeViewModel
import com.vitregram.tv.presentation.notifications.NotificationsScreen
import com.vitregram.tv.presentation.profile.ProfileScreen
import com.vitregram.tv.presentation.reels.ReelsScreen
import com.vitregram.tv.presentation.reels.ReelsViewModel
import com.vitregram.tv.presentation.search.SearchScreen
import com.vitregram.tv.presentation.search.SearchViewModel
import com.vitregram.tv.presentation.settings.SettingsScreen
import com.vitregram.tv.presentation.stories.StoriesScreen
import com.vitregram.tv.ui.components.GlassSidebar
import com.vitregram.tv.ui.theme.ObsidianBackground

@Composable
fun TvNavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    val repository = TvApplication.instance.instagramRepository

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: Screen.Home.route

    val showSidebar = currentRoute != Screen.Auth.route && currentRoute != Screen.Stories.route

    Row(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBackground)
    ) {
        if (showSidebar) {
            GlassSidebar(
                currentRoute = currentRoute,
                onNavigate = { targetScreen ->
                    if (currentRoute != targetScreen.route) {
                        navController.navigate(targetScreen.route) {
                            popUpTo(Screen.Home.route) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                }
            )
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {
            NavHost(
                navController = navController,
                startDestination = if (TvApplication.instance.keystoreManager.hasValidSession()) Screen.Home.route else Screen.Auth.route
            ) {
                composable(Screen.Auth.route) {
                    val authVm = remember { AuthViewModel(repository) }
                    AuthScreen(
                        viewModel = authVm,
                        onAuthenticated = {
                            navController.navigate(Screen.Home.route) {
                                popUpTo(Screen.Auth.route) { inclusive = true }
                            }
                        }
                    )
                }

                composable(Screen.Home.route) {
                    val homeVm = remember {
                        HomeViewModel(
                            getHomeFeedUseCase = GetHomeFeedUseCase(repository),
                            getStoriesUseCase = GetStoriesUseCase(repository)
                        )
                    }
                    HomeScreen(
                        viewModel = homeVm,
                        onOpenPost = { post ->
                            navController.navigate(Screen.Reels.route)
                        },
                        onOpenStory = { story ->
                            navController.navigate(Screen.Stories.route)
                        }
                    )
                }

                composable(Screen.Reels.route) {
                    val reelsVm = remember {
                        ReelsViewModel(
                            getReelsUseCase = GetReelsUseCase(repository),
                            likePostUseCase = LikePostUseCase(repository)
                        )
                    }
                    ReelsScreen(viewModel = reelsVm)
                }

                composable(Screen.Stories.route) {
                    StoriesScreen(
                        stories = emptyList(),
                        onFinish = { navController.popBackStack() }
                    )
                }

                composable(Screen.Explore.route) {
                    val exploreVm = remember { ExploreViewModel(repository) }
                    ExploreScreen(
                        viewModel = exploreVm,
                        onOpenPost = { navController.navigate(Screen.Reels.route) }
                    )
                }

                composable(Screen.Search.route) {
                    val searchVm = remember {
                        SearchViewModel(SearchUseCase(repository))
                    }
                    SearchScreen(
                        viewModel = searchVm,
                        onUserClick = { user ->
                            navController.navigate(Screen.Profile.route)
                        }
                    )
                }

                composable(Screen.Profile.route) {
                    ProfileScreen(repository = repository)
                }

                composable(Screen.Notifications.route) {
                    NotificationsScreen()
                }

                composable(Screen.Settings.route) {
                    SettingsScreen(
                        repository = repository,
                        onLogout = {
                            navController.navigate(Screen.Auth.route) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    )
                }
            }
        }
    }
}
