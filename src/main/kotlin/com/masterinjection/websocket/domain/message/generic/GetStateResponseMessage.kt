package com.masterinjection.websocket.domain.message.generic

import com.masterinjection.websocket.domain.message.BaseResponseMessage
import com.masterinjection.websocket.domain.message.MessageType

class GetStateResponseMessage(
    timestamp: Long,
    responseTo: Long,
    val data: Data,
) : BaseResponseMessage(
    timestamp = timestamp,
    responseTo = responseTo,
    type = MessageType.STATE,
) {

    data class Data(
        val id: String,
        val name: String,
        val connected: Boolean,
    )
}