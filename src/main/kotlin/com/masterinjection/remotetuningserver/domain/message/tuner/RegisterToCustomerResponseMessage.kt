package com.masterinjection.remotetuningserver.domain.message.tuner

import com.masterinjection.remotetuningserver.domain.message.BaseResponseMessage
import com.masterinjection.remotetuningserver.domain.message.MessageType

class RegisterToCustomerResponseMessage(
    timestamp: Long,
    responseTo: Long,
    val success: Boolean,
) : BaseResponseMessage(
    timestamp = timestamp,
    responseTo = responseTo,
    type = MessageType.REGISTER_TO_CUSTOMER_RESPONSE,
)
