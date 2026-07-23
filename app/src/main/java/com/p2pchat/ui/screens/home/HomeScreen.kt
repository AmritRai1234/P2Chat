package com.p2pchat.ui.screens.home

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BluetoothSearching
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.PortableWifiOff
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.ui.draw.clip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.TextButton
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.p2pchat.domain.model.ConnectionStatus
import com.p2pchat.ui.components.ConnectionStatusBar
import com.p2pchat.ui.components.PeerCard
import com.p2pchat.ui.components.PermissionCard
import com.p2pchat.ui.components.RadarVisualizer

/**
 * Home screen showing discovered/connected peers with scanning controls.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onPeerClick: (String, String) -> Unit,
    onSettingsClick: () -> Unit,
    onGroupsClick: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // Permissions
    val requiredPermissions = remember {
        buildList {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                add(Manifest.permission.BLUETOOTH_ADVERTISE)
                add(Manifest.permission.BLUETOOTH_CONNECT)
                add(Manifest.permission.BLUETOOTH_SCAN)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.NEARBY_WIFI_DEVICES)
            }
            add(Manifest.permission.ACCESS_FINE_LOCATION)
            add(Manifest.permission.ACCESS_COARSE_LOCATION)
        }.toTypedArray()
    }

    fun hasPermissions(ctx: Context, perms: Array<String>): Boolean {
        return perms.all {
            ContextCompat.checkSelfPermission(ctx, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    var permissionsGranted by remember {
        mutableStateOf(hasPermissions(context, requiredPermissions))
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val granted = results.values.isNotEmpty() && results.values.all { it }
        permissionsGranted = granted || hasPermissions(context, requiredPermissions)
        if (permissionsGranted) {
            viewModel.toggleScanning()
        }
    }

    LaunchedEffect(Unit) {
        if (hasPermissions(context, requiredPermissions)) {
            permissionsGranted = true
            if (!uiState.isScanning) {
                viewModel.toggleScanning()
            }
        }
    }

    // Show error snackbar
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }

    val showShareAppDialog = remember { mutableStateOf(false) }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    // FAB rotation animation
    val fabRotation by animateFloatAsState(
        targetValue = if (uiState.isScanning) 360f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "fabRotation"
    )

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Column {
                        Text(
                            text = "P2Chat",
                            style = MaterialTheme.typography.headlineLarge
                        )
                        Text(
                            text = "Chat without boundaries",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showShareAppDialog.value = true }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Share,
                            contentDescription = "Share P2Chat App"
                        )
                    }
                    IconButton(onClick = onGroupsClick) {
                        Icon(
                            imageVector = Icons.Filled.Group,
                            contentDescription = "Groups"
                        )
                    }
                    IconButton(onClick = onSettingsClick) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = "Settings"
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface
                )
            )

            // Share App QR Code Dialog
            if (showShareAppDialog.value) {
                val qrContent = "p2pchat://share-app?package=com.p2pchat&v=1.0.0"
                val qrBitmap = remember {
                    com.p2pchat.ui.components.QRCodeGenerator.generateQRCode(qrContent, 450, 450)
                }

                androidx.compose.material3.AlertDialog(
                    onDismissRequest = { showShareAppDialog.value = false },
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.QrCode, null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(10.dp))
                            Text("Share P2Chat App")
                        }
                    },
                    text = {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                "Scan this QR code on a nearby device to download P2Chat offline",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )

                            Spacer(Modifier.height(14.dp))

                            // QR Code Bitmap Card
                            if (qrBitmap != null) {
                                Card(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(16.dp)),
                                    colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color.White),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                                ) {
                                    androidx.compose.foundation.Image(
                                        bitmap = qrBitmap.asImageBitmap(),
                                        contentDescription = "P2Chat App QR Code",
                                        modifier = Modifier
                                            .size(210.dp)
                                            .padding(14.dp)
                                    )
                                }
                            }

                            Spacer(Modifier.height(16.dp))

                            // Action: Share APK file via Android OS
                            Button(
                                onClick = {
                                    try {
                                        val apkFile = java.io.File(context.applicationInfo.sourceDir)
                                        val uri = androidx.core.content.FileProvider.getUriForFile(
                                            context,
                                            "${context.packageName}.fileprovider",
                                            apkFile
                                        )
                                        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                            type = "application/vnd.android.package-archive"
                                            putExtra(android.content.Intent.EXTRA_STREAM, uri)
                                            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        context.startActivity(android.content.Intent.createChooser(intent, "Share P2Chat APK Offline (1.8 MB)"))
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Filled.Share, null, Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Send APK via Bluetooth / Wi-Fi")
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showShareAppDialog.value = false }) { Text("Close") }
                    }
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (!permissionsGranted) {
                        permissionLauncher.launch(requiredPermissions)
                    } else {
                        viewModel.toggleScanning()
                    }
                },
                containerColor = if (uiState.isScanning) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.primary
                },
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(
                    imageVector = if (uiState.isScanning) Icons.Filled.Stop else Icons.Filled.Radar,
                    contentDescription = if (uiState.isScanning) "Stop Scanning" else "Start Scanning",
                    modifier = Modifier.rotate(if (uiState.isScanning) 0f else fabRotation)
                )
            }
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Connection status bar
            ConnectionStatusBar(
                isAdvertising = uiState.isAdvertising,
                isDiscovering = uiState.isDiscovering,
                connectedCount = uiState.connectedCount
            )

            if (!permissionsGranted) {
                // Show permission request
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    PermissionCard(
                        onGrantClick = {
                            permissionLauncher.launch(requiredPermissions)
                        }
                    )
                }
            } else if (uiState.peers.isEmpty()) {
                // Sonar Radar Visualizer for empty/scanning state
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    RadarVisualizer(isScanning = uiState.isScanning)
                }
            } else {
                // Peer list
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = uiState.peers,
                        key = { it.endpointId }
                    ) { peer ->
                        PeerCard(
                            peer = peer,
                            onClick = {
                                if (peer.connectionStatus == ConnectionStatus.CONNECTED) {
                                    onPeerClick(peer.endpointId, peer.name)
                                } else if (peer.connectionStatus == ConnectionStatus.DISCOVERED) {
                                    viewModel.connectToPeer(peer.endpointId)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
