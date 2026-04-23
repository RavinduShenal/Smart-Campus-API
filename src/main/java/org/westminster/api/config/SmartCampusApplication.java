package org.westminster.api.config;

import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;
import javax.ws.rs.ApplicationPath;

@ApplicationPath("/api/v1")
public class SmartCampusApplication extends ResourceConfig {
    public SmartCampusApplication() {
        // This tells Jersey to look for your resources AND use Jackson for JSON
        packages("org.westminster.api.resource", "org.westminster.api.exception", "org.westminster.api.filter");
        register(JacksonFeature.class);
    }
}