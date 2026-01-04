package com.intentguard.app

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.intentguard.app.logic.RiskAnalyzer
import com.intentguard.app.model.*
import com.intentguard.app.ui.theme.IntentGuardTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            IntentGuardTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    IntentGuardApp(packageManager)
                }
            }
        }
    }
}

class ScanViewModel(private val packageManager: PackageManager) : ViewModel() {
    var scanState by mutableStateOf<ScanState>(ScanState.Idle)
        private set

    var filterOptions by mutableStateOf(FilterOptions())
        private set

    fun startScan() {
        viewModelScope.launch {
            scanState = ScanState.Scanning(0, 0)
            try {
                val results = scanAllApps()
                scanState = ScanState.Complete(results)
            } catch (e: Exception) {
                scanState = ScanState.Error(e.message ?: "Unknown error occurred")
            }
        }
    }

    fun updateFilter(newOptions: FilterOptions) {
        filterOptions = newOptions
    }

    private suspend fun scanAllApps(): List<AppAnalysis> = withContext(Dispatchers.Default) {
        val packages = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
        val results = mutableListOf<AppAnalysis>()
        val total = packages.size

        packages.forEachIndexed { index, appInfo ->
            scanState = ScanState.Scanning(index + 1, total)

            try {
                val analysis = analyzePackage(appInfo)
                if (analysis != null) {
                    results.add(analysis)
                }
            } catch (e: Exception) {
                // Skip problematic apps
            }
        }

        results.sortedByDescending { it.riskScore }
    }

    private fun analyzePackage(appInfo: ApplicationInfo): AppAnalysis? {
        val packageName = appInfo.packageName
        val appName = appInfo.loadLabel(packageManager).toString()

        val packageInfo = packageManager.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS)

        val grantedPermissions = mutableSetOf<String>()
        packageInfo.requestedPermissions?.forEachIndexed { index, permission ->
            val flags = packageInfo.requestedPermissionsFlags?.get(index) ?: 0
            if ((flags and android.content.pm.PackageInfo.REQUESTED_PERMISSION_GRANTED) != 0) {
                grantedPermissions.add(permission)
            }
        }

        val services = try {
            val serviceInfo = packageManager.getPackageInfo(packageName, PackageManager.GET_SERVICES)
            serviceInfo.services?.map { it.name } ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }

        val app = AppInfo(
            packageName = packageName,
            appName = appName,
            permissions = grantedPermissions,
            services = services,
            isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0,
            installTime = packageInfo.firstInstallTime,
            versionName = packageInfo.versionName
        )

        return RiskAnalyzer.analyze(app)
    }
}

@Composable
fun IntentGuardApp(packageManager: PackageManager) {
    val viewModel = remember { ScanViewModel(packageManager) }
    val scanState = viewModel.scanState
    val filterOptions = viewModel.filterOptions

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "IntentGuard",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "Privacy Permission Analyzer",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        when (scanState) {
            is ScanState.Idle -> {
                IdleScreen(onScanClick = { viewModel.startScan() })
            }
            is ScanState.Scanning -> {
                ScanningScreen(scanState.progress, scanState.total)
            }
            is ScanState.Complete -> {
                ResultsScreen(
                    results = scanState.results,
                    filterOptions = filterOptions,
                    onFilterChange = { viewModel.updateFilter(it) },
                    onRescan = { viewModel.startScan() }
                )
            }
            is ScanState.Error -> {
                ErrorScreen(scanState.message, onRetry = { viewModel.startScan() })
            }
        }
    }
}

@Composable
fun IdleScreen(onScanClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "🛡️",
            style = MaterialTheme.typography.displayLarge
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Scan Your Apps",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Detect suspicious permission usage in installed apps",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onScanClick,
            modifier = Modifier
                .fillMaxWidth(0.6f)
                .height(56.dp)
        ) {
            Text("Start Scan", style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
fun ScanningScreen(progress: Int, total: Int) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(64.dp),
            strokeWidth = 6.dp
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Scanning Apps...",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "$progress / $total apps analyzed",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun ErrorScreen(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "❌", style = MaterialTheme.typography.displayMedium)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Scan Failed",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = onRetry) {
            Text("Retry")
        }
    }
}

@Composable
fun ResultsScreen(
    results: List<AppAnalysis>,
    filterOptions: FilterOptions,
    onFilterChange: (FilterOptions) -> Unit,
    onRescan: () -> Unit
) {
    val filteredResults = remember(results, filterOptions) {
        results.filter { analysis ->
            (!filterOptions.showOnlyRisky || analysis.isRisky) &&
                    (analysis.issues.any { it.severity.ordinal >= filterOptions.minimumSeverity.ordinal })
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        SecurityDashboard(results)

        Spacer(modifier = Modifier.height(16.dp))

        FilterBar(filterOptions, onFilterChange)

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(filteredResults) { analysis ->
                ExpandableRiskCard(analysis)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onRescan,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Rescan Apps")
        }
    }
}

@Composable
fun SecurityDashboard(results: List<AppAnalysis>) {
    val totalApps = results.size
    val riskyApps = results.count { it.isRisky }
    val criticalIssues = results.sumOf { it.issues.count { issue -> issue.severity == Severity.HIGH } }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Security Overview",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatCard(totalApps.toString(), "Apps Scanned", MaterialTheme.colorScheme.primary)
            StatCard(
                riskyApps.toString(),
                "Flagged",
                if (riskyApps > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            )
            StatCard(
                criticalIssues.toString(),
                "Critical",
                if (criticalIssues > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun StatCard(value: String, label: String, color: Color) {
    Card(
        modifier = Modifier.width(100.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun FilterBar(options: FilterOptions, onUpdate: (FilterOptions) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FilterChip(
            selected = options.showOnlyRisky,
            onClick = { onUpdate(options.copy(showOnlyRisky = !options.showOnlyRisky)) },
            label = { Text("Risky Only") }
        )

        FilterChip(
            selected = options.minimumSeverity == Severity.HIGH,
            onClick = {
                onUpdate(options.copy(
                    minimumSeverity = if (options.minimumSeverity == Severity.HIGH) Severity.LOW else Severity.HIGH
                ))
            },
            label = { Text("Critical Only") }
        )
    }
}

@Composable
fun ExpandableRiskCard(analysis: AppAnalysis) {
    var isExpanded by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { isExpanded = !isExpanded },
        colors = CardDefaults.cardColors(
            containerColor = when {
                analysis.hasCriticalIssues -> MaterialTheme.colorScheme.errorContainer
                analysis.isRisky -> MaterialTheme.colorScheme.tertiaryContainer
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = analysis.app.appName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (!isExpanded && analysis.issues.isNotEmpty()) {
                        Text(
                            text = "Tap to see ${analysis.issues.size} concern${if (analysis.issues.size > 1) "s" else ""}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }

                RiskBadge(analysis.riskScore)
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(16.dp))

                    if (analysis.hasCriticalIssues) {
                        DangerousPermissionsSummary(analysis.app)
                        Spacer(modifier = Modifier.height(12.dp))
                        Divider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    Text(
                        text = "Privacy Concerns:",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    analysis.issues.forEach { issue ->
                        IssueRow(issue)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // MANAGE PERMISSIONS BUTTON
                    OutlinedButton(
                        onClick = {
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", analysis.app.packageName, null)
                            }
                            context.startActivity(intent)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("⚙️ Manage Permissions")
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = generateRecommendation(analysis),
                        style = MaterialTheme.typography.bodySmall,
                        fontStyle = FontStyle.Italic,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun DangerousPermissionsSummary(app: AppInfo) {
    val dangerousPermissions = mutableListOf<Pair<String, String>>()

    if (app.hasPermission("CAMERA")) {
        dangerousPermissions.add("📷 Camera" to "Photos & videos")
    }
    if (app.hasPermission("RECORD_AUDIO")) {
        dangerousPermissions.add("🎤 Microphone" to "Audio recording")
    }
    if (app.hasPermission("ACCESS_FINE_LOCATION")) {
        dangerousPermissions.add("📍 Location" to "Precise GPS tracking")
    }
    if (app.hasPermission("READ_CONTACTS")) {
        dangerousPermissions.add("📇 Contacts" to "Full contact list")
    }
    if (app.hasPermission("READ_SMS")) {
        dangerousPermissions.add("💬 SMS" to "Text messages")
    }
    if (app.hasPermission("CALL_PHONE")) {
        dangerousPermissions.add("📞 Phone" to "Make calls")
    }

    if (dangerousPermissions.isNotEmpty()) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "⚠️ Active Permissions:",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )

                Spacer(modifier = Modifier.height(8.dp))

                dangerousPermissions.forEach { (emoji, description) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = emoji,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RiskBadge(score: Float) {
    val (label, color) = when {
        score >= 7.0f -> "High Risk" to MaterialTheme.colorScheme.error
        score >= 4.0f -> "Medium Risk" to MaterialTheme.colorScheme.tertiary
        else -> "Low Risk" to MaterialTheme.colorScheme.primary
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.15f)
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium,
            color = color,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun IssueRow(issue: DetectedIssue) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = when (issue.severity) {
                Severity.HIGH -> "🔴"
                Severity.MEDIUM -> "🟡"
                Severity.LOW -> "🟢"
            },
            modifier = Modifier.padding(end = 8.dp)
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = issue.pattern.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = issue.explanation,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

fun generateRecommendation(analysis: AppAnalysis): String {
    return when {
        analysis.hasCriticalIssues ->
            "💡 Tap 'Manage Permissions' above to revoke unnecessary access."
        analysis.riskScore >= 5.0f ->
            "💡 Consider if you need all features this app provides."
        else ->
            "💡 Monitor battery drain and data usage for unusual activity."
    }
}