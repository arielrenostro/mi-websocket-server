package com.masterinjection.websocket.service

import com.masterinjection.websocket.domain.Customer
import com.masterinjection.websocket.domain.ITuningListener
import com.masterinjection.websocket.domain.Tuner
import com.masterinjection.websocket.domain.message.BaseMessage
import com.masterinjection.websocket.domain.message.customer.RegisterTunerRequestMessage
import com.masterinjection.websocket.domain.message.customer.RegisterTunerResponseMessage
import com.masterinjection.websocket.domain.message.generic.*
import com.masterinjection.websocket.domain.message.tuner.ListCustomersRequestMessage
import com.masterinjection.websocket.domain.message.tuner.ListCustomersResponseMessage
import com.masterinjection.websocket.domain.message.tuner.RegisterToCustomerRequestMessage
import com.masterinjection.websocket.domain.message.tuner.RegisterToCustomerResponseMessage
import com.masterinjection.websocket.extension.sendJsonError
import com.masterinjection.websocket.extension.sendJsonMessage
import org.slf4j.LoggerFactory
import org.springframework.scheduling.TaskScheduler
import org.springframework.stereotype.Service
import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.WebSocketSession
import java.time.Instant
import java.util.*
import java.util.concurrent.ConcurrentHashMap

@Service
class TuningSessionService(
    private val scheduler: TaskScheduler,
) {

    private val logger = LoggerFactory.getLogger(TuningSessionService::class.java)
    private val customers = ConcurrentHashMap<String, Customer>()
    private val tuners = ConcurrentHashMap<String, Tuner>()
    private val listeners = Collections.synchronizedList(mutableListOf<ITuningListener>())

    // TODO: remove this after response structure.
    private val pendingRegisters = ConcurrentHashMap<Long, PendingRegisterDto>()

    fun registerTuner(session: WebSocketSession, name: String?) {
        if (name.isNullOrBlank()) {
            this.logger.warn("Tuner name $name not found during registration")
            session.sendJsonError(null, "Tuner Name not found")
            session.close(CloseStatus.NOT_ACCEPTABLE)
            return
        }

        val tuner = Tuner(
            id = session.id,
            name = name,
            session = session,
        )
        tuners.putIfAbsent(tuner.id, tuner)

        tuner.session.sendJsonMessage(
            RegisteredMessage(
                id = tuner.id,
                name = tuner.name,
                timestamp = System.currentTimeMillis(),
            )
        )
    }

    fun registerCustomer(session: WebSocketSession, name: String?) {
        if (name.isNullOrBlank()) {
            this.logger.warn("Customer name $name not found during registration")
            session.sendJsonError(null, "Customer Name not found")
            session.close(CloseStatus.NOT_ACCEPTABLE)
            return
        }

        val customer = Customer(
            id = session.id,
            name = name,
            session = session,
        )
        customers.putIfAbsent(customer.id, customer)

        this.dispatchListeners { it.onCustomerRegister(customer) }

        customer.session.sendJsonMessage(
            RegisteredMessage(
                id = customer.id,
                name = customer.name,
                timestamp = System.currentTimeMillis(),
            )
        )
    }

    fun unregisterCustomer(session: WebSocketSession, status: CloseStatus) {
        val customer = customers.remove(session.id)
        if (customer == null) {
            this.logger.warn("Customer not found during unregister: ${session.id} - $status")
            return
        }
        customer.tunerConnected?.let {
            it.session.sendJsonMessage(
                PairDisconnectedMessage(
                    timestamp = System.currentTimeMillis(),
                )
            )
            customer.tunerConnected = null
        }
    }

    fun unregisterTuner(session: WebSocketSession, status: CloseStatus) {
        val tuner = tuners.remove(session.id)
        if (tuner == null) {
            this.logger.warn("Tuner not found during unregister: ${session.id} - $status")
            return
        }
        tuner.customerConnected?.let {
            it.session.sendJsonMessage(
                PairDisconnectedMessage(
                    timestamp = System.currentTimeMillis(),
                )
            )
            tuner.customerConnected = null
        }
    }

    private fun dispatchListeners(fn: (ITuningListener) -> Unit) {
        for (listener in this.listeners) {
            try {
                fn(listener)
            } catch (e: Exception) {
                this.logger.error("Failure during listener dispatch: ${e.message}", e)
            }
        }
    }

    fun getTuner(id: String): Tuner {
        return tuners[id] ?: throw IllegalArgumentException("No Tuner found with id $id")
    }

    fun getCustomer(id: String): Customer {
        return customers[id] ?: throw IllegalArgumentException("No Customer found with id $id")
    }

    fun onTunerMessage(session: WebSocketSession, message: BaseMessage) {
        try {
            val tuner = getTuner(session.id)
            when (message) {
                is ListCustomersRequestMessage -> onListCustomerRequest(tuner, message)
                is RegisterToCustomerRequestMessage -> onRegisterToCustomerRequest(tuner, message)
                is EchoSerialDataMessage -> onEchoSerialDataMessage(tuner, message)
                is GetStateRequestMessage -> onGetStateRequest(tuner, message)
                else -> throw IllegalArgumentException("Unsupported message type ${message.type}")
            }
        } catch (e: Exception) {
            this.logger.warn("Failure during tuner message. Session: ${session.id} Timestamp: ${message.timestamp} Type: ${e.message}")
            session.sendJsonError(message.timestamp, e.message)
        }
    }

    private fun onEchoSerialDataMessage(
        tuner: Tuner,
        message: EchoSerialDataMessage
    ) {
        val customer = tuner.customerConnected ?: throw IllegalStateException("Customer not connected")
        customer.session.sendJsonMessage(message)
    }

    private fun onEchoSerialDataMessage(
        customer: Customer,
        message: EchoSerialDataMessage
    ) {
        val tuner = customer.tunerConnected ?: throw IllegalStateException("Tuner not connected")
        tuner.session.sendJsonMessage(message)
    }

    private fun onRegisterToCustomerRequest(tuner: Tuner, message: RegisterToCustomerRequestMessage) {
        val customer = getCustomer(message.customerId)
        if (customer.tunerConnected != null) {
            throw IllegalStateException("Tuner already connected: ${customer.tunerConnected?.id} ${customer.tunerConnected?.name}")
        }
        if (tuner.customerConnected != null) {
            throw IllegalStateException("Tuner already connected to another customer: ${tuner.customerConnected?.id} ${tuner.customerConnected?.name}")
        }

        val registerTimestamp = System.currentTimeMillis()
        customer.session.sendJsonMessage(
            RegisterTunerRequestMessage(
                timestamp = registerTimestamp,
                tuner = RegisterTunerRequestMessage.Tuner(
                    id = tuner.id,
                    name = tuner.name,
                )
            )
        )

        // TODO: implements a structure to send message to websocket clients and await for answer.
        pendingRegisters[registerTimestamp] = PendingRegisterDto(
            request = message,
            registerTimestamp = registerTimestamp,
            tuner = tuner,
        )

        // wait 3s and answer the tuner if customer not answer.
        val fn = Runnable {
            pendingRegisters[registerTimestamp]?.let {
                tuner.session.sendJsonMessage(
                    RegisterToCustomerResponseMessage(
                        timestamp = System.currentTimeMillis(),
                        responseTo = message.timestamp,
                        success = false,
                    )
                )
                pendingRegisters.remove(registerTimestamp)
            }
        }
        this.scheduler.schedule(fn, Instant.now().plusSeconds(15)) // TODO FIX

    }

    private fun onListCustomerRequest(tuner: Tuner, message: ListCustomersRequestMessage) {
        val response = ListCustomersResponseMessage(
            timestamp = System.currentTimeMillis(),
            responseTo = message.timestamp,
            customers = this.customers.values.map { ListCustomersResponseMessage.CustomerResponse.fromCustomer(it) }
        )
        tuner.session.sendJsonMessage(response)
    }

    fun onCustomerMessage(session: WebSocketSession, message: BaseMessage) {
        try {
            val customer = getCustomer(session.id)
            when (message) {
                is RegisterTunerResponseMessage -> onRegisterTunerResponse(customer, message)
                is GetStateRequestMessage -> onGetStateRequest(customer, message)
                is EchoSerialDataMessage -> onEchoSerialDataMessage(customer, message)
                else -> throw IllegalArgumentException("Unsupported message type ${message.type}")
            }

        } catch (e: Exception) {
            this.logger.warn("Failure during customer message. Session: ${session.id} Timestamp: ${message.timestamp} Type: ${e.message}")
            session.sendJsonError(message.timestamp, e.message)
        }
    }

    private fun onGetStateRequest(tuner: Tuner, message: GetStateRequestMessage) {
        tuner.session.sendJsonMessage(
            GetStateResponseMessage(
                timestamp = System.currentTimeMillis(),
                responseTo = message.timestamp,
                data = GetStateResponseMessage.Data(
                    id = tuner.id,
                    name = tuner.name,
                    connected = tuner.customerConnected != null,
                )
            )
        )
    }

    private fun onGetStateRequest(customer: Customer, message: GetStateRequestMessage) {
        customer.session.sendJsonMessage(
            GetStateResponseMessage(
                timestamp = System.currentTimeMillis(),
                responseTo = message.timestamp,
                data = GetStateResponseMessage.Data(
                    id = customer.id,
                    name = customer.name,
                    connected = customer.tunerConnected != null,
                )
            )
        )
    }

    private fun onRegisterTunerResponse(customer: Customer, message: RegisterTunerResponseMessage) {

        val pendingRegister = pendingRegisters[message.responseTo]
            ?: throw IllegalArgumentException("No register found: ${customer.id} ${customer.name} - ${message.responseTo}")

        val tuner = pendingRegister.tuner

        if (customer.tunerConnected != null) {
            throw IllegalStateException("Tuner already connected: ${customer.tunerConnected?.id} ${customer.tunerConnected?.name}")
        }
        if (tuner.customerConnected != null) {
            throw IllegalStateException("Tuner already connected to another customer: ${tuner.customerConnected?.id} ${tuner.customerConnected?.name}")
        }

        pendingRegisters.remove(message.responseTo)

        if (message.success) {
            tuner.customerConnected = customer
            customer.tunerConnected = tuner
        }

        tuner.session.sendJsonMessage(
            RegisterToCustomerResponseMessage(
                timestamp = System.currentTimeMillis(),
                responseTo = pendingRegister.request.timestamp,
                success = message.success,
            )
        )
        customer.session.sendJsonMessage(
            RegisterToCustomerResponseMessage(
                timestamp = System.currentTimeMillis(),
                responseTo = message.timestamp,
                success = message.success,
            )
        )

        this.logger.info("Tuner ${tuner.id} ${tuner.name} registered to Customer ${customer.id} ${customer.name}")
    }

    private data class PendingRegisterDto(
        val registerTimestamp: Long,
        val request: RegisterToCustomerRequestMessage,
        val tuner: Tuner,
    )
}