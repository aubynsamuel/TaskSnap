package com.veivek.taskSnap.presentation.navigation

import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey

/**
 * Extension functions for NavBackStack to simplify navigation operations.
 */

/**
 * Navigate to a new screen. If the screen is already at the top, replace it.
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
 * Useful for replacing the current screen without adding to history.
 */
fun NavBackStack<NavKey>.navigateAndRemoveCurrent(targetScreen: NavKey) {
    this[this.lastIndex] = targetScreen
}

/**
 * Navigate to a screen and clear all other screens from backstack.
 * Useful for "reset to home" scenarios.
 */
fun NavBackStack<NavKey>.navigateAndRemoveAllOther(targetScreen: NavKey) {
    this.add(targetScreen)
    this.removeAll { navKey -> navKey != targetScreen }
}

/**
 * Clear backstack up to (and including) the target screen.
 * Useful for "back to X" navigation patterns.
 */
fun NavBackStack<NavKey>.clearUpTo(targetScreen: NavKey) {
    val targetIndex = this.indexOf(targetScreen)
    if (targetIndex != -1) {
        this.removeAll { navKey -> this.indexOf(navKey) > targetIndex }
    }
}

/**
 * Pop the backstack if there are multiple entries, otherwise do nothing.
 */
fun NavBackStack<NavKey>.popOrStay() {
    if (this.size > 1) {
        this.removeLastOrNull()
    }
}
