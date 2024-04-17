package me.study.notey.navigation

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.stevdzasan.messagebar.rememberMessageBarState
import com.stevdzasan.onetap.rememberOneTapSignInState
import io.realm.kotlin.mongodb.App
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.study.notey.models.Mood
import me.study.notey.models.RequestState
import me.study.notey.presentation.components.DisplayAlertDialog
import me.study.notey.presentation.screens.auth.AuthenticationScreen
import me.study.notey.presentation.screens.auth.AuthenticationViewModel
import me.study.notey.presentation.screens.home.HomeScreen
import me.study.notey.presentation.screens.home.HomeViewModel
import me.study.notey.presentation.screens.write.WriteScreen
import me.study.notey.presentation.screens.write.WriteViewModel
import me.study.notey.util.Constants.APP_ID
import me.study.notey.util.Constants.WRITE_SCREEN_ARGUMENT_KEY


@Composable
fun SetupNavGraph(
    startDestination: String,
    navController: NavHostController,
    onDataLoaded: () -> Unit
) {
    NavHost(
        startDestination = startDestination,
        navController = navController
    ) {
        authenticationRoute(
            navigateToHomeScreen = {
                navController.popBackStack()
                navController.navigate(Screen.Home.route)
            },
            onDataLoaded = onDataLoaded
        )
        homeRoute(
            navigateToWriteScreen = {
                navController.navigate(Screen.Write.route)
            },
            navigateToWriteScreenWithArgs = {
                navController.navigate(Screen.Write.passNoteId(noteId = it))
            },
            navigateToAuthScreen = {
                navController.popBackStack()
                navController.navigate(Screen.Authentication.route)
            },
            onDataLoaded = onDataLoaded
        )
        writeRoute(
            onBackPressed = {
                navController.popBackStack()
            }
        )
    }
}

fun NavGraphBuilder.authenticationRoute(
    navigateToHomeScreen: () -> Unit,
    onDataLoaded: () -> Unit
) {
    composable(route = Screen.Authentication.route) {
        val viewModel: AuthenticationViewModel = viewModel()
        val authenticated by viewModel.authenticated
        val loadingState by viewModel.loadingState
        val oneTapState = rememberOneTapSignInState()
        val messageBarState = rememberMessageBarState()

        LaunchedEffect(key1 = Unit) {
            onDataLoaded()
        }

        AuthenticationScreen(
            authenticated = authenticated,
            loadingState = loadingState,
            oneTapSignInState = oneTapState,
            messageBarState = messageBarState,
            onButtonClicked = {
                oneTapState.open()
                viewModel.setLoading(true)
            },
            onSuccessfulFirebaseSignIn = { tokenId ->
                viewModel.signInWithMongoAtlas(
                    tokenId = tokenId,
                    onSuccess = {
                        messageBarState.addSuccess("Authenticated with Success!")
                        viewModel.setLoading(false)
                    },
                    onError = { e ->
                        messageBarState.addError(e)
                        viewModel.setLoading(false)
                    }
                )
            },
            onFailedFirebaseSignIn = { e ->
                messageBarState.addError(e)
                viewModel.setLoading(false)
            },
            onDialogDismissed = { message ->
                messageBarState.addError(Exception(message))
                viewModel.setLoading(false)
            },
            navigateToHome = navigateToHomeScreen
        )
    }
}

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

@OptIn(ExperimentalFoundationApi::class)
fun NavGraphBuilder.writeRoute(
    onBackPressed: () -> Unit
) {
    composable(
        route = Screen.Write.route,
        arguments = listOf(navArgument(name = WRITE_SCREEN_ARGUMENT_KEY) {
            type = NavType.StringType
            nullable = true
            defaultValue = null
        })
    ) {
        val context = LocalContext.current
        val viewModel: WriteViewModel = hiltViewModel()
        val uiState = viewModel.uiState
        val pagerState = rememberPagerState(pageCount = { Mood.values().size })
        val galleryState = viewModel.galleryState
        val pageNumber by remember { derivedStateOf { pagerState.currentPage } }

        WriteScreen(
            moodName = { Mood.values()[pageNumber].name },
            uiState = uiState,
            pagerState = pagerState,
            galleryState = galleryState,
            onTitleChanged = { viewModel.setTitle(title = it) },
            onDescriptionChanged = { viewModel.setDescription(description = it) },
            onDeleteConfirmed = {
                viewModel.deleteNote(
                    onSuccess = {
                        Toast.makeText(context, "Note deleted!", Toast.LENGTH_SHORT).show()
                        onBackPressed()
                    },
                    onError = { errorMessage ->
                        Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
                    }
                )
            },
            onDateTimeUpdated = {
                viewModel.updateDateTime(
                    zonedDateTime = it
                )
            },
            onBackPressed = onBackPressed,
            onSaveClicked = {
                viewModel.upsertNote(
                    note = it.apply {
                        mood = Mood.values()[pageNumber].name
                    },
                    onSuccess = onBackPressed,
                    onError = { errorMessage ->
                        Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
                    }
                )
            },
            onImageSelect = {
                val type = context.contentResolver.getType(it)?.split("/")?.last() ?: "jpg"
                Log.d("WriteViewModel", "Uri: $it")
                viewModel.addImage(
                    image = it,
                    imageType = type
                )
            },
            onImageDeleteClicked = {
                galleryState.removeImage(it)
            }
        )
    }
}
