package com.masterinjection.remotetuningserver.domain.message.generic

import com.masterinjection.remotetuningserver.domain.message.BaseMessage
import com.masterinjection.remotetuningserver.domain.message.MessageType

class PairConnectedMessage(
    timestamp: Long,
    val peer: Peer,
) : BaseMessage(timestamp, MessageType.PAIR_CONNECTED) {

    data class Peer(val id: String, val name: String)
}
