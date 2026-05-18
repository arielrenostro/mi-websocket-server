package com.masterinjection.remotetuningserver.domain.message.generic

import com.masterinjection.remotetuningserver.domain.message.BaseMessage
import com.masterinjection.remotetuningserver.domain.message.MessageType

class PairDisconnectedMessage(
    timestamp: Long,
) : BaseMessage(
    timestamp = timestamp,
    type = MessageType.PAIR_DISCONNECTED,
)