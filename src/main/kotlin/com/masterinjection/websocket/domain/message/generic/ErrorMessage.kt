package com.masterinjection.websocket.domain.message.generic

import com.masterinjection.websocket.domain.message.BaseMessage
import com.masterinjection.websocket.domain.message.MessageType

class ErrorMessage(
    timestamp: Long,
    responseTo: Long?,
    val message: String,
) : BaseMessage(timestamp, MessageType.ERROR)