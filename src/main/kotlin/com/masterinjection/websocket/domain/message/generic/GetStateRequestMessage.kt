package com.masterinjection.websocket.domain.message.generic

import com.masterinjection.websocket.domain.message.BaseMessage
import com.masterinjection.websocket.domain.message.MessageType

class GetStateRequestMessage(
    timestamp: Long,
) : BaseMessage(
    timestamp = timestamp,
    type = MessageType.GET_STATE,
)
