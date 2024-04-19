package me.study.home.navigation

import android.widget.Toast
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import io.realm.kotlin.mongodb.App
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.study.home.HomeScreen
import me.study.home.HomeViewModel
import me.study.ui.components.DisplayAlertDialog
import me.study.util.Constants.APP_ID
import me.study.util.Screen
import me.study.util.models.RequestState

fun NavGraphBuilder.homeRoute(
    navigateToWriteScreen: () -> Unit,
    navigateToWriteScreenWithArgs: (String) -> Unit,
    navigateToAuthScreen: () -> Unit,
    onDataLoaded: () -> Unit
) {
    composable(route = Screen.Home.route) {
        val context = LocalContext.current
        val viewModel: HomeViewModel = hiltViewModel()
        val notes by viewModel.notes
        val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
        var signOutDialogOpened by remember { mutableStateOf(false) }
        var deleteAllDialogOpened by remember { mutableStateOf(false) }
        val scope = rememberCoroutineScope()

        LaunchedEffect(key1 = notes) {
            if (notes !is RequestState.Loading) {
                onDataLoaded()
            }
        }

        HomeScreen(
            notes = notes,
            drawerState = drawerState,
            onMenuClicked = {
                scope.launch {
                    drawerState.open()
                }
            },
            dateIsSelected = viewModel.dateIsSelected,
            onDateSelected = {
                viewModel.getNotes(it)
            },
            onDateReset = {
                viewModel.getNotes()
            },
            onSignOutClicked = {
                signOutDialogOpened = true
            },
            onDeleteAllClicked = {
                deleteAllDialogOpened = true
            },
            navigateToWriteScreen = navigateToWriteScreen,
            navigateToWriteScreenWithArgs = navigateToWriteScreenWithArgs
        )

        DisplayAlertDialog(
            title = "Sign out",
            message = "Are you sure you want to sign out from your Google Account?",
            dialogOpened = signOutDialogOpened,
            onYesClicked = {
                scope.launch(Dispatchers.IO) {
                    val user = App.Companion.create(APP_ID).currentUser
                    if (user != null) {
                        user.logOut()
                        withContext(Dispatchers.Main) {
                            navigateToAuthScreen()
                        }
                    }
                }
            },
            onDialogClosed = { signOutDialogOpened = false }
        )

        DisplayAlertDialog(
            title = "Delete All Notes",
            message = "Are you sure you want to permanently delete all your notes?",
            dialogOpened = deleteAllDialogOpened,
            onYesClicked = {
                viewModel.deleteAllNotes(
                    onSuccess = {
                        Toast.makeText(
                            context,
                            "All Notes Deleted",
                            Toast.LENGTH_SHORT
                        ).show()
                        scope.launch {
                            drawerState.close()
                        }
                    },
                    onError = {
                        Toast.makeText(
                            context,
                            if (it.message == "No Internet Connection")
                                "We need an Internet connection for this operation"
                            else it.message,
                            Toast.LENGTH_SHORT
                        ).show()
                        scope.launch {
                            drawerState.close()
                        }
                    }
                )
            },
            onDialogClosed = { deleteAllDialogOpened = false }
        )
    }
}