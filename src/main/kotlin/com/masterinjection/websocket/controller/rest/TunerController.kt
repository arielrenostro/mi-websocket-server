package com.masterinjection.websocket.controller.rest

import com.masterinjection.websocket.controller.rest.dto.StateResponse
import com.masterinjection.websocket.domain.AuthenticatedClient
import com.masterinjection.websocket.domain.Tuner
import com.masterinjection.websocket.service.TuningSessionService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/tuners")
class TunerController(private val tuningSessionService: TuningSessionService) {

    @GetMapping("/{id}/state")
    fun getState(@PathVariable id: String, client: AuthenticatedClient): ResponseEntity<StateResponse> {
        val tuner = tuningSessionService.getTunerById(id, client)
        return ResponseEntity.ok(tuner.toStateResponse())
    }

    @DeleteMapping("/{id}")
    fun unregister(@PathVariable id: String, client: AuthenticatedClient): ResponseEntity<Void> {
        tuningSessionService.unregisterTuner(id, client)
        return ResponseEntity.noContent().build()
    }

    private fun Tuner.toStateResponse() = StateResponse(id, name, status)
}
