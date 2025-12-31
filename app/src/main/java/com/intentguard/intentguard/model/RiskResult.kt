package com.intentguard.app.model

data class SuspicionPattern(
    val name: String,
    val description: String,
    val checkFn: (AppInfo) -> Boolean,
    val severityMultiplier: Float
)

data class DetectedIssue(
    val pattern: SuspicionPattern,
    val severity: Severity,
    val explanation: String
)

enum class Severity {
    LOW, MEDIUM, HIGH
}

data class AppAnalysis(
    val app: AppInfo,
    val issues: List<DetectedIssue>,
    val riskScore: Float,
    val humanReadableSummary: String
) {
    val isRisky: Boolean get() = riskScore >= 5.0f
    val hasCriticalIssues: Boolean get() = issues.any { it.severity == Severity.HIGH }
}

sealed class ScanState {
    object Idle : ScanState()
    data class Scanning(val progress: Int, val total: Int) : ScanState()
    data class Complete(val results: List<AppAnalysis>) : ScanState()
    data class Error(val message: String) : ScanState()
}

data class FilterOptions(
    val showOnlyRisky: Boolean = false,
    val minimumSeverity: Severity = Severity.LOW,
    val selectedCategories: Set<AppCategory> = emptySet()
)