package com.masterinjection.remotetuningserver.domain.message.generic

import com.masterinjection.remotetuningserver.domain.message.BaseMessage
import com.masterinjection.remotetuningserver.domain.message.MessageType

class EchoSerialDataMessage(
    timestamp: Long,
    val data: String,
) : BaseMessage(
    timestamp = timestamp,
    type = MessageType.ECHO_SERIAL_DATA
)
