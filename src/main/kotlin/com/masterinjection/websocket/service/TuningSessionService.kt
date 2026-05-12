package com.masterinjection.websocket.service

import com.masterinjection.websocket.domain.AuthenticatedClient
import com.masterinjection.websocket.domain.ClientStatus
import com.masterinjection.websocket.domain.ClientType
import com.masterinjection.websocket.domain.Customer
import com.masterinjection.websocket.domain.Tuner
import com.masterinjection.websocket.domain.message.BaseMessage
import com.masterinjection.websocket.domain.message.customer.RegisterTunerRequestMessage
import com.masterinjection.websocket.domain.message.generic.EchoSerialDataMessage
import com.masterinjection.websocket.domain.message.generic.PairConnectedMessage
import com.masterinjection.websocket.domain.message.generic.PairDisconnectedMessage
import com.masterinjection.websocket.domain.message.generic.RegisteredMessage
import com.masterinjection.websocket.domain.message.tuner.RegisterToCustomerResponseMessage
import com.masterinjection.websocket.extension.sendJsonError
import com.masterinjection.websocket.extension.sendJsonMessage
import org.slf4j.LoggerFactory
import org.springframework.scheduling.TaskScheduler
import org.springframework.stereotype.Service
import org.springframework.web.socket.WebSocketSession
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

@Service
class TuningSessionService(
    private val scheduler: TaskScheduler,
) {

    private val logger = LoggerFactory.getLogger(TuningSessionService::class.java)

    private val tuners = ConcurrentHashMap<String, Tuner>()
    private val customers = ConcurrentHashMap<String, Customer>()
    private val tunersByWsSessionId = ConcurrentHashMap<String, Tuner>()
    private val customersByWsSessionId = ConcurrentHashMap<String, Customer>()
    private val pendingRegisters = ConcurrentHashMap<Long, PendingRegisterDto>()

    // --- WebSocket lifecycle ---

    fun newTunerConnection(session: WebSocketSession, name: String) {
        val tuner = Tuner(
            id = UUID.randomUUID().toString(),
            name = name,
            secret = UUID.randomUUID().toString().replace("-", ""),
            wsSession = session,
        )
        tuners[tuner.id] = tuner
        tunersByWsSessionId[session.id] = tuner
        session.sendJsonMessage(RegisteredMessage(tuner.id, tuner.name, tuner.secret, System.currentTimeMillis()))
        logger.info("Tuner registered: ${tuner.id} name=${tuner.name} session=${session.id}")
    }

    fun reconnectTunerWs(session: WebSocketSession, id: String, secret: String) {
        val tuner = validateTuner(id, secret)
        tuner.wsSession?.let { old -> tunersByWsSessionId.remove(old.id) }
        tuner.wsSession = session
        tuner.disconnectedAt = null
        tuner.status = if (tuner.customerConnected != null) ClientStatus.PAIRED else ClientStatus.CONNECTED
        tunersByWsSessionId[session.id] = tuner
        session.sendJsonMessage(RegisteredMessage(tuner.id, tuner.name, tuner.secret, System.currentTimeMillis()))
        tuner.customerConnected?.let { customer ->
            session.sendJsonMessage(PairConnectedMessage(System.currentTimeMillis(), PairConnectedMessage.Peer(customer.id, customer.name)))
        }
        logger.info("Tuner reconnected: ${tuner.id} session=${session.id}")
    }

    fun disconnectTunerWs(session: WebSocketSession) {
        val tuner = tunersByWsSessionId.remove(session.id) ?: return
        tuner.wsSession = null
        tuner.status = ClientStatus.DISCONNECTED
        val disconnectedAt = Instant.now()
        tuner.disconnectedAt = disconnectedAt

        tuner.customerConnected?.let { customer ->
            tuner.customerConnected = null
            customer.tunerConnected = null
            if (customer.wsSession != null) customer.status = ClientStatus.CONNECTED
            customer.wsSession?.sendJsonMessage(PairDisconnectedMessage(System.currentTimeMillis()))
        }

        scheduler.schedule(Runnable {
            if (tuners[tuner.id]?.disconnectedAt == disconnectedAt) {
                tuners.remove(tuner.id)
                logger.info("Tuner auto-removed after 2min disconnect: ${tuner.id}")
            }
        }, Instant.now().plusSeconds(120))

        logger.info("Tuner WS disconnected: ${tuner.id} session=${session.id}")
    }

    fun newCustomerConnection(session: WebSocketSession, name: String) {
        val customer = Customer(
            id = UUID.randomUUID().toString(),
            name = name,
            secret = UUID.randomUUID().toString().replace("-", ""),
            wsSession = session,
        )
        customers[customer.id] = customer
        customersByWsSessionId[session.id] = customer
        session.sendJsonMessage(RegisteredMessage(customer.id, customer.name, customer.secret, System.currentTimeMillis()))
        logger.info("Customer registered: ${customer.id} name=${customer.name} session=${session.id}")
    }

    fun reconnectCustomerWs(session: WebSocketSession, id: String, secret: String) {
        val customer = validateCustomer(id, secret)
        customer.wsSession?.let { old -> customersByWsSessionId.remove(old.id) }
        customer.wsSession = session
        customer.disconnectedAt = null
        customer.status = if (customer.tunerConnected != null) ClientStatus.PAIRED else ClientStatus.CONNECTED
        customersByWsSessionId[session.id] = customer
        session.sendJsonMessage(RegisteredMessage(customer.id, customer.name, customer.secret, System.currentTimeMillis()))
        customer.tunerConnected?.let { tuner ->
            session.sendJsonMessage(PairConnectedMessage(System.currentTimeMillis(), PairConnectedMessage.Peer(tuner.id, tuner.name)))
        }
        logger.info("Customer reconnected: ${customer.id} session=${session.id}")
    }

    fun disconnectCustomerWs(session: WebSocketSession) {
        val customer = customersByWsSessionId.remove(session.id) ?: return
        customer.wsSession = null
        customer.status = ClientStatus.DISCONNECTED
        val disconnectedAt = Instant.now()
        customer.disconnectedAt = disconnectedAt

        customer.tunerConnected?.let { tuner ->
            customer.tunerConnected = null
            tuner.customerConnected = null
            if (tuner.wsSession != null) tuner.status = ClientStatus.CONNECTED
            tuner.wsSession?.sendJsonMessage(PairDisconnectedMessage(System.currentTimeMillis()))
        }

        scheduler.schedule(Runnable {
            if (customers[customer.id]?.disconnectedAt == disconnectedAt) {
                customers.remove(customer.id)
                logger.info("Customer auto-removed after 2min disconnect: ${customer.id}")
            }
        }, Instant.now().plusSeconds(120))

        logger.info("Customer WS disconnected: ${customer.id} session=${session.id}")
    }

    // --- REST: authentication (interceptor entry point) ---

    fun authenticate(id: String, secret: String): AuthenticatedClient {
        tuners[id]?.let {
            if (it.secret != secret) throw SecurityException("Invalid secret for tuner $id")
            return AuthenticatedClient(id, ClientType.TUNER)
        }
        customers[id]?.let {
            if (it.secret != secret) throw SecurityException("Invalid secret for customer $id")
            return AuthenticatedClient(id, ClientType.CUSTOMER)
        }
        throw SecurityException("Client not found: $id")
    }

    // --- REST: operations (authorization enforced here) ---

    fun getTunerById(id: String, client: AuthenticatedClient): Tuner {
        if (client.type != ClientType.TUNER || client.id != id) throw SecurityException("Access denied")
        return getTuner(id)
    }

    fun getCustomerById(id: String, client: AuthenticatedClient): Customer {
        if (client.type != ClientType.CUSTOMER || client.id != id) throw SecurityException("Access denied")
        return getCustomer(id)
    }

    fun listCustomers(client: AuthenticatedClient): List<Customer> {
        if (client.type != ClientType.TUNER) throw SecurityException("Only tuners can list customers")
        return customers.values.toList()
    }

    fun unregisterTuner(id: String, client: AuthenticatedClient) {
        if (client.type != ClientType.TUNER || client.id != id) throw SecurityException("Access denied")
        val tuner = tuners.remove(id) ?: return
        tuner.wsSession?.let { tunersByWsSessionId.remove(it.id) }
        tuner.customerConnected?.let { customer ->
            customer.tunerConnected = null
            if (customer.wsSession != null) customer.status = ClientStatus.CONNECTED
            customer.wsSession?.sendJsonMessage(PairDisconnectedMessage(System.currentTimeMillis()))
        }
        logger.info("Tuner unregistered: ${tuner.id}")
    }

    fun unregisterCustomer(id: String, client: AuthenticatedClient) {
        if (client.type != ClientType.CUSTOMER || client.id != id) throw SecurityException("Access denied")
        val customer = customers.remove(id) ?: return
        customer.wsSession?.let { customersByWsSessionId.remove(it.id) }
        customer.tunerConnected?.let { tuner ->
            tuner.customerConnected = null
            if (tuner.wsSession != null) tuner.status = ClientStatus.CONNECTED
            tuner.wsSession?.sendJsonMessage(PairDisconnectedMessage(System.currentTimeMillis()))
        }
        logger.info("Customer unregistered: ${customer.id}")
    }

    fun requestConnection(client: AuthenticatedClient, customerId: String): Long {
        if (client.type != ClientType.TUNER) throw SecurityException("Only tuners can request connections")
        val tuner = getTuner(client.id)
        val customer = getCustomer(customerId)

        check(tuner.customerConnected == null) {
            "Tuner already paired with customer: ${tuner.customerConnected?.id}"
        }
        check(customer.tunerConnected == null) {
            "Customer already paired with tuner: ${customer.tunerConnected?.id}"
        }
        val customerWs = customer.wsSession
            ?: throw IllegalStateException("Customer ${customer.id} has no active WebSocket connection")

        val requestId = System.currentTimeMillis()
        customerWs.sendJsonMessage(
            RegisterTunerRequestMessage(
                timestamp = requestId,
                tuner = RegisterTunerRequestMessage.Tuner(id = tuner.id, name = tuner.name),
            )
        )
        pendingRegisters[requestId] = PendingRegisterDto(requestId, tuner, customer)

        scheduler.schedule(Runnable {
            if (pendingRegisters.remove(requestId) != null) {
                tuner.wsSession?.sendJsonMessage(
                    RegisterToCustomerResponseMessage(
                        timestamp = System.currentTimeMillis(),
                        responseTo = requestId,
                        success = false,
                    )
                )
                logger.info("Connection request $requestId timed out")
            }
        }, Instant.now().plusSeconds(15))

        logger.info("Connection request $requestId: tuner=${tuner.id} → customer=${customer.id}")
        return requestId
    }

    fun respondToConnection(client: AuthenticatedClient, requestId: Long, accepted: Boolean) {
        if (client.type != ClientType.CUSTOMER) throw SecurityException("Only customers can respond to connections")
        val pending = pendingRegisters.remove(requestId)
            ?: throw IllegalArgumentException("No pending connection request: $requestId")

        require(pending.customer.id == client.id) {
            "Connection request does not belong to this customer"
        }

        val tuner = pending.tuner
        val customer = pending.customer
        val now = System.currentTimeMillis()

        if (accepted) {
            tuner.customerConnected = customer
            customer.tunerConnected = tuner
            tuner.status = ClientStatus.PAIRED
            customer.status = ClientStatus.PAIRED
            tuner.wsSession?.sendJsonMessage(PairConnectedMessage(now, PairConnectedMessage.Peer(customer.id, customer.name)))
            customer.wsSession?.sendJsonMessage(PairConnectedMessage(now, PairConnectedMessage.Peer(tuner.id, tuner.name)))
        } else {
            tuner.wsSession?.sendJsonMessage(RegisterToCustomerResponseMessage(now, requestId, success = false))
        }

        logger.info("Connection request $requestId: accepted=$accepted tuner=${tuner.id} customer=${customer.id}")
    }

    // --- WebSocket: message handling ---

    fun onTunerMessage(session: WebSocketSession, message: BaseMessage) {
        try {
            val tuner = tunersByWsSessionId[session.id]
                ?: throw IllegalStateException("WS session not linked to any tuner: ${session.id}")
            when (message) {
                is EchoSerialDataMessage -> {
                    val ws = tuner.customerConnected?.wsSession
                        ?: throw IllegalStateException("Tuner has no paired customer with an active WebSocket")
                    ws.sendJsonMessage(message)
                }
                else -> throw IllegalArgumentException("Unsupported message type: ${message.type}")
            }
        } catch (e: Exception) {
            logger.warn("Failure during tuner WS message. Session: ${session.id} Error: ${e.message}")
            session.sendJsonError(message.timestamp, e.message)
        }
    }

    fun onCustomerMessage(session: WebSocketSession, message: BaseMessage) {
        try {
            val customer = customersByWsSessionId[session.id]
                ?: throw IllegalStateException("WS session not linked to any customer: ${session.id}")
            when (message) {
                is EchoSerialDataMessage -> {
                    val ws = customer.tunerConnected?.wsSession
                        ?: throw IllegalStateException("Customer has no paired tuner with an active WebSocket")
                    ws.sendJsonMessage(message)
                }
                else -> throw IllegalArgumentException("Unsupported message type: ${message.type}")
            }
        } catch (e: Exception) {
            logger.warn("Failure during customer WS message. Session: ${session.id} Error: ${e.message}")
            session.sendJsonError(message.timestamp, e.message)
        }
    }

    // --- internal helpers ---

    private fun validateTuner(id: String, secret: String): Tuner {
        val tuner = tuners[id] ?: throw IllegalArgumentException("Tuner not found: $id")
        if (tuner.secret != secret) throw SecurityException("Invalid secret for tuner $id")
        return tuner
    }

    private fun validateCustomer(id: String, secret: String): Customer {
        val customer = customers[id] ?: throw IllegalArgumentException("Customer not found: $id")
        if (customer.secret != secret) throw SecurityException("Invalid secret for customer $id")
        return customer
    }

    private fun getTuner(id: String): Tuner =
        tuners[id] ?: throw IllegalArgumentException("Tuner not found: $id")

    private fun getCustomer(id: String): Customer =
        customers[id] ?: throw IllegalArgumentException("Customer not found: $id")

    private data class PendingRegisterDto(
        val requestId: Long,
        val tuner: Tuner,
        val customer: Customer,
    )
}
