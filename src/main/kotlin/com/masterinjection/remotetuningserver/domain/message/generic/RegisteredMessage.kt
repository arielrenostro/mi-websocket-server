package com.masterinjection.remotetuningserver.domain.message.generic

import com.masterinjection.remotetuningserver.domain.message.BaseMessage
import com.masterinjection.remotetuningserver.domain.message.MessageType

class RegisteredMessage(
    val id: String,
    val name: String,
    val secret: String,
    timestamp: Long,
) : BaseMessage(timestamp, MessageType.REGISTERED)
