package com.masterinjection.websocket.domain.message.generic

import com.masterinjection.websocket.domain.message.BaseMessage
import com.masterinjection.websocket.domain.message.MessageType

class EchoSerialDataMessage(
    timestamp: Long,
    val data: String,
) : BaseMessage(
    timestamp = timestamp,
    type = MessageType.ECHO_SERIAL_DATA
)
