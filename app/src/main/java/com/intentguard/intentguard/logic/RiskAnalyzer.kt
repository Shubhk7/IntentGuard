package com.intentguard.app.logic

import com.intentguard.app.model.*

object RiskAnalyzer {

    private val suspicionPatterns = listOf(
        SuspicionPattern(
            name = "Utility Overreach",
            description = "Utility apps rarely need sensitive sensors",
            checkFn = { app ->
                app.category == AppCategory.UTILITY &&
                        (app.hasPermission("RECORD_AUDIO") || app.hasPermission("CAMERA"))
            },
            severityMultiplier = 2.5f
        ),

        SuspicionPattern(
            name = "Gaming Location Tracker",
            description = "Most games don't need precise location",
            checkFn = { app ->
                app.category == AppCategory.GAMING &&
                        app.hasPermission("ACCESS_FINE_LOCATION") &&
                        !app.appName.lowercase().contains("pokemon") &&
                        !app.appName.lowercase().contains("ingress") &&
                        !app.appName.lowercase().contains("go")
            },
            severityMultiplier = 1.8f
        ),

        SuspicionPattern(
            name = "Background Audio Monitoring",
            description = "App runs services that could record in background",
            checkFn = { app ->
                app.hasPermission("RECORD_AUDIO") &&
                        app.services.any { it.contains("background", ignoreCase = true) } &&
                        app.category !in setOf(AppCategory.COMMUNICATION, AppCategory.MEDIA)
            },
            severityMultiplier = 3.0f
        ),

        SuspicionPattern(
            name = "Unexpected Contact Access",
            description = "App category typically doesn't require contacts",
            checkFn = { app ->
                app.hasPermission("READ_CONTACTS") &&
                        app.category !in setOf(
                    AppCategory.COMMUNICATION,
                    AppCategory.SOCIAL_MEDIA,
                    AppCategory.PRODUCTIVITY
                )
            },
            severityMultiplier = 2.0f
        ),

        SuspicionPattern(
            name = "Permission Hoarding",
            description = "App requests many unrelated sensitive permissions",
            checkFn = { app ->
                val sensitiveCounts = listOf(
                    app.hasPermission("RECORD_AUDIO"),
                    app.hasPermission("CAMERA"),
                    app.hasPermission("ACCESS_FINE_LOCATION"),
                    app.hasPermission("READ_CONTACTS")
                ).count { it }
                sensitiveCounts >= 3
            },
            severityMultiplier = 2.2f
        ),

        SuspicionPattern(
            name = "Flashlight Camera Access",
            description = "Flashlight apps don't need full camera permission",
            checkFn = { app ->
                app.appName.lowercase().contains("flashlight") &&
                        app.hasPermission("CAMERA")
            },
            severityMultiplier = 2.8f
        ),

        SuspicionPattern(
            name = "Calculator Location Tracking",
            description = "Simple utility apps don't need location access",
            checkFn = { app ->
                (app.appName.lowercase().contains("calculator") ||
                        app.appName.lowercase().contains("cleaner")) &&
                        app.hasPermission("ACCESS_FINE_LOCATION")
            },
            severityMultiplier = 2.5f
        ),

        SuspicionPattern(
            name = "Unexpected Audio Recording",
            description = "App type doesn't typically need audio recording",
            checkFn = { app ->
                app.hasPermission("RECORD_AUDIO") &&
                        app.category in setOf(
                    AppCategory.UTILITY,
                    AppCategory.FINANCE,
                    AppCategory.SHOPPING,
                    AppCategory.HEALTH_FITNESS
                )
            },
            severityMultiplier = 2.3f
        )
    )

    fun analyze(app: AppInfo): AppAnalysis {
        val issues = detectIssues(app)
        val riskScore = calculateRiskScore(app, issues)
        val summary = generateSummary(app, issues)

        return AppAnalysis(
            app = app,
            issues = issues,
            riskScore = riskScore,
            humanReadableSummary = summary
        )
    }

