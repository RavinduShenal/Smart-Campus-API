package org.westminster.api.data;

import java.util.*;
import org.westminster.api.model.*;

public class DataStore {
    // Shared maps to store data throughout the application lifecycle
    public static Map<String, Room> rooms = new HashMap<>();
    public static Map<String, Sensor> sensors = new HashMap<>();
    // Map sensor ID to a list of its readings
    public static Map<String, List<SensorReading>> sensorReadings = new HashMap<>();
}