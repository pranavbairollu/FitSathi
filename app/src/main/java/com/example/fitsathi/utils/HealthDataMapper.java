package com.example.fitsathi.utils;

import androidx.health.connect.client.records.HydrationRecord;
import androidx.health.connect.client.records.StepsRecord;
import androidx.health.connect.client.records.WeightRecord;
import androidx.health.connect.client.units.Mass;
import androidx.health.connect.client.units.Volume;

import com.example.fitsathi.data.entities.StepLog;
import com.example.fitsathi.models.WeightLog;
import com.example.fitsathi.managers.HealthConnectBridge;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class to map FitSathi data models to Health Connect records.
 */
public class HealthDataMapper {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    public static StepsRecord mapToStepsRecord(StepLog stepLog) {
        LocalDate date = LocalDate.parse(stepLog.getDate(), DATE_FORMATTER);
        Instant startOfDay = date.atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant endOfDay = date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();

        return HealthConnectBridge.createStepsRecord(
                startOfDay,
                ZoneId.systemDefault().getRules().getOffset(startOfDay),
                endOfDay,
                ZoneId.systemDefault().getRules().getOffset(endOfDay),
                (long) stepLog.getSteps()
        );
    }

    public static WeightRecord mapToWeightRecord(WeightLog weightLog) {
        Instant time = Instant.ofEpochMilli(weightLog.getTimestamp());
        return HealthConnectBridge.createWeightRecord(
                time,
                ZoneId.systemDefault().getRules().getOffset(time),
                Mass.kilograms(weightLog.getWeight())
        );
    }

    public static List<HydrationRecord> mapToHydrationRecords(String dateStr, List<Long> intakeList) {
        List<HydrationRecord> records = new ArrayList<>();
        LocalDate date = LocalDate.parse(dateStr, DATE_FORMATTER);
        
        Instant baseTime = date.atStartOfDay(ZoneId.systemDefault()).toInstant();
        
        for (int i = 0; i < intakeList.size(); i++) {
            Long amountMl = intakeList.get(i);
            Instant startTime = baseTime.plusSeconds(i * 3600);
            Instant endTime = startTime.plusSeconds(60); // 1 minute duration
            
            records.add(HealthConnectBridge.createHydrationRecord(
                    startTime,
                    ZoneId.systemDefault().getRules().getOffset(startTime),
                    endTime,
                    ZoneId.systemDefault().getRules().getOffset(endTime),
                    Volume.milliliters(amountMl.doubleValue())
            ));
        }
        return records;
    }
}
