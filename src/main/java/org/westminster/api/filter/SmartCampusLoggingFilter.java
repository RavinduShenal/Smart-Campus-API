package org.westminster.api.filter;

import javax.ws.rs.container.*;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.util.logging.Logger;

@Provider
public class SmartCampusLoggingFilter implements ContainerRequestFilter, ContainerResponseFilter {
    private static final Logger LOG = Logger.getLogger(SmartCampusLoggingFilter.class.getName());

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        // Log incoming Request
        LOG.info("Incoming Request: " + requestContext.getMethod() + " " + requestContext.getUriInfo().getPath());
    }

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
        // Log outgoing Response Status
        LOG.info("Outgoing Response Status: " + responseContext.getStatus());
    }
}