package me.neko.nzhelper.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.navigation.NavBackStackEntry

private const val SCREEN_TRANSITION_DURATION = 300

fun AnimatedContentTransitionScope<NavBackStackEntry>.screenEnter(): EnterTransition =
    slideIntoContainer(
        AnimatedContentTransitionScope.SlideDirection.Left,
        animationSpec = tween(SCREEN_TRANSITION_DURATION)
    ) + fadeIn(animationSpec = tween(SCREEN_TRANSITION_DURATION))

fun AnimatedContentTransitionScope<NavBackStackEntry>.screenExit(): ExitTransition =
    slideOutOfContainer(
        AnimatedContentTransitionScope.SlideDirection.Left,
        animationSpec = tween(SCREEN_TRANSITION_DURATION)
    ) + fadeOut(animationSpec = tween(SCREEN_TRANSITION_DURATION))

fun AnimatedContentTransitionScope<NavBackStackEntry>.screenPopEnter(): EnterTransition =
    slideIntoContainer(
        AnimatedContentTransitionScope.SlideDirection.Right,
        animationSpec = tween(SCREEN_TRANSITION_DURATION)
    ) + fadeIn(animationSpec = tween(SCREEN_TRANSITION_DURATION))

fun AnimatedContentTransitionScope<NavBackStackEntry>.screenPopExit(): ExitTransition =
    slideOutOfContainer(
        AnimatedContentTransitionScope.SlideDirection.Right,
        animationSpec = tween(SCREEN_TRANSITION_DURATION)
    ) + fadeOut(animationSpec = tween(SCREEN_TRANSITION_DURATION))
