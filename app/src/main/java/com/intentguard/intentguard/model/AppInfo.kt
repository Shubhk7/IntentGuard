package com.intentguard.app.model

data class AppInfo(
    val packageName: String,
    val appName: String,
    val permissions: Set<String>,
    val services: List<String>,
    val isSystemApp: Boolean,
    val installTime: Long,
    val versionName: String?
) {
    fun hasPermission(permission: String): Boolean {
        return permissions.any { it.endsWith(permission) }
    }

    val category: AppCategory by lazy {
        categorizeApp(packageName, appName, permissions)
    }
}

enum class AppCategory {
    COMMUNICATION,      // Messaging, email, phone
    SOCIAL_MEDIA,       // Social networks, dating
    PRODUCTIVITY,       // Office, notes, calendar
    MEDIA,              // Photo, video, music players
    NAVIGATION,         // Maps, GPS, travel
    GAMING,             // Games
    UTILITY,            // Flashlight, calculator, file managers
    HEALTH_FITNESS,     // Health tracking, workout
    FINANCE,            // Banking, payments
    SHOPPING,           // E-commerce
    SYSTEM,             // Launchers, keyboards
    UNKNOWN             // Cannot categorize
}

fun categorizeApp(packageName: String, appName: String, permissions: Set<String>): AppCategory {
    val lowerName = appName.lowercase()
    val lowerPackage = packageName.lowercase()

    return when {
        // Communication
        lowerPackage.contains("phone") || lowerPackage.contains("dialer") ||
                lowerPackage.contains("sms") || lowerPackage.contains("message") ||
                lowerName.contains("messenger") || lowerName.contains("whatsapp") ||
                lowerName.contains("telegram") || lowerName.contains("signal") ->
            AppCategory.COMMUNICATION

        // Social Media
        lowerPackage.contains("instagram") || lowerPackage.contains("facebook") ||
                lowerPackage.contains("twitter") || lowerPackage.contains("tiktok") ||
                lowerPackage.contains("snapchat") || lowerName.contains("social") ->
            AppCategory.SOCIAL_MEDIA

        // Navigation
        lowerPackage.contains("maps") || lowerPackage.contains("navigation") ||
                lowerName.contains("uber") || lowerName.contains("lyft") ||
                lowerName.contains("waze") || lowerName.contains("gps") ->
            AppCategory.NAVIGATION

        // Media & Camera
        lowerName.contains("camera") || lowerName.contains("photo") ||
                lowerName.contains("gallery") || lowerPackage.contains("camera") ||
                lowerName.contains("video") && !lowerName.contains("game") ->
            AppCategory.MEDIA

        // Gaming
        lowerPackage.contains("game") || lowerName.contains("game") ||
                lowerPackage.contains("unity") || lowerPackage.contains("unreal") ->
            AppCategory.GAMING

        // Utility
        lowerName.contains("flashlight") || lowerName.contains("calculator") ||
                lowerName.contains("cleaner") || lowerName.contains("battery") ||
                lowerName.contains("file") && lowerName.contains("manager") ->
            AppCategory.UTILITY

        // Health & Fitness
        lowerName.contains("fitness") || lowerName.contains("health") ||
                lowerName.contains("workout") || lowerName.contains("step") ->
            AppCategory.HEALTH_FITNESS

        // Finance
        lowerName.contains("bank") || lowerName.contains("payment") ||
                lowerName.contains("wallet") || lowerName.contains("finance") ->
            AppCategory.FINANCE

        // Shopping
        lowerName.contains("shop") || lowerName.contains("amazon") ||
                lowerName.contains("ebay") || lowerName.contains("store") ->
            AppCategory.SHOPPING

        // System
        lowerName.contains("launcher") || lowerName.contains("keyboard") ->
            AppCategory.SYSTEM

        else -> AppCategory.UNKNOWN
    }
}