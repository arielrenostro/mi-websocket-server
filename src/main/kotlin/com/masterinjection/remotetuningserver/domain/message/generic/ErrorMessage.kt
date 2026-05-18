package com.masterinjection.remotetuningserver.domain.message.generic

import com.masterinjection.remotetuningserver.domain.message.BaseMessage
import com.masterinjection.remotetuningserver.domain.message.MessageType

class ErrorMessage(
    timestamp: Long,
    responseTo: Long?,
    val message: String,
) : BaseMessage(timestamp, MessageType.ERROR)