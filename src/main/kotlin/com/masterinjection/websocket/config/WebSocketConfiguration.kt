package com.masterinjection.websocket.config

import com.masterinjection.websocket.controller.ws.CustomerWebSocket
import com.masterinjection.websocket.controller.ws.TunerWebSocket
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.context.annotation.Configuration
import org.springframework.http.server.ServerHttpRequest
import org.springframework.http.server.ServerHttpResponse
import org.springframework.web.servlet.HandlerInterceptor
import org.springframework.web.socket.WebSocketHandler
import org.springframework.web.socket.config.annotation.EnableWebSocket
import org.springframework.web.socket.config.annotation.WebSocketConfigurer
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry
import org.springframework.web.socket.server.HandshakeInterceptor
import org.springframework.web.util.UriComponentsBuilder
import java.lang.Exception

@Configuration
@EnableWebSocket
class WebSocketConfiguration(
    private val tunerWebSocket: TunerWebSocket,
    private val customerWebSocket: CustomerWebSocket,
) : WebSocketConfigurer {

    override fun registerWebSocketHandlers(registry: WebSocketHandlerRegistry) {
        registry.addHandler(tunerWebSocket, "/ws/tuner")
            .setAllowedOrigins("*")
            .addInterceptors(Interceptor())

        registry.addHandler(customerWebSocket, "/ws/customer")
            .setAllowedOrigins("*")
            .addInterceptors(Interceptor())
    }

    private class Interceptor : HandshakeInterceptor {

        override fun beforeHandshake(
            request: ServerHttpRequest,
            response: ServerHttpResponse,
            wsHandler: WebSocketHandler,
            attributes: MutableMap<String, Any>
        ): Boolean {
            UriComponentsBuilder
                .fromUri(request.uri)
                .build()
                .queryParams
                .forEach { (key, value) -> value.firstOrNull()?.let { attributes[key] = it } }
            return true
        }

        override fun afterHandshake(
            request: ServerHttpRequest,
            response: ServerHttpResponse,
            wsHandler: WebSocketHandler,
            exception: Exception?
        ) {

        }
    }
}