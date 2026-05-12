package com.masterinjection.websocket.domain

import org.springframework.web.socket.WebSocketSession

class Customer(
    val id: String,
    val name: String,
    val secret: String,
    var wsSession: WebSocketSession? = null,
    var tunerConnected: Tuner? = null,
)