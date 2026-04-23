package org.westminster.api.resource;

import javax.ws.rs.*;
import javax.ws.rs.core.*;
import org.westminster.api.data.DataStore;
import org.westminster.api.model.Sensor;
import org.westminster.api.exception.LinkedResourceNotFoundException;
import java.util.*;
import java.util.stream.Collectors;

@Path("/sensors")
public class SensorResource {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<Sensor> getSensors(@QueryParam("type") String type) {
        if (type == null) return new ArrayList<>(DataStore.sensors.values());
        return DataStore.sensors.values().stream()
                .filter(s -> s.getType().equalsIgnoreCase(type))
                .collect(Collectors.toList());
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response addSensor(Sensor s) {
        if (!DataStore.rooms.containsKey(s.getRoomId())) {
            throw new LinkedResourceNotFoundException("Room not found.");
        }
        DataStore.sensors.put(s.getId(), s);
        DataStore.rooms.get(s.getRoomId()).getSensorIds().add(s.getId());
        return Response.status(201).entity(s).build();
    }

    @Path("/{sensorId}/readings")
    public SensorReadingResource getReadings(@PathParam("sensorId") String sid) {
        return new SensorReadingResource(sid);
    }
}