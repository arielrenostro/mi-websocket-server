package com.masterinjection.remotetuningserver.domain.message

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonSubTypes.Type
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.masterinjection.remotetuningserver.domain.message.customer.RegisterTunerRequestMessage
import com.masterinjection.remotetuningserver.domain.message.generic.EchoSerialDataMessage
import com.masterinjection.remotetuningserver.domain.message.generic.ErrorMessage
import com.masterinjection.remotetuningserver.domain.message.generic.PairConnectedMessage
import com.masterinjection.remotetuningserver.domain.message.generic.PairDisconnectedMessage
import com.masterinjection.remotetuningserver.domain.message.generic.RegisteredMessage
import com.masterinjection.remotetuningserver.domain.message.tuner.RegisterToCustomerResponseMessage

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.EXISTING_PROPERTY,
    property = "type"
)
@JsonSubTypes(
    Type(value = RegisteredMessage::class, name = "REGISTERED"),
    Type(value = ErrorMessage::class, name = "ERROR"),
    Type(value = EchoSerialDataMessage::class, name = "ECHO_SERIAL_DATA"),
    Type(value = PairConnectedMessage::class, name = "PAIR_CONNECTED"),
    Type(value = PairDisconnectedMessage::class, name = "PAIR_DISCONNECTED"),
    Type(value = RegisterToCustomerResponseMessage::class, name = "REGISTER_TO_CUSTOMER_RESPONSE"),
    Type(value = RegisterTunerRequestMessage::class, name = "REGISTER_TUNER"),
)
abstract class BaseMessage(
    val timestamp: Long,
    val type: MessageType,
)
