package com.masterinjection.websocket.domain.message.generic

import com.masterinjection.websocket.domain.message.BaseMessage
import com.masterinjection.websocket.domain.message.MessageType

class RegisteredMessage(
    val id: String,
    val name: String,
    val secret: String,
    timestamp: Long,
) : BaseMessage(timestamp, MessageType.REGISTERED)
