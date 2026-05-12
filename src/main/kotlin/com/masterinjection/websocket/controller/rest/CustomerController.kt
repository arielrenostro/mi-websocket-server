package com.masterinjection.websocket.controller.rest

import com.masterinjection.websocket.controller.rest.dto.CustomerResponse
import com.masterinjection.websocket.controller.rest.dto.StateResponse
import com.masterinjection.websocket.domain.AuthenticatedClient
import com.masterinjection.websocket.domain.Customer
import com.masterinjection.websocket.service.TuningSessionService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/customers")
class CustomerController(private val tuningSessionService: TuningSessionService) {

    @GetMapping("/{id}/state")
    fun getState(@PathVariable id: String, client: AuthenticatedClient): ResponseEntity<StateResponse> {
        val customer = tuningSessionService.getCustomerById(id, client)
        return ResponseEntity.ok(customer.toStateResponse())
    }

    @DeleteMapping("/{id}")
    fun unregister(@PathVariable id: String, client: AuthenticatedClient): ResponseEntity<Void> {
        tuningSessionService.unregisterCustomer(id, client)
        return ResponseEntity.noContent().build()
    }

    @GetMapping
    fun list(client: AuthenticatedClient): ResponseEntity<List<CustomerResponse>> {
        val customers = tuningSessionService.listCustomers(client)
        return ResponseEntity.ok(customers.map { it.toCustomerResponse() })
    }

    private fun Customer.toStateResponse() = StateResponse(id, name, tunerConnected != null)

    private fun Customer.toCustomerResponse() = CustomerResponse(id, name, tunerConnected != null)
}
