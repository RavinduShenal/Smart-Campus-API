package org.westminster.api.resource;

import javax.ws.rs.*;
import javax.ws.rs.core.*;
import org.westminster.api.data.DataStore;
import org.westminster.api.model.Room;
import org.westminster.api.exception.RoomNotEmptyException;
import java.util.*;

@Path("/rooms")
public class SensorRoomResource {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<Room> getRooms() {
        return new ArrayList<>(DataStore.rooms.values());
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response addRoom(Room room) {
        DataStore.rooms.put(room.getId(), room);
        return Response.status(201).entity(room).build();
    }

    @DELETE
    @Path("/{id}")
    public Response deleteRoom(@PathParam("id") String id) {
        Room r = DataStore.rooms.get(id);
        if (r != null && !r.getSensorIds().isEmpty()) {
            throw new RoomNotEmptyException("Room has sensors; cannot delete.");
        }
        DataStore.rooms.remove(id);
        return Response.noContent().build();
    }
}