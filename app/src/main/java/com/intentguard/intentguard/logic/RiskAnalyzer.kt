package com.intentguard.app.logic

import com.intentguard.app.model.*

object RiskAnalyzer {

    private val legitimateUseCases = mapOf(
        // Communication apps - video/voice calls, screen sharing
        "whatsapp" to setOf("CAMERA", "RECORD_AUDIO", "READ_CONTACTS"),
        "telegram" to setOf("CAMERA", "RECORD_AUDIO", "READ_CONTACTS"),
        "messenger" to setOf("CAMERA", "RECORD_AUDIO", "READ_CONTACTS"),
        "discord" to setOf("CAMERA", "RECORD_AUDIO", "READ_CONTACTS", "ACCESS_FINE_LOCATION"),
        "slack" to setOf("CAMERA", "RECORD_AUDIO", "READ_CONTACTS"),
        "teams" to setOf("CAMERA", "RECORD_AUDIO", "READ_CONTACTS"),
        "zoom" to setOf("CAMERA", "RECORD_AUDIO", "READ_CONTACTS"),
        "skype" to setOf("CAMERA", "RECORD_AUDIO", "READ_CONTACTS"),
        "signal" to setOf("CAMERA", "RECORD_AUDIO", "READ_CONTACTS"),

        // Payment apps - QR scanning, OTP, UPI contacts
        "payment" to setOf("CAMERA", "READ_CONTACTS", "READ_SMS"),
        "bank" to setOf("CAMERA", "READ_CONTACTS", "READ_SMS", "CALL_PHONE"),
        "pay" to setOf("CAMERA", "READ_CONTACTS", "READ_SMS"),
        "upi" to setOf("CAMERA", "READ_CONTACTS", "READ_SMS"),
        "phonepe" to setOf("CAMERA", "READ_CONTACTS", "READ_SMS"),
        "paytm" to setOf("CAMERA", "READ_CONTACTS", "READ_SMS"),
        "bhim" to setOf("CAMERA", "READ_CONTACTS", "READ_SMS"),

        // Telecom apps - manage SIM, calls, messages
        "airtel" to setOf("CAMERA", "READ_CONTACTS", "READ_SMS", "CALL_PHONE", "READ_PHONE_STATE"),
        "jio" to setOf("CAMERA", "READ_CONTACTS", "READ_SMS", "CALL_PHONE", "READ_PHONE_STATE"),
        "vodafone" to setOf("CAMERA", "READ_CONTACTS", "READ_SMS", "CALL_PHONE", "READ_PHONE_STATE"),
        "vi" to setOf("CAMERA", "READ_CONTACTS", "READ_SMS", "CALL_PHONE"),

        // Social media - content creation
        "instagram" to setOf("CAMERA", "RECORD_AUDIO", "READ_CONTACTS", "ACCESS_FINE_LOCATION"),
        "facebook" to setOf("CAMERA", "RECORD_AUDIO", "READ_CONTACTS", "ACCESS_FINE_LOCATION"),
        "snapchat" to setOf("CAMERA", "RECORD_AUDIO", "READ_CONTACTS", "ACCESS_FINE_LOCATION"),
        "tiktok" to setOf("CAMERA", "RECORD_AUDIO", "READ_CONTACTS", "ACCESS_FINE_LOCATION"),
        "twitter" to setOf("CAMERA", "RECORD_AUDIO", "READ_CONTACTS", "ACCESS_FINE_LOCATION"),
        "linkedin" to setOf("CAMERA", "RECORD_AUDIO", "READ_CONTACTS"),
        "youtube" to setOf("CAMERA", "RECORD_AUDIO", "ACCESS_FINE_LOCATION"),

        // Delivery / ride apps
        "zomato" to setOf("ACCESS_FINE_LOCATION", "CAMERA", "CALL_PHONE"),
        "swiggy" to setOf("ACCESS_FINE_LOCATION", "CAMERA", "CALL_PHONE"),
        "uber" to setOf("ACCESS_FINE_LOCATION", "CAMERA", "CALL_PHONE", "READ_CONTACTS"),
        "ola" to setOf("ACCESS_FINE_LOCATION", "CAMERA", "READ_CONTACTS", "CALL_PHONE"),
        "blinkit" to setOf("ACCESS_FINE_LOCATION", "CAMERA"),
        "dunzo" to setOf("ACCESS_FINE_LOCATION", "CAMERA", "CALL_PHONE"),

        // Caller ID apps
        "truecaller" to setOf("READ_CONTACTS", "CALL_PHONE", "READ_PHONE_STATE", "READ_SMS"),

        // Voice assistants
        "alexa" to setOf("RECORD_AUDIO", "CAMERA", "ACCESS_FINE_LOCATION"),
        "google" to setOf("RECORD_AUDIO", "CAMERA", "ACCESS_FINE_LOCATION", "READ_CONTACTS"),
        "assistant" to setOf("RECORD_AUDIO", "CAMERA", "ACCESS_FINE_LOCATION"),

        // Microsoft ecosystem apps
        "microsoft" to setOf("CAMERA", "READ_CONTACTS", "READ_SMS", "CALL_PHONE"),
        "windows" to setOf("CAMERA", "READ_CONTACTS", "READ_SMS", "CALL_PHONE"),
        "outlook" to setOf("CAMERA", "READ_CONTACTS", "CALL_PHONE"),
        "m365 copilot" to setOf("RECORD_AUDIO", "READ_CONTACTS"),
        "seeing ai" to setOf("CAMERA", "RECORD_AUDIO"),
        "authenticator" to setOf("CAMERA"),
        "link to windows" to setOf("READ_CONTACTS", "READ_SMS", "CALL_PHONE"),

        // E-commerce
        "amazon" to setOf("CAMERA", "ACCESS_FINE_LOCATION", "RECORD_AUDIO"),
        "flipkart" to setOf("CAMERA", "ACCESS_FINE_LOCATION"),
        "myntra" to setOf("CAMERA", "ACCESS_FINE_LOCATION"),

        // Emergency/safety apps
        "112" to setOf("ACCESS_FINE_LOCATION", "CALL_PHONE", "CAMERA", "RECORD_AUDIO"),
        "emergency" to setOf("ACCESS_FINE_LOCATION", "CALL_PHONE", "CAMERA")
    )

    private fun hasLegitimateNeed(app: AppInfo, permission: String): Boolean {
        val appNameLower = app.appName.lowercase()
        val packageLower = app.packageName.lowercase()

        for ((keyword, allowedPerms) in legitimateUseCases) {
            if (appNameLower.contains(keyword) || packageLower.contains(keyword)) {
                if (allowedPerms.any { permission.endsWith(it) }) {
                    return true
                }
            }
        }

        return when (app.category) {
            AppCategory.COMMUNICATION -> permission.endsWith("CAMERA") ||
                    permission.endsWith("RECORD_AUDIO") ||
                    permission.endsWith("READ_CONTACTS")

            AppCategory.SOCIAL_MEDIA -> permission.endsWith("CAMERA") ||
                    permission.endsWith("RECORD_AUDIO") ||
                    permission.endsWith("READ_CONTACTS") ||
                    permission.endsWith("ACCESS_FINE_LOCATION")

            AppCategory.NAVIGATION -> permission.endsWith("ACCESS_FINE_LOCATION") ||
                    permission.endsWith("CAMERA") ||
                    permission.endsWith("CALL_PHONE")

            AppCategory.MEDIA -> permission.endsWith("CAMERA") ||
                    permission.endsWith("RECORD_AUDIO")

            AppCategory.FINANCE -> permission.endsWith("CAMERA") ||
                    permission.endsWith("READ_SMS") ||
                    permission.endsWith("READ_CONTACTS")

            AppCategory.SHOPPING -> permission.endsWith("CAMERA") ||
                    permission.endsWith("ACCESS_FINE_LOCATION")

            else -> false
        }
    }

    private val suspicionPatterns = listOf(
        SuspicionPattern(
            name = "Utility Overreach",
            description = "Utility apps rarely need sensitive sensors",
            checkFn = { app ->
                app.category == AppCategory.UTILITY &&
                        ((app.hasPermission("RECORD_AUDIO") && !hasLegitimateNeed(app, "RECORD_AUDIO")) ||
                                (app.hasPermission("CAMERA") && !hasLegitimateNeed(app, "CAMERA")))
            },
            severityMultiplier = 2.8f
        ),

        SuspicionPattern(
            name = "Gaming Location Tracker",
            description = "Most games don't need precise location",
            checkFn = { app ->
                app.category == AppCategory.GAMING &&
                        app.hasPermission("ACCESS_FINE_LOCATION") &&
                        !app.appName.lowercase().let { name ->
                            name.contains("pokemon") || name.contains("ingress") ||
                                    name.contains(" go") || name.contains("multiplayer") ||
                                    name.contains("online") || name.contains("world") ||
                                    name.contains("royale") || name.contains("battle")
                        }
            },
            severityMultiplier = 2.0f
        ),

        SuspicionPattern(
            name = "Background Audio Monitoring",
            description = "App runs services that could record in background",
            checkFn = { app ->
                app.hasPermission("RECORD_AUDIO") &&
                        app.services.any { it.contains("background", ignoreCase = true) } &&
                        !hasLegitimateNeed(app, "RECORD_AUDIO") &&
                        app.category !in setOf(AppCategory.COMMUNICATION, AppCategory.MEDIA)
            },
            severityMultiplier = 3.5f
        ),

        SuspicionPattern(
            name = "Unexpected Contact Access",
            description = "App doesn't need contacts for its purpose",
            checkFn = { app ->
                app.hasPermission("READ_CONTACTS") &&
                        !hasLegitimateNeed(app, "READ_CONTACTS") &&
                        app.category !in setOf(
                    AppCategory.COMMUNICATION,
                    AppCategory.SOCIAL_MEDIA,
                    AppCategory.PRODUCTIVITY
                )
            },
            severityMultiplier = 2.2f
        ),

        SuspicionPattern(
            name = "Excessive Permissions",
            description = "App has many permissions without clear justification",
            checkFn = { app ->
                val suspiciousPerms = listOf(
                    "RECORD_AUDIO" to !hasLegitimateNeed(app, "RECORD_AUDIO"),
                    "CAMERA" to !hasLegitimateNeed(app, "CAMERA"),
                    "ACCESS_FINE_LOCATION" to !hasLegitimateNeed(app, "ACCESS_FINE_LOCATION"),
                    "READ_CONTACTS" to !hasLegitimateNeed(app, "READ_CONTACTS"),
                    "READ_SMS" to !hasLegitimateNeed(app, "READ_SMS"),
                    "CALL_PHONE" to !hasLegitimateNeed(app, "CALL_PHONE")
                ).filter { (perm, isSuspicious) ->
                    app.hasPermission(perm) && isSuspicious
                }.size

                suspiciousPerms >= 3
            },
            severityMultiplier = 2.5f
        ),

        SuspicionPattern(
            name = "Flashlight Camera Abuse",
            description = "Flashlight only needs flash LED, not full camera",
            checkFn = { app ->
                app.appName.lowercase().let { name ->
                    name.contains("flashlight") || name.contains("torch")
                } &&
                        app.hasPermission("CAMERA") &&
                        !app.appName.lowercase().contains("camera")
            },
            severityMultiplier = 3.0f
        ),

        SuspicionPattern(
            name = "Utility Location Tracking",
            description = "Simple utilities don't need location",
            checkFn = { app ->
                app.appName.lowercase().let { name ->
                    (name.contains("calculator") ||
                            name.contains("cleaner") ||
                            name.contains("battery") ||
                            name.contains("booster")) &&
                            !name.contains("map") &&
                            !name.contains("travel")
                } &&
                        app.hasPermission("ACCESS_FINE_LOCATION")
            },
            severityMultiplier = 2.8f
        ),

        SuspicionPattern(
            name = "Unjustified Audio Access",
            description = "App has microphone without clear need",
            checkFn = { app ->
                app.hasPermission("RECORD_AUDIO") &&
                        !hasLegitimateNeed(app, "RECORD_AUDIO") &&
                        app.category in setOf(
                    AppCategory.UTILITY,
                    AppCategory.HEALTH_FITNESS,
                    AppCategory.UNKNOWN
                )
            },
            severityMultiplier = 2.4f
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
            app.isSystemApp -> baseScore * 0.15f
            app.category == AppCategory.UNKNOWN -> baseScore * 1.5f
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
                Severity.HIGH -> 3.5
                Severity.MEDIUM -> 1.8
                Severity.LOW -> 0.6
            }
        }.toFloat()

        if (app.category == AppCategory.UNKNOWN && issues.isNotEmpty()) {
            score += 1.5f
        }

        return score
    }

    private fun generateExplanation(pattern: SuspicionPattern, app: AppInfo): String {
        return when (pattern.name) {
            "Utility Overreach" -> when {
                app.hasPermission("RECORD_AUDIO") && app.hasPermission("CAMERA") ->
                    "Has mic + camera without clear business need. Could spy via audio/video."
                app.hasPermission("RECORD_AUDIO") ->
                    "Microphone access unjustified for this utility type."
                else ->
                    "Camera access without legitimate reason. Risk of hidden photo capture."
            }

            "Gaming Location Tracker" ->
                "Tracks GPS in non-location game. Used for ad targeting and behavior profiling."

            "Background Audio Monitoring" ->
                "Mic + background service combo. High risk of 24/7 eavesdropping."

            "Unexpected Contact Access" ->
                "Contact list access serves no clear purpose. May harvest data for ad networks."

            "Excessive Permissions" ->
                "Multiple unjustified permissions suggest aggressive data collection strategy."

            "Flashlight Camera Abuse" ->
                "Known scam pattern: flashlight apps only need flash LED control, not full camera API."

            "Utility Location Tracking" ->
                "Simple utilities never need GPS. Classic privacy violation indicator."

            "Unjustified Audio Access" ->
                "Microphone access without legitimate voice feature. Potential conversation monitoring."

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