package org.westminster.api.resource;

import javax.ws.rs.*;
import javax.ws.rs.core.*;
import org.westminster.api.data.DataStore;
import org.westminster.api.model.Room;
import org.westminster.api.exception.RoomNotEmptyException;
import java.util.*;

@Path("/rooms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SensorRoomResource {

    // 1. GET ALL ROOMS
    @GET
    public List<Room> getRooms() {
        return new ArrayList<>(DataStore.rooms.values());
    }

    // 2. GET SINGLE ROOM (You were missing this!)
    @GET
    @Path("/{id}")
    public Response getRoomById(@PathParam("id") String id) {
        Room room = DataStore.rooms.get(id);
        if (room == null) {
            return Response.status(Response.Status.NOT_FOUND)
                           .entity("{\"error\": \"Room " + id + " not found\"}")
                           .build();
        }
        return Response.ok(room).build();
    }

    // 3. POST - ADD ROOM
    @POST
    public Response addRoom(Room room) {
        // Validation: Ensure ID isn't null
        if (room.getId() == null || room.getId().isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                           .entity("{\"error\": \"Room ID is required\"}")
                           .build();
        }
        
        DataStore.rooms.put(room.getId(), room);
        return Response.status(201).entity(room).build();
    }

    // 4. DELETE ROOM
    @DELETE
    @Path("/{id}")
    public Response deleteRoom(@PathParam("id") String id) {
        Room r = DataStore.rooms.get(id);
        
        // Check if exists
        if (r == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        // Your custom exception logic for Task 5
        if (!r.getSensorIds().isEmpty()) {
            throw new RoomNotEmptyException("Room " + id + " has sensors; cannot delete.");
        }
        
        DataStore.rooms.remove(id);
        return Response.noContent().build();
    }
}