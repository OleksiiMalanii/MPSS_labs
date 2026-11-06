package com.smartparking.locator.cloud

enum class SyncStatus(val label: String) {
    Idle("Очікування"), Sending("Відправлення"), Ok("OK"), Failed("Помилка"), Pending("Pending"),
}

// State — усе, що видно на вкладці «Хмара».
data class CloudState(
    val status: SyncStatus = SyncStatus.Idle,
    val sent: Int = 0,
    val pending: Int = 0,
    val detail: String = "Ще нічого не відправлено",
    val lastPayload: String = "—",
    val auto: Boolean = true,
)

// Action — життєвий цикл від Activity та дії користувача.
sealed interface CloudAction {
    data object Start : CloudAction
    data object Stop : CloudAction
    data class SetAuto(val on: Boolean) : CloudAction
    data object SendNow : CloudAction
}

// Event — разові повідомлення (Toast).
sealed interface CloudEvent {
    data class ShowMessage(val text: String) : CloudEvent
}
