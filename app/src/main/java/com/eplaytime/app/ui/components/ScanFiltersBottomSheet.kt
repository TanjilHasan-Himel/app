package com.eplaytime.app.ui.components

import android.net.Uri
import android.provider.DocumentsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.eplaytime.app.ui.theme.*
import com.eplaytime.app.ui.viewmodel.MusicViewModel
import java.io.File
import kotlin.math.roundToInt

private val DURATION_STEPS = listOf(0, 15, 30, 45, 60, 90, 120)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanFiltersBottomSheet(
    onDismiss: () -> Unit,
    viewModel: MusicViewModel
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    val minDurationSec by viewModel.minSongDurationSec.collectAsState()
    val hideCallsAndVoiceNotes by viewModel.isCallRecordingHidden.collectAsState()
    val folderFilterMode by viewModel.folderFilterMode.collectAsState()
    val filteredFolders by viewModel.filteredFolders.collectAsState()
    val lastFoundCount by viewModel.lastScanFoundCount.collectAsState()
    val lastExcludedCount by viewModel.lastScanExcludedCount.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val detectedFolders by viewModel.folders.collectAsState()

    var showAddFolderDialog by remember { mutableStateOf(false) }

    // Scoped Storage Document Tree Picker
    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            val resolvedPath = resolveFolderPath(uri)
            if (resolvedPath.isNotBlank()) {
                viewModel.addFilteredFolder(resolvedPath)
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF161616),
        dragHandle = {
            BottomSheetDefaults.DragHandle(
                color = SoftGold.copy(alpha = 0.4f),
                width = 36.dp,
                height = 4.dp
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 8.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Scan filters",
                        fontFamily = OutfitFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Changes apply the next time you scan this device.",
                        fontFamily = OutfitFontFamily,
                        fontWeight = FontWeight.Normal,
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Last scan summary pill
            if (lastFoundCount > 0 || lastExcludedCount > 0) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.06f))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = SoftGold,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Last scan: $lastFoundCount songs found, $lastExcludedCount excluded",
                        fontFamily = OutfitFontFamily,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = SoftGold
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Scrollable Settings Cards
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // 1. Exclude Short Songs Card
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White.copy(alpha = 0.04f))
                            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                            .padding(18.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(SoftGold.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Timer,
                                    contentDescription = null,
                                    tint = SoftGold,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Exclude short songs",
                                    fontFamily = OutfitFontFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 16.sp,
                                    color = Color.White
                                )
                                Text(
                                    text = "Skip songs shorter than the selected duration.",
                                    fontFamily = OutfitFontFamily,
                                    fontSize = 12.sp,
                                    color = TextSecondary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Duration Readout Badge
                        val durationLabel = if (minDurationSec <= 0) "Off (Include all)" else "$minDurationSec seconds"
                        Text(
                            text = durationLabel,
                            fontFamily = OutfitFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = if (minDurationSec > 0) SoftGold else TextSecondary
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        // Stepped Slider
                        val stepIndex = remember(minDurationSec) {
                            val closest = DURATION_STEPS.minByOrNull { kotlin.math.abs(it - minDurationSec) } ?: 30
                            DURATION_STEPS.indexOf(closest).toFloat()
                        }

                        Slider(
                            value = stepIndex,
                            onValueChange = { newIndex ->
                                val rounded = newIndex.roundToInt().coerceIn(0, DURATION_STEPS.lastIndex)
                                val newSec = DURATION_STEPS[rounded]
                                if (newSec != minDurationSec) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    viewModel.setMinSongDurationSec(newSec)
                                }
                            },
                            valueRange = 0f..(DURATION_STEPS.lastIndex.toFloat()),
                            steps = DURATION_STEPS.size - 2,
                            colors = SliderDefaults.colors(
                                thumbColor = SoftGold,
                                activeTrackColor = SoftGold,
                                inactiveTrackColor = Color.White.copy(alpha = 0.15f),
                                activeTickColor = Color.Black.copy(alpha = 0.5f),
                                inactiveTickColor = Color.White.copy(alpha = 0.3f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White.copy(alpha = 0.04f))
                            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                            .clickable { viewModel.toggleCallRecordingFilter(!hideCallsAndVoiceNotes) }
                            .padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.RecordVoiceOver, null, tint = SoftGold, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Hide calls & voice notes", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                            Text(
                                "Detects call-recording and WhatsApp Voice Notes folders. WhatsApp Audio songs stay visible.",
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        }
                        Switch(
                            checked = hideCallsAndVoiceNotes,
                            onCheckedChange = viewModel::toggleCallRecordingFilter,
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.Black, checkedTrackColor = SoftGold)
                        )
                    }
                }

                // 2. Folder Filter Mode (Segmented Button)
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White.copy(alpha = 0.04f))
                            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                            .padding(18.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(SoftGold.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Tune,
                                    contentDescription = null,
                                    tint = SoftGold,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Folder filter mode",
                                    fontFamily = OutfitFontFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 16.sp,
                                    color = Color.White
                                )
                                Text(
                                    text = "Scan everything and exclude folders, or only scan selected music folders.",
                                    fontFamily = OutfitFontFamily,
                                    fontSize = 12.sp,
                                    color = TextSecondary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Segmented Button
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(24.dp))
                                .background(Color.White.copy(alpha = 0.06f))
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            val isExclude = folderFilterMode.equals("EXCLUDE", ignoreCase = true)

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(if (isExclude) SoftGold else Color.Transparent)
                                    .bounceClick {
                                        viewModel.setFolderFilterMode("EXCLUDE")
                                    }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Exclude folders",
                                    fontFamily = OutfitFontFamily,
                                    fontWeight = if (isExclude) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 13.sp,
                                    color = if (isExclude) Color.Black else TextSecondary
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(if (!isExclude) SoftGold else Color.Transparent)
                                    .bounceClick {
                                        viewModel.setFolderFilterMode("INCLUDE")
                                    }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Include only",
                                    fontFamily = OutfitFontFamily,
                                    fontWeight = if (!isExclude) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 13.sp,
                                    color = if (!isExclude) Color.Black else TextSecondary
                                )
                            }
                        }
                    }
                }

                // 3. Excluded / Included Folders List
                item {
                    val isExclude = folderFilterMode.equals("EXCLUDE", ignoreCase = true)
                    val sectionTitle = if (isExclude) "Excluded folders" else "Included folders"
                    val sectionSub = if (isExclude) {
                        "Skip songs stored in folders that match any of these paths."
                    } else {
                        "Only include songs stored in folders that match any of these paths."
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White.copy(alpha = 0.04f))
                            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                            .padding(18.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(SoftGold.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Folder,
                                    contentDescription = null,
                                    tint = SoftGold,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = sectionTitle,
                                    fontFamily = OutfitFontFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 16.sp,
                                    color = Color.White
                                )
                                Text(
                                    text = sectionSub,
                                    fontFamily = OutfitFontFamily,
                                    fontSize = 12.sp,
                                    color = TextSecondary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // + Add Folder Button
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(SoftGold.copy(alpha = 0.15f))
                                .border(1.dp, SoftGold.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
                                .bounceClick { showAddFolderDialog = true }
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    tint = SoftGold,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Add folder",
                                    fontFamily = OutfitFontFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                    color = SoftGold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // List of Folders
                        if (filteredFolders.isEmpty()) {
                            Text(
                                text = if (isExclude) "No folders excluded yet." else "No folders included yet.",
                                fontFamily = OutfitFontFamily,
                                fontSize = 13.sp,
                                color = TextSecondary.copy(alpha = 0.7f),
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                filteredFolders.forEach { folderPath ->
                                    val folderName = folderPath.trimEnd('/').substringAfterLast('/')
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color.White.copy(alpha = 0.05f))
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.FolderOpen,
                                                contentDescription = null,
                                                tint = SoftGold,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column {
                                                Text(
                                                    text = folderName,
                                                    fontFamily = OutfitFontFamily,
                                                    fontWeight = FontWeight.Medium,
                                                    fontSize = 14.sp,
                                                    color = Color.White,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = folderPath,
                                                    fontFamily = OutfitFontFamily,
                                                    fontSize = 11.sp,
                                                    color = TextSecondary,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }

                                        BounceIconButton(
                                            onClick = { viewModel.removeFilteredFolder(folderPath) },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Remove",
                                                tint = TextSecondary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 4. Bottom "Scan device" Button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(SoftGold, Color(0xFFC59F43))
                        )
                    )
                    .bounceClick(enabled = !isLoading) {
                        viewModel.rescanDevice()
                    },
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = Color.Black,
                        strokeWidth = 2.5.dp,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Scan device",
                            fontFamily = OutfitFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color.Black
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }

    // Add Folder Dialog (Quick-select detected folder or browse system tree)
    if (showAddFolderDialog) {
        Dialog(onDismissRequest = { showAddFolderDialog = false }) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(24.dp),
                color = Color(0xFF1E1E1E),
                border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Select Folder",
                        fontFamily = OutfitFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = Color.White
                    )

                    // Option 1: System Document Tree Browser
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(SoftGold.copy(alpha = 0.12f))
                            .clickable {
                                showAddFolderDialog = false
                                folderPickerLauncher.launch(null)
                            }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.FolderSpecial,
                            contentDescription = null,
                            tint = SoftGold,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Browse phone storage...",
                                fontFamily = OutfitFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp,
                                color = SoftGold
                            )
                            Text(
                                text = "Pick any directory with Android file picker",
                                fontFamily = OutfitFontFamily,
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }
                    }

                    HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

                    Text(
                        text = "Detected audio folders:",
                        fontFamily = OutfitFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp,
                        color = TextSecondary
                    )

                    // Detected Folders List
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 240.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(detectedFolders) { folder ->
                            val isAlreadyAdded = filteredFolders.contains(folder.path)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isAlreadyAdded) Color.White.copy(alpha = 0.03f) else Color.White.copy(alpha = 0.06f))
                                    .clickable(enabled = !isAlreadyAdded) {
                                        viewModel.addFilteredFolder(folder.path)
                                        showAddFolderDialog = false
                                    }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Icon(
                                        imageVector = Icons.Default.Folder,
                                        contentDescription = null,
                                        tint = if (isAlreadyAdded) TextSecondary else SoftGold,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = folder.name,
                                            fontFamily = OutfitFontFamily,
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 14.sp,
                                            color = if (isAlreadyAdded) TextSecondary else Color.White,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${folder.songCount} songs",
                                            fontFamily = OutfitFontFamily,
                                            fontSize = 11.sp,
                                            color = TextSecondary
                                        )
                                    }
                                }

                                if (isAlreadyAdded) {
                                    Text(
                                        text = "Added",
                                        fontFamily = OutfitFontFamily,
                                        fontSize = 11.sp,
                                        color = TextSecondary
                                    )
                                }
                            }
                        }
                    }

                    TextButton(
                        onClick = { showAddFolderDialog = false },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Cancel", color = SoftGold, fontFamily = OutfitFontFamily)
                    }
                }
            }
        }
    }
}

/**
 * Resolves standard file path from Document Tree Uri
 */
private fun resolveFolderPath(uri: Uri): String {
    return try {
        val docId = DocumentsContract.getTreeDocumentId(uri)
        val split = docId.split(":")
        val type = split[0]
        if ("primary".equals(type, ignoreCase = true)) {
            if (split.size > 1) "/storage/emulated/0/${split[1]}" else "/storage/emulated/0"
        } else {
            if (split.size > 1) "/storage/$type/${split[1]}" else "/storage/$type"
        }
    } catch (e: Exception) {
        uri.path ?: uri.toString()
    }
}

