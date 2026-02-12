package com.masterinjection.websocket.domain.message.tuner

import com.masterinjection.websocket.domain.message.BaseResponseMessage
import com.masterinjection.websocket.domain.message.MessageType

class RegisterToCustomerResponseMessage(
    timestamp: Long,
    responseTo: Long,
    val success: Boolean,
) : BaseResponseMessage(
    timestamp = timestamp,
    responseTo = responseTo,
    type = MessageType.REGISTER_TO_CUSTOMER_RESPONSE,
)
