package com.masterinjection.websocket.extension

import tools.jackson.databind.ObjectMapper

class GlobalMapper {

    companion object {

        private var INSTANCE: ObjectMapper? = null

        fun getInstance(): ObjectMapper {
            return INSTANCE ?: throw IllegalStateException("GlobalMapper not initialized")
        }

        fun register(mapper: ObjectMapper) {
            INSTANCE = mapper
        }
    }
}