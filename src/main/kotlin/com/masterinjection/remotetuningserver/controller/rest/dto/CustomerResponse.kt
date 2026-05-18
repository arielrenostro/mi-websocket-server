package com.masterinjection.remotetuningserver.controller.rest.dto

import com.masterinjection.remotetuningserver.domain.ClientStatus

data class CustomerResponse(val id: String, val name: String, val status: ClientStatus)