    private fun detectIssues(app: AppInfo): List<DetectedIssue> {
        val issues = mutableListOf<DetectedIssue>()

        for (pattern in suspicionPatterns) {
            if (pattern.checkFn(app)) {
                issues.add(
                    DetectedIssue(
                        pattern = pattern,
                        severity = calculateSeverity(pattern, app),
                        explanation = generateExplanation(pattern, app)
                    )
                )
            }
        }

        return issues
    }

    private fun calculateSeverity(pattern: SuspicionPattern, app: AppInfo): Severity {
        val baseScore = pattern.severityMultiplier
        val adjustedScore = when {
            app.isSystemApp -> baseScore * 0.3f
            app.category == AppCategory.UNKNOWN -> baseScore * 1.3f
            else -> baseScore
        }

        return when {
            adjustedScore >= 2.5f -> Severity.HIGH
            adjustedScore >= 1.5f -> Severity.MEDIUM
            else -> Severity.LOW
        }
    }

    private fun calculateRiskScore(app: AppInfo, issues: List<DetectedIssue>): Float {
        var score = issues.sumOf { issue ->
            when (issue.severity) {
                Severity.HIGH -> 3.0
                Severity.MEDIUM -> 1.5
                Severity.LOW -> 0.5
            }
        }.toFloat()

        if (app.category == AppCategory.UNKNOWN && issues.isNotEmpty()) {
            score += 1.0f
        }

        return score
    }

    private fun generateExplanation(pattern: SuspicionPattern, app: AppInfo): String {
        val name = app.appName
        val cat = app.category.name.lowercase().replace('_', ' ')

        return when (pattern.name) {
            "Utility Overreach" -> when {
                app.hasPermission("RECORD_AUDIO") && app.hasPermission("CAMERA") ->
                    "Has microphone and camera access. Unusual for ${cat} apps—could capture audio/video without notice."
                app.hasPermission("RECORD_AUDIO") ->
                    "Can record audio. ${cat.capitalize()} apps rarely need microphone access for core features."
                else ->
                    "Has camera access. Could take photos/videos without your knowledge."
            }

            "Gaming Location Tracker" ->
                "Tracks precise GPS location. Non-AR games don't need this—may profile your movements for ads."

            "Background Audio Monitoring" ->
                "Microphone + background service = potential 24/7 listening. High risk for ${cat} apps."

            "Unexpected Contact Access" ->
                "${cat.capitalize()} apps don't typically need contacts. May build social graph for ad targeting."

            "Permission Hoarding" -> {
                val perms = buildList {
                    if (app.hasPermission("RECORD_AUDIO")) add("mic")
                    if (app.hasPermission("CAMERA")) add("camera")
                    if (app.hasPermission("ACCESS_FINE_LOCATION")) add("location")
                    if (app.hasPermission("READ_CONTACTS")) add("contacts")
                }
                "Has ${perms.joinToString(", ")} access—more than needed for stated purpose."
            }

            "Flashlight Camera Access" ->
                "Flashlight only needs flash LED control, not full camera. Could secretly take photos."

            "Calculator Location Tracking" ->
                "Calculators/cleaners don't need to know where you are. Tracks daily routines and work location."

            "Unexpected Audio Recording" ->
                "${cat.capitalize()} apps rarely need audio. Could eavesdrop on conversations for voice data."

            else -> pattern.description
        }
    }

    private fun generateSummary(app: AppInfo, issues: List<DetectedIssue>): String {
        if (issues.isEmpty()) {
            return "✓ No suspicious permissions detected"
        }

        val high = issues.count { it.severity == Severity.HIGH }
        val med = issues.count { it.severity == Severity.MEDIUM }

        return when {
            high > 0 -> "⚠️ $high critical issue${if (high > 1) "s" else ""}"
            med > 0 -> "⚠️ $med moderate concern${if (med > 1) "s" else ""}"
            else -> "⚠️ ${issues.size} minor concern${if (issues.size > 1) "s" else ""}"
        }
    }
}