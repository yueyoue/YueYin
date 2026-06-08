package com.yueyin.ui.navi

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Discover : Screen("discover")
    object Library : Screen("library")
    object Search : Screen("search")
    object Profile : Screen("profile")
    object Player : Screen("player/{bookId}") {
        fun createRoute(bookId: String) = "player/$bookId"
    }
    object LibraryDetail : Screen("libraryDetail/{libraryId}/{libraryName}") {
        fun createRoute(libraryId: String, libraryName: String) = "libraryDetail/$libraryId/$libraryName"
    }
}
