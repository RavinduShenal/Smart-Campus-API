package org.westminster.api.resource;

import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.util.*;

@Path("/") 
public class DiscoveryResource {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getDiscovery() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("version", "v1");
        response.put("GPRS_Tharuka", "w2119843@westminster.ac.uk");
        
        Map<String, String> links = new HashMap<>();
        links.put("rooms", "/api/v1/rooms");
        links.put("sensors", "/api/v1/sensors");
        response.put("_links", links);

        return Response.ok(response).build();
    }
}