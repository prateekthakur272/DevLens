package dev.prateekthakur.devlens.domain.permission

/** PRD §15 — static classification of well-known Android permissions. */
object PermissionRiskDatabase {

    private data class Entry(val risk: PermissionRisk, val description: String)

    private val dangerous: Map<String, String> = mapOf(
        "android.permission.READ_CALENDAR" to "Read the user's calendar data.",
        "android.permission.WRITE_CALENDAR" to "Add, remove or change the user's calendar events.",
        "android.permission.CAMERA" to "Take pictures and record video.",
        "android.permission.READ_CONTACTS" to "Read the user's contacts data.",
        "android.permission.WRITE_CONTACTS" to "Write the user's contacts data.",
        "android.permission.GET_ACCOUNTS" to "Access the list of accounts known to the device.",
        "android.permission.ACCESS_FINE_LOCATION" to "Access precise device location.",
        "android.permission.ACCESS_COARSE_LOCATION" to "Access approximate device location.",
        "android.permission.ACCESS_BACKGROUND_LOCATION" to "Access location while the app runs in the background.",
        "android.permission.RECORD_AUDIO" to "Record audio via the microphone.",
        "android.permission.READ_PHONE_STATE" to "Read phone state, including the phone number and device identity.",
        "android.permission.READ_PHONE_NUMBERS" to "Read the device's phone number(s).",
        "android.permission.CALL_PHONE" to "Initiate a phone call without going through the dialer.",
        "android.permission.ANSWER_PHONE_CALLS" to "Answer an incoming phone call.",
        "android.permission.READ_CALL_LOG" to "Read the user's call log.",
        "android.permission.WRITE_CALL_LOG" to "Write to the user's call log.",
        "android.permission.ADD_VOICEMAIL" to "Add voicemails into the system.",
        "android.permission.USE_SIP" to "Make/receive SIP calls.",
        "android.permission.PROCESS_OUTGOING_CALLS" to "Monitor, redirect or abort outgoing calls.",
        "android.permission.BODY_SENSORS" to "Access data from body sensors such as heart-rate monitors.",
        "android.permission.BODY_SENSORS_BACKGROUND" to "Access body sensor data while running in the background.",
        "android.permission.ACTIVITY_RECOGNITION" to "Access the user's physical activity recognition data.",
        "android.permission.SEND_SMS" to "Send SMS messages.",
        "android.permission.RECEIVE_SMS" to "Receive SMS messages.",
        "android.permission.READ_SMS" to "Read SMS messages.",
        "android.permission.RECEIVE_WAP_PUSH" to "Receive WAP push messages.",
        "android.permission.RECEIVE_MMS" to "Receive MMS messages.",
        "android.permission.READ_EXTERNAL_STORAGE" to "Read from external/shared storage.",
        "android.permission.WRITE_EXTERNAL_STORAGE" to "Write to external/shared storage.",
        "android.permission.MANAGE_EXTERNAL_STORAGE" to "Broad access to manage all files on external storage.",
        "android.permission.READ_MEDIA_IMAGES" to "Read image files from shared storage.",
        "android.permission.READ_MEDIA_VIDEO" to "Read video files from shared storage.",
        "android.permission.READ_MEDIA_AUDIO" to "Read audio files from shared storage.",
        "android.permission.POST_NOTIFICATIONS" to "Post notifications to the user.",
        "android.permission.BLUETOOTH_CONNECT" to "Connect to already-paired Bluetooth devices.",
        "android.permission.BLUETOOTH_SCAN" to "Discover and pair nearby Bluetooth devices.",
        "android.permission.BLUETOOTH_ADVERTISE" to "Advertise this device to nearby Bluetooth devices.",
        "android.permission.NEARBY_WIFI_DEVICES" to "Discover and connect to nearby Wi-Fi devices.",
        "android.permission.UWB_RANGING" to "Range to nearby devices via Ultra-Wideband.",
    )

    private val special: Map<String, String> = mapOf(
        "android.permission.SYSTEM_ALERT_WINDOW" to "Draw on top of other apps.",
        "android.permission.WRITE_SETTINGS" to "Modify system settings.",
        "android.permission.REQUEST_INSTALL_PACKAGES" to "Request installation of other packages.",
        "android.permission.SCHEDULE_EXACT_ALARM" to "Schedule exact alarms that fire even in Doze.",
        "android.permission.MANAGE_OWN_CALLS" to "Manage the app's own self-managed calls.",
        "android.permission.QUERY_ALL_PACKAGES" to "See the list of all installed apps on the device.",
    )

    private val signature: Map<String, String> = mapOf(
        "android.permission.BIND_ACCESSIBILITY_SERVICE" to "Bind to an accessibility service (system-level).",
        "android.permission.BIND_DEVICE_ADMIN" to "Bind to a device admin receiver (system-level).",
        "android.permission.BIND_NOTIFICATION_LISTENER_SERVICE" to "Bind to a notification listener (system-level).",
        "android.permission.BIND_VPN_SERVICE" to "Bind to a VPN service (system-level).",
        "android.permission.BIND_JOB_SERVICE" to "Bind to a JobScheduler job service (system-level).",
        "android.permission.WRITE_SECURE_SETTINGS" to "Modify secure system settings (privileged).",
    )

    private val normal: Map<String, String> = mapOf(
        "android.permission.INTERNET" to "Open network sockets.",
        "android.permission.ACCESS_NETWORK_STATE" to "View network connectivity state.",
        "android.permission.ACCESS_WIFI_STATE" to "View Wi-Fi connectivity state.",
        "android.permission.VIBRATE" to "Control the vibrator.",
        "android.permission.WAKE_LOCK" to "Prevent the processor from sleeping.",
        "android.permission.RECEIVE_BOOT_COMPLETED" to "Run automatically after the device finishes booting.",
        "android.permission.FOREGROUND_SERVICE" to "Run a foreground service.",
        "android.permission.CHANGE_WIFI_STATE" to "Change Wi-Fi connectivity state.",
        "android.permission.BLUETOOTH" to "Connect to previously paired Bluetooth devices.",
        "android.permission.NFC" to "Access NFC hardware.",
        "android.permission.EXPAND_STATUS_BAR" to "Expand or collapse the status bar.",
        "android.permission.CHECK_LICENSE" to "Check licensing status against the Play Store.",
        "android.permission.BILLING" to "Perform in-app billing operations (com.android.vending.BILLING).",
    )

    fun classify(permissionName: String): Pair<PermissionRisk, String> = when {
        dangerous.containsKey(permissionName) -> PermissionRisk.DANGEROUS to dangerous.getValue(permissionName)
        special.containsKey(permissionName) -> PermissionRisk.SPECIAL to special.getValue(permissionName)
        signature.containsKey(permissionName) -> PermissionRisk.SIGNATURE to signature.getValue(permissionName)
        normal.containsKey(permissionName) -> PermissionRisk.NORMAL to normal.getValue(permissionName)
        permissionName.startsWith("android.permission.") -> PermissionRisk.NORMAL to "Standard Android platform permission."
        else -> PermissionRisk.UNKNOWN to "Custom or third-party permission not declared by the Android platform. Verify its purpose with the declaring app/library."
    }
}
