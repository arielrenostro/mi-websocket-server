package com.masterinjection.remotetuningserver.domain

import org.springframework.web.socket.WebSocketSession
import java.time.Instant

class Tuner(
    val id: String,
    val name: String,
    val secret: String,
    var wsSession: WebSocketSession? = null,
    var customerConnected: Customer? = null,
    var status: ClientStatus = ClientStatus.CONNECTED,
    var disconnectedAt: Instant? = null,
)