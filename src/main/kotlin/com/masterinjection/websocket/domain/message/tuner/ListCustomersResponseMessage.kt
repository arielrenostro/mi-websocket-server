package com.masterinjection.websocket.domain.message.tuner

import com.masterinjection.websocket.domain.Customer
import com.masterinjection.websocket.domain.message.BaseResponseMessage
import com.masterinjection.websocket.domain.message.MessageType

class ListCustomersResponseMessage(
    timestamp: Long,
    responseTo: Long,
    val customers: List<CustomerResponse>
) : BaseResponseMessage(
    timestamp = timestamp,
    responseTo = responseTo,
    type = MessageType.LIST_CUSTOMERS_RESPONSE,
) {

    data class CustomerResponse(
        val id: String,
        val name: String,
        val tunerConnected: Boolean,
    ) {
        companion object {
            fun fromCustomer(customer: Customer): CustomerResponse {
                return CustomerResponse(
                    id = customer.id,
                    name = customer.name,
                    tunerConnected = customer.tunerConnected != null,
                )
            }
        }
    }
}
