package org.westminster.api.resource;

import javax.ws.rs.*;
import javax.ws.rs.core.*;
import org.westminster.api.data.DataStore;
import org.westminster.api.model.*;
import org.westminster.api.exception.SensorUnavailableException;
import java.util.*;

public class SensorReadingResource {
    private String sid;
    public SensorReadingResource(String sid) { this.sid = sid; }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response add(SensorReading r) {
        Sensor s = DataStore.sensors.get(sid);
        if (s != null && "MAINTENANCE".equalsIgnoreCase(s.getStatus())) {
            throw new SensorUnavailableException("Sensor is down.");
        }
        DataStore.sensorReadings.computeIfAbsent(sid, k -> new ArrayList<>()).add(r);
        if (s != null) s.setCurrentValue(r.getValue());
        return Response.status(201).entity(r).build();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<SensorReading> get() {
        return DataStore.sensorReadings.getOrDefault(sid, new ArrayList<>());
    }
}