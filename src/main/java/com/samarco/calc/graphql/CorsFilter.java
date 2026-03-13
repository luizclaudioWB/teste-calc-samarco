package com.samarco.calc.graphql;

import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import io.quarkus.vertx.http.runtime.filters.Filters;

@ApplicationScoped
public class CorsFilter {

    public void registerFilters(@Observes Filters filters) {
        filters.register(rc -> {
            HttpServerResponse response = rc.response();
            HttpServerRequest request = rc.request();
            String origin = request.getHeader("Origin");
            if (origin != null) {
                response.putHeader("Access-Control-Allow-Origin", origin);
                response.putHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
                response.putHeader("Access-Control-Allow-Headers", "Content-Type, Accept");
            }
            if ("OPTIONS".equalsIgnoreCase(request.method().name())) {
                response.setStatusCode(204).end();
            } else {
                rc.next();
            }
        }, 100);
    }
}
