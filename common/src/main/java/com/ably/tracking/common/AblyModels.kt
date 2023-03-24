package com.ably.tracking.common

import com.ably.tracking.Resolution

/**
 * Encapsulates the properties of an Ably presence message which are needed by asset tracking SDKs.
 */
data class PresenceMessage(
    val action: PresenceAction,
    val data: PresenceData,

    val timestamp: Long,

    /**
     * Combination of Ably `clientId` and `connectionId`.
     * See: https://sdk.ably.com/builds/ably/specification/main/features/#TP3h
     */
    val memberKey: String,
    val clientId: String,
    val connectionId: String,

    val id: String
) {
    /**
     * Given a message to compare to, deduces whether this message is "newer".
     *
     * Ably spec point: RTP2b
     */
    fun isNewerThan(compare: PresenceMessage): Boolean {

        // RTP2b1
        if (this.isSynthesizedLeave() || compare.isSynthesizedLeave()) {
            return this.timestamp > compare.timestamp
        }

        // RTP2b2
        val thisIdSplit = splitId()
        val compareIdSplit = compare.splitId()

        // Check valid connection id
        if (thisIdSplit.size != 3 || compareIdSplit.size != 3) {
            return false
        }

        val thisMessageSerial = thisIdSplit[1].toIntOrNull()
        val thisIndex = thisIdSplit[2].toIntOrNull()
        val compareMessageSerial = compareIdSplit[1].toIntOrNull()
        val compareMessageIndex = compareIdSplit[2].toIntOrNull()

        return thisMessageSerial != null && thisIndex != null && compareMessageSerial != null && compareMessageIndex != null &&
            ((thisMessageSerial > compareMessageSerial) || (thisMessageSerial == compareMessageSerial && thisIndex > compareMessageIndex))
    }

    private fun isSynthesizedLeave(): Boolean = !this.id.startsWith(this.connectionId)

    private fun splitId(): List<String> = this.id.split(':', limit = 3)
}

enum class PresenceAction {
    PRESENT_OR_ENTER, LEAVE_OR_ABSENT, UPDATE;
}

data class PresenceData(val type: String, val resolution: Resolution? = null, val rawLocations: Boolean? = null)
