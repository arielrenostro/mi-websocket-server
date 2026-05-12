package com.masterinjection.websocket.controller.rest

import com.masterinjection.websocket.controller.rest.dto.ConnectRequest
import com.masterinjection.websocket.controller.rest.dto.ConnectResponse
import com.masterinjection.websocket.controller.rest.dto.ConnectionRespondRequest
import com.masterinjection.websocket.domain.AuthenticatedClient
import com.masterinjection.websocket.service.TuningSessionService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/connections")
class ConnectionController(private val tuningSessionService: TuningSessionService) {

    @PostMapping
    fun requestConnection(client: AuthenticatedClient, @RequestBody request: ConnectRequest): ResponseEntity<ConnectResponse> {
        val requestId = tuningSessionService.requestConnection(client, request.customerId)
        return ResponseEntity.ok(ConnectResponse(requestId))
    }

    @PostMapping("/{requestId}/respond")
    fun respond(
        @PathVariable requestId: Long,
        client: AuthenticatedClient,
        @RequestBody request: ConnectionRespondRequest,
    ): ResponseEntity<Void> {
        tuningSessionService.respondToConnection(client, requestId, request.accepted)
        return ResponseEntity.ok().build()
    }
}
