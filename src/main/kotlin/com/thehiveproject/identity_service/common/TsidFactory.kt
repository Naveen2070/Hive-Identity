package com.thehiveproject.identity_service.common

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Component
import java.time.Instant

private val EPOCH_START = Instant.parse("2024-01-01T00:00:00Z").toEpochMilli()
private const val NODE_ID_BITS = 10
private const val SEQUENCE_BITS = 12
private const val MAX_SEQUENCE = (1L shl SEQUENCE_BITS) - 1
private const val NODE_ID_SHIFT = SEQUENCE_BITS
private const val TIMESTAMP_SHIFT = SEQUENCE_BITS + NODE_ID_BITS

@Component
@Lazy(false)
class TsidFactory(
    @param:Value("\${app.node-id:1}") private val nodeId: Long
) {
    private var lastTimestamp = -1L
    private var sequence = 0L

    companion object {
        private var INSTANCE: TsidFactory? = null
        fun fastGenerate(): Long {
            return INSTANCE?.generate() ?: throw IllegalStateException("TsidFactory not initialized")
        }
    }

    init {
        INSTANCE = this
    }

    @Synchronized
    fun generate(): Long {
        var currentTimestamp = System.currentTimeMillis()

        if (currentTimestamp < lastTimestamp) {
            // Simple clock rollback handling
            val offset = lastTimestamp - currentTimestamp
            if (offset < 5) {
                Thread.sleep(offset + 1)
                currentTimestamp = System.currentTimeMillis()
            } else {
                throw IllegalStateException("Clock moved backwards")
            }
        }

        if (currentTimestamp == lastTimestamp) {
            sequence = (sequence + 1) and MAX_SEQUENCE
            if (sequence == 0L) {
                currentTimestamp = waitNextMillis(currentTimestamp)
            }
        } else {
            sequence = 0L
        }

        lastTimestamp = currentTimestamp

        return ((currentTimestamp - EPOCH_START) shl TIMESTAMP_SHIFT) or
                (nodeId shl NODE_ID_SHIFT) or
                sequence
    }

    private fun waitNextMillis(currentTimestamp: Long): Long {
        var timestamp = currentTimestamp
        while (timestamp <= lastTimestamp) {
            timestamp = System.currentTimeMillis()
        }
        return timestamp
    }
}