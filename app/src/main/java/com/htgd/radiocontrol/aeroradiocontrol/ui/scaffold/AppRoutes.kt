package com.htgd.radiocontrol.aeroradiocontrol.ui.scaffold

/**
 * String-route catalog for the v4 NavGraph.
 *
 * Kept as plain const strings (vs `sealed class`) because Navigation Compose's
 * type-safe API isn't stable in 2.8.x and the screen graph is shallow. If/when
 * we adopt the navigation-compose 2.9+ type-safe routes, swap this for an
 * `@Serializable` sealed hierarchy.
 */
object AppRoutes {
    const val Splash    = "splash"
    const val Login     = "login"
    const val Main      = "main"          // hosts the 5-tab scaffold

    // Inside Main, each Tab is a nested route:
    const val TabTerminal  = "tab/terminal"
    const val TabBroadcast = "tab/broadcast"
    const val TabAI        = "tab/ai"
    const val TabTask      = "tab/task"
    const val TabService   = "tab/service"
}
