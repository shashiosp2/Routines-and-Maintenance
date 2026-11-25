package com.example.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.EventNote
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.EventNote
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.R
import com.example.data.ThemePreference
import com.example.ui.maintenance.MaintenanceScreen
import com.example.ui.maintenance.MaintenanceViewModel
import com.example.ui.routines.RoutineViewModel
import com.example.ui.routines.RoutinesScreen
import com.example.ui.settings.SettingsScreen
import com.example.ui.settings.SettingsViewModel
import com.example.ui.theme.RoutinaryTheme

object RoutinaryRoutes {
    const val MAINTENANCE = "maintenance"
    const val ROUTINES = "routines"
    const val SETTINGS = "settings"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutinaryApp(
    container: RoutinaryContainer
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: RoutinaryRoutes.MAINTENANCE

    val maintenanceVm: MaintenanceViewModel = viewModel(factory = container.viewModelFactory)
    val routineVm: RoutineViewModel = viewModel(factory = container.viewModelFactory)
    val settingsVm: SettingsViewModel = viewModel(factory = container.viewModelFactory)

    val maintenanceState by maintenanceVm.uiState.collectAsStateWithLifecycle()
    val routineState by routineVm.uiState.collectAsStateWithLifecycle()
    val settingsState by settingsVm.uiState.collectAsStateWithLifecycle()

    var showHistorySheet by rememberSaveable { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    // Refresh permissions, files, and elapsed routine timers whenever returning to foreground
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        settingsVm.refreshPermissionStatus()
        maintenanceVm.onResumeLifecycle()
        routineVm.onResumeLifecycle()
    }

    LaunchedEffect(maintenanceState.snackbarMessage) {
        maintenanceState.snackbarMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            maintenanceVm.clearSnackbar()
        }
    }
    LaunchedEffect(routineState.snackbarMessage) {
        routineState.snackbarMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            routineVm.clearSnackbar()
        }
    }
    LaunchedEffect(settingsState.snackbarMessage) {
        settingsState.snackbarMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            settingsVm.clearSnackbar()
        }
    }

    val systemDark = isSystemInDarkTheme()
    val isDarkTheme = when (settingsState.settings.themePreference) {
        ThemePreference.SYSTEM -> systemDark
        ThemePreference.LIGHT -> false
        ThemePreference.DARK -> true
    }

    val isSettingsRoute = currentRoute == RoutinaryRoutes.SETTINGS

    fun navigateToPrimaryTab(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    RoutinaryTheme(darkTheme = isDarkTheme) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val useNavigationRail = maxWidth >= 720.dp && !isSettingsRoute

            Scaffold(
                modifier = Modifier.fillMaxSize(),
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                topBar = {
                    CenterAlignedTopAppBar(
                        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        navigationIcon = {
                            if (isSettingsRoute) {
                                IconButton(
                                    onClick = { navController.popBackStack() },
                                    modifier = Modifier.testTag("settings_back_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                        contentDescription = stringResource(R.string.cd_back)
                                    )
                                }
                            } else {
                                Row {
                                    IconButton(
                                        onClick = {
                                            when (currentRoute) {
                                                RoutinaryRoutes.MAINTENANCE -> maintenanceVm.refresh()
                                                RoutinaryRoutes.ROUTINES -> routineVm.refresh()
                                            }
                                        },
                                        modifier = Modifier.testTag("top_reload_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Refresh,
                                            contentDescription = stringResource(R.string.cd_reload)
                                        )
                                    }
                                    IconButton(
                                        onClick = { settingsVm.toggleTheme(isDarkTheme) },
                                        modifier = Modifier.testTag("theme_toggle_button")
                                    ) {
                                        Icon(
                                            imageVector = if (isDarkTheme) {
                                                Icons.Rounded.LightMode
                                            } else {
                                                Icons.Rounded.DarkMode
                                            },
                                            contentDescription = stringResource(
                                                if (isDarkTheme) {
                                                    R.string.cd_switch_to_light
                                                } else {
                                                    R.string.cd_switch_to_dark
                                                }
                                            )
                                        )
                                    }
                                }
                            }
                        },
                    title = {
                        val titleText = when (currentRoute) {
                            RoutinaryRoutes.ROUTINES -> "My Routines"
                            RoutinaryRoutes.SETTINGS -> "Vault & Settings"
                            else -> "Maintenance"
                        }
                        Text(
                            text = titleText,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    actions = {
                        if (!isSettingsRoute) {
                            if (currentRoute == RoutinaryRoutes.MAINTENANCE) {
                                IconButton(
                                    onClick = { showHistorySheet = true },
                                    modifier = Modifier.testTag("history_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.History,
                                        contentDescription = stringResource(R.string.cd_history)
                                    )
                                }
                            }
                            IconButton(
                                onClick = { navController.navigate(RoutinaryRoutes.SETTINGS) },
                                modifier = Modifier.testTag("open_settings_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Settings,
                                    contentDescription = stringResource(R.string.cd_settings)
                                )
                            }
                        }
                    }
                )
            },
            bottomBar = {
                if (!isSettingsRoute && !useNavigationRail) {
                    NavigationBar {
                        NavigationBarItem(
                            selected = currentRoute == RoutinaryRoutes.MAINTENANCE,
                            onClick = { navigateToPrimaryTab(RoutinaryRoutes.MAINTENANCE) },
                            icon = {
                                Icon(
                                    imageVector = if (currentRoute == RoutinaryRoutes.MAINTENANCE) {
                                        Icons.Rounded.EventNote
                                    } else {
                                        Icons.Outlined.EventNote
                                    },
                                    contentDescription = null
                                )
                            },
                            label = { Text(stringResource(R.string.nav_maintenance)) },
                            modifier = Modifier.testTag("bottom_nav_maintenance")
                        )
                        NavigationBarItem(
                            selected = currentRoute == RoutinaryRoutes.ROUTINES,
                            onClick = { navigateToPrimaryTab(RoutinaryRoutes.ROUTINES) },
                            icon = {
                                Icon(
                                    imageVector = if (currentRoute == RoutinaryRoutes.ROUTINES) {
                                        Icons.Rounded.Timer
                                    } else {
                                        Icons.Outlined.Timer
                                    },
                                    contentDescription = null
                                )
                            },
                            label = { Text(stringResource(R.string.nav_routines)) },
                            modifier = Modifier.testTag("bottom_nav_routines")
                        )
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useNavigationRail) {
                    NavigationRail {
                        NavigationRailItem(
                            selected = currentRoute == RoutinaryRoutes.MAINTENANCE,
                            onClick = { navigateToPrimaryTab(RoutinaryRoutes.MAINTENANCE) },
                            icon = {
                                Icon(
                                    imageVector = if (currentRoute == RoutinaryRoutes.MAINTENANCE) {
                                        Icons.Rounded.EventNote
                                    } else {
                                        Icons.Outlined.EventNote
                                    },
                                    contentDescription = null
                                )
                            },
                            label = { Text(stringResource(R.string.nav_maintenance)) }
                        )
                        NavigationRailItem(
                            selected = currentRoute == RoutinaryRoutes.ROUTINES,
                            onClick = { navigateToPrimaryTab(RoutinaryRoutes.ROUTINES) },
                            icon = {
                                Icon(
                                    imageVector = if (currentRoute == RoutinaryRoutes.ROUTINES) {
                                        Icons.Rounded.Timer
                                    } else {
                                        Icons.Outlined.Timer
                                    },
                                    contentDescription = null
                                )
                            },
                            label = { Text(stringResource(R.string.nav_routines)) }
                        )
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = RoutinaryRoutes.MAINTENANCE,
                    modifier = Modifier.weight(1f)
                ) {
                    composable(RoutinaryRoutes.MAINTENANCE) {
                        MaintenanceScreen(
                            uiState = maintenanceState,
                            showHistorySheet = showHistorySheet,
                            onDismissHistorySheet = { showHistorySheet = false },
                            onRefresh = maintenanceVm::refresh,
                            onSortClick = maintenanceVm::onSortColumnClick,
                            onUpdatePriority = maintenanceVm::updatePriority,
                            onArmDone = maintenanceVm::armDoneConfirmation,
                            onCancelDone = maintenanceVm::cancelDoneConfirmation,
                            onConfirmDone = maintenanceVm::confirmDone,
                            onAddTask = maintenanceVm::addNewTask,
                            onInitializeStarterFiles = maintenanceVm::initializeMissingExternalFiles,
                            onOpenSettings = { navController.navigate(RoutinaryRoutes.SETTINGS) }
                        )
                    }

                    composable(RoutinaryRoutes.ROUTINES) {
                        RoutinesScreen(
                            uiState = routineState,
                            onRefresh = routineVm::refresh,
                            onToggleShowAllDays = routineVm::toggleShowAllDays,
                            onStartRoutine = routineVm::startRoutine,
                            onTogglePauseResume = routineVm::togglePauseResume,
                            onSkipStep = routineVm::skipCurrentStep,
                            onCompleteStep = routineVm::completeCurrentStep,
                            onCloseAndReset = routineVm::closeAndResetRoutine,
                            onFinishSummary = routineVm::finishSummary,
                            onInitializeStarterFiles = routineVm::initializeMissingExternalFiles,
                            onOpenSettings = { navController.navigate(RoutinaryRoutes.SETTINGS) }
                        )
                    }

                    composable(RoutinaryRoutes.SETTINGS) {
                        SettingsScreen(
                            uiState = settingsState,
                            onRefreshPermissionStatus = settingsVm::refreshPermissionStatus,
                            onSetStorageMode = settingsVm::setStorageMode,
                            onUpdateVaultRootPath = settingsVm::updateVaultRootPath,
                            onUpdateMaintenancePath = settingsVm::updateMaintenancePath,
                            onUpdateHistoryPath = settingsVm::updateHistoryPath,
                            onUpdateRoutinesPath = settingsVm::updateRoutinesPath,
                            onVaultFolderPicked = settingsVm::onVaultFolderPicked,
                            onMarkdownFilePicked = settingsVm::onMarkdownFilePicked,
                            onSetOvertimeSound = settingsVm::setOvertimeSound,
                            onSetOvertimeVibrate = settingsVm::setOvertimeVibrate,
                            onInitializeExternalFiles = settingsVm::initializeExternalFiles,
                            onResetSampleVault = settingsVm::resetSampleVault
                        )
                    }
                }
            }
        }
        }
    }
}
