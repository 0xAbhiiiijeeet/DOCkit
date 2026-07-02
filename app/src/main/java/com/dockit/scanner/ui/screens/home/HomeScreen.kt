package com.dockit.scanner.ui.screens.home

import android.app.Activity
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.dockit.scanner.R
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import com.dockit.scanner.data.models.pdfEntity
import com.dockit.scanner.ui.screens.common.ErrorScreen
import com.dockit.scanner.ui.screens.common.LoadingDialog
import com.dockit.scanner.ui.screens.home.components.RenameDeleteDialog
import com.dockit.scanner.ui.screens.home.components.pdfLayout
import com.dockit.scanner.utils.copyPdfFileToAppDirectory
import com.dockit.scanner.utils.showToast
import com.dockit.scanner.ui.viewmodels.PdfViewModel
import com.dockit.scanner.utils.getFileSize
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(pdfViewModel: PdfViewModel) {

    LoadingDialog(pdfViewModel = pdfViewModel)
    RenameDeleteDialog(pdfViewModel = pdfViewModel)

    val activity = LocalContext.current as Activity
    val context = LocalContext.current

    val pdfList by pdfViewModel.pdfStateFlow.collectAsState()

    val scannerLauncher =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.StartIntentSenderForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val scanningResult = GmsDocumentScanningResult.fromActivityResultIntent(result.data)

                scanningResult?.pdf?.let { pdf ->
                    Log.d("pdfName", pdf.uri.lastPathSegment.toString())
                    val date = Date()
                    val fileName =
                        SimpleDateFormat("dd-MMM-yyyy HH:mm:ss", Locale.getDefault()).format(date) + ".pdf"

                    copyPdfFileToAppDirectory(context, pdf.uri, fileName)
                    val pdfEntity =
                        pdfEntity(UUID.randomUUID().toString(), fileName, getFileSize(context, fileName), date)
                    pdfViewModel.insertPdf(pdfEntity)
                }
            }
        }

    val scanner = remember {
        GmsDocumentScanning.getClient(
            GmsDocumentScannerOptions.Builder()
                .setGalleryImportAllowed(true)
                .setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_PDF)
                .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
                .build()
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(id = R.string.app_name),
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                actions = {
                    IconButton(
                        onClick = { pdfViewModel.isDarkMode = !pdfViewModel.isDarkMode },
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text(
                            text = if (pdfViewModel.isDarkMode) "☀️" else "🌙",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                    Switch(
                        checked = pdfViewModel.isDarkMode,
                        onCheckedChange = { pdfViewModel.isDarkMode = it },
                        modifier = Modifier.padding(end = 16.dp),
                        colors = androidx.compose.material3.SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = MaterialTheme.colorScheme.primary,
                            uncheckedThumbColor = Color.White,
                            uncheckedTrackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                        )
                    )
                },
                colors = TopAppBarDefaults.mediumTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    scanner.getStartScanIntent(activity).addOnSuccessListener {
                        scannerLauncher.launch(IntentSenderRequest.Builder(it).build())
                    }.addOnFailureListener {
                        it.printStackTrace()
                        context.showToast(it.message.toString())
                    }
                },
                text = {
                    Text(
                        text = stringResource(R.string.scan),
                        style = MaterialTheme.typography.labelLarge
                    )
                },
                icon = {
                    Icon(
                        painter = painterResource(R.drawable.outline_add_a_photo_24),
                        contentDescription = "Scan document"
                    )
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = MaterialTheme.shapes.large
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (pdfList.isEmpty()) {
                EmptyState()
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(
                        items = pdfList,
                        key = { pdfEntity -> pdfEntity.id }
                    ) { pdfEntity ->
                        pdfLayout(pdfEntity = pdfEntity, pdfViewModel = pdfViewModel)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EmptyState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp),
        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
    ) {
        Icon(
            painter = painterResource(R.drawable.outline_delete_24),
            contentDescription = "Empty",
            modifier = Modifier.size(96.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
        )

        Text(
            text = "No Documents Yet",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = 24.dp)
        )

        Text(
            text = "Start scanning your first document to build your library.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}
