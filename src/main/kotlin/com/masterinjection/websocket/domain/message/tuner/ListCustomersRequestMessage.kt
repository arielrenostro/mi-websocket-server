package com.masterinjection.websocket.domain.message.tuner

import com.masterinjection.websocket.domain.message.BaseMessage
import com.masterinjection.websocket.domain.message.MessageType

class ListCustomersRequestMessage(
    timestamp: Long,
) : BaseMessage(
    timestamp = timestamp,
    type = MessageType.LIST_CUSTOMERS,
)
