package com.masterinjection.websocket.domain.message.customer

import com.masterinjection.websocket.domain.message.BaseMessage
import com.masterinjection.websocket.domain.message.MessageType

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