package org.westminster.api.exception;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.util.HashMap;
import java.util.Map;

@Provider
public class DependencyMapper implements ExceptionMapper<LinkedResourceNotFoundException> {
    @Override
    public Response toResponse(LinkedResourceNotFoundException ex) {
        Map<String, String> error = new HashMap<>();
        error.put("error", "Unprocessable Entity");
        error.put("message", ex.getMessage());
        // Returning 422 as it's more semantically accurate for missing references
        return Response.status(422).entity(error).build();
    }
}