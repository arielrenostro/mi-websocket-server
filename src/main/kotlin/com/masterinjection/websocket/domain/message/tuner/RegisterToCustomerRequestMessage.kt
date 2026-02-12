package com.masterinjection.websocket.domain.message.tuner

import com.masterinjection.websocket.domain.message.BaseMessage
import com.masterinjection.websocket.domain.message.MessageType

class RegisterToCustomerRequestMessage(
    timestamp: Long,
    val customerId: String,
) : BaseMessage(
    timestamp = timestamp,
    type = MessageType.REGISTER_TO_CUSTOMER,
)
