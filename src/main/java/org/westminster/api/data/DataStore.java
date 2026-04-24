package org.westminster.api.data;

import java.util.*;
import org.westminster.api.model.*;

public class DataStore {
    // Shared maps to store data throughout the application lifecycle
    public static Map<String, Room> rooms = new HashMap<>();
    public static Map<String, Sensor> sensors = new HashMap<>();
    // Map sensor ID to a list of its readings
    public static Map<String, List<SensorReading>> sensorReadings = new HashMap<>();

    // Sample data loaded automatically when app starts
    static {
        Room r1 = new Room("LIB-301", "Library Quiet Study", 50);
        Room r2 = new Room("LAB-101", "Computer Lab", 30);
        rooms.put(r1.getId(), r1);
        rooms.put(r2.getId(), r2);

        Sensor s1 = new Sensor("TEMP-001", "Temperature", "ACTIVE", 22.5, "LIB-301");
        r1.getSensorIds().add(s1.getId());
        sensors.put(s1.getId(), s1);
        sensorReadings.put(s1.getId(), new ArrayList<>());

        Sensor s2 = new Sensor("CO2-001", "CO2", "ACTIVE", 412.0, "LAB-101");
        r2.getSensorIds().add(s2.getId());
        sensors.put(s2.getId(), s2);
        sensorReadings.put(s2.getId(), new ArrayList<>());
    }
}