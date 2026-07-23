package com.p2pchat.ui.navigation

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.p2pchat.ui.screens.chat.ChatScreen
import com.p2pchat.ui.screens.groups.GroupsScreen
import com.p2pchat.ui.screens.home.HomeScreen
import com.p2pchat.ui.screens.settings.SettingsScreen
import com.p2pchat.ui.screens.splash.SplashScreen

object Routes {
    const val SPLASH = "splash"
    const val HOME = "home"
    const val CHAT = "chat/{endpointId}/{peerName}"
    const val GROUPS = "groups"
    const val GROUP_CHAT = "group_chat/{groupId}/{groupName}"
    const val SETTINGS = "settings"

    fun chatRoute(endpointId: String, peerName: String) = "chat/$endpointId/$peerName"
    fun groupChatRoute(groupId: String, groupName: String) = "group_chat/$groupId/$groupName"
}

@Composable
fun NavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Routes.SPLASH,
        enterTransition = {
            slideInHorizontally(
                initialOffsetX = { it },
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
            ) + fadeIn()
        },
        exitTransition = {
            slideOutHorizontally(
                targetOffsetX = { -it / 3 },
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
            ) + fadeOut()
        },
        popEnterTransition = {
            slideInHorizontally(
                initialOffsetX = { -it / 3 },
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
            ) + fadeIn()
        },
        popExitTransition = {
            slideOutHorizontally(
                targetOffsetX = { it },
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
            ) + fadeOut()
        }
    ) {
        composable(
            route = Routes.SPLASH,
            enterTransition = { fadeIn() },
            exitTransition = { fadeOut() }
        ) {
            SplashScreen(
                onNavigateToHome = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                }
            )
        }

        composable(route = Routes.HOME) {
            HomeScreen(
                onPeerClick = { endpointId, peerName ->
                    navController.navigate(Routes.chatRoute(endpointId, peerName))
                },
                onSettingsClick = {
                    navController.navigate(Routes.SETTINGS)
                },
                onGroupsClick = {
                    navController.navigate(Routes.GROUPS)
                }
            )
        }

        composable(
            route = Routes.CHAT,
            arguments = listOf(
                navArgument("endpointId") { type = NavType.StringType },
                navArgument("peerName") { type = NavType.StringType }
            )
        ) {
            ChatScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(route = Routes.GROUPS) {
            GroupsScreen(
                onNavigateBack = { navController.popBackStack() },
                onGroupClick = { groupId, groupName ->
                    navController.navigate(Routes.groupChatRoute(groupId, groupName))
                }
            )
        }

        composable(
            route = Routes.GROUP_CHAT,
            arguments = listOf(
                navArgument("groupId") { type = NavType.StringType },
                navArgument("groupName") { type = NavType.StringType }
            )
        ) {
            ChatScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(route = Routes.SETTINGS) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
