package com.masterinjection.websocket.domain.message.customer

import com.masterinjection.websocket.domain.message.BaseResponseMessage
import com.masterinjection.websocket.domain.message.MessageType

class RegisterTunerResponseMessage(
    timestamp: Long,
    responseTo: Long,
    val success: Boolean,
) : BaseResponseMessage(
    timestamp = timestamp,
    responseTo = responseTo,
    type = MessageType.REGISTER_TUNER_RESPONSE,
)