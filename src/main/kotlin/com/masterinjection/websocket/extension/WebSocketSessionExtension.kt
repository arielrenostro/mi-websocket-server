package com.masterinjection.websocket.extension

import com.masterinjection.websocket.domain.message.BaseMessage
import com.masterinjection.websocket.domain.message.generic.ErrorMessage
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession

fun WebSocketSession.sendJsonMessage(message: BaseMessage) {
    val mapper = GlobalMapper.getInstance()
    val json = mapper.writeValueAsString(message)
    sendMessage(TextMessage(json))
}

fun WebSocketSession.sendJsonError(responseTo: Long?, errorMessage: String?) {
    sendJsonMessage(
        ErrorMessage(
            timestamp = System.currentTimeMillis(),
            responseTo = responseTo,
            message = errorMessage ?: "Unknown error. Check server log.",
        )
    )
}