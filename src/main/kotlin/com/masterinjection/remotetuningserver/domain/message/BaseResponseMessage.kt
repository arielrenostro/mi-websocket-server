package com.masterinjection.remotetuningserver.domain.message

abstract class BaseResponseMessage(
    /**
     * Unix timestamp
     */
    timestamp: Long,
    /**
     * Message Type
     */
    type: MessageType,
    /**
     * Unix timestamp of request message
     */
    val responseTo: Long,
) : BaseMessage(
    timestamp = timestamp,
    type = type,
)