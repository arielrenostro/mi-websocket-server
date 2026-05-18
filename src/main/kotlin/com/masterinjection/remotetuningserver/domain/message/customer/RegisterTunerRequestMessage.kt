package com.masterinjection.remotetuningserver.domain.message.customer

import com.masterinjection.remotetuningserver.domain.message.BaseMessage
import com.masterinjection.remotetuningserver.domain.message.MessageType

class RegisterTunerRequestMessage(
    timestamp: Long,
    val tuner: Tuner,
) : BaseMessage(
    timestamp = timestamp,
    type = MessageType.REGISTER_TUNER,
) {

    data class Tuner(
        val id: String,
        val name: String,
    )
}