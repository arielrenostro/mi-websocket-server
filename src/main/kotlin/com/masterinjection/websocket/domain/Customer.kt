package com.masterinjection.websocket.domain

import org.springframework.web.socket.WebSocketSession

data class Customer(
    val id: String,
    val name: String,
    val session: WebSocketSession,
    var tunerConnected: Tuner? = null,
)