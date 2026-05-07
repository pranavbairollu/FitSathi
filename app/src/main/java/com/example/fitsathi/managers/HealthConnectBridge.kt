package com.example.fitsathi.managers

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.records.HydrationRecord
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.response.ReadRecordsResponse
import androidx.health.connect.client.units.Mass
import androidx.health.connect.client.units.Volume
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.guava.future
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.time.Instant
import java.time.ZoneOffset

/**
 * Kotlin bridge to provide ListenableFuture based methods for Health Connect,
 * making it easier to call from Java.
 */
object HealthConnectBridge {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @JvmStatic
    fun <T : Record> readRecords(
        client: HealthConnectClient,
        request: ReadRecordsRequest<T>
    ): ListenableFuture<ReadRecordsResponse<T>> {
        return scope.future {
            try {
                client.readRecords(request)
            } catch (e: Exception) {
                android.util.Log.e("HCBridge", "Read error: ${e.message}")
                throw e
            }
        }
    }

    @JvmStatic
    fun getGrantedPermissions(client: HealthConnectClient): ListenableFuture<Set<String>> {
        return scope.future {
            client.permissionController.getGrantedPermissions()
        }
    }

    @JvmStatic
    fun <T : Record> createReadRequest(
        recordType: Class<T>,
        startTime: Instant,
        endTime: Instant
    ): ReadRecordsRequest<T> {
        return ReadRecordsRequest(
            recordType = recordType.kotlin,
            timeRangeFilter = androidx.health.connect.client.time.TimeRangeFilter.between(startTime, endTime)
        )
    }

    @JvmStatic
    fun insertRecords(client: HealthConnectClient, records: List<Record>): ListenableFuture<Unit> {
        return scope.future {
            try {
                client.insertRecords(records)
                Unit
            } catch (e: Exception) {
                android.util.Log.e("HCBridge", "Insert error: ${e.message}")
                throw e
            }
        }
    }

    @JvmStatic
    fun createStepsRecord(
        startTime: Instant,
        startZoneOffset: ZoneOffset,
        endTime: Instant,
        endZoneOffset: ZoneOffset,
        count: Long
    ): StepsRecord {
        return StepsRecord(
            startTime = startTime,
            startZoneOffset = startZoneOffset,
            endTime = endTime,
            endZoneOffset = endZoneOffset,
            count = count
        )
    }

    @JvmStatic
    fun createWeightRecord(
        time: Instant,
        zoneOffset: ZoneOffset,
        weight: Mass
    ): WeightRecord {
        return WeightRecord(
            time = time,
            zoneOffset = zoneOffset,
            weight = weight
        )
    }

    @JvmStatic
    fun createHydrationRecord(
        startTime: Instant,
        startZoneOffset: ZoneOffset,
        endTime: Instant,
        endZoneOffset: ZoneOffset,
        volume: Volume
    ): HydrationRecord {
        return HydrationRecord(
            startTime = startTime,
            startZoneOffset = startZoneOffset,
            endTime = endTime,
            endZoneOffset = endZoneOffset,
            volume = volume
        )
    }

    @JvmStatic
    fun <T : Record> getReadPermission(recordType: Class<T>): String {
        return androidx.health.connect.client.permission.HealthPermission.getReadPermission(recordType.kotlin)
    }

    @JvmStatic
    fun <T : Record> getWritePermission(recordType: Class<T>): String {
        return androidx.health.connect.client.permission.HealthPermission.getWritePermission(recordType.kotlin)
    }
}
