package com.microcommerce.apigateway.filter;

import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Predicate;

@Component
public class RouteValidator {

    // Exact endpoints reachable without a JWT. The previous list used a loose
    // `contains()` match and referenced a non-existent "/auth/login" route,
    // while the real endpoints live under /api/v1/auth.
    public static final List<String> openApiEndpoints = List.of(
            "/api/v1/auth/register",
            "/api/v1/auth/authenticate",
            "/eureka"
    );

    public Predicate<ServerHttpRequest> isSecured =
            request -> openApiEndpoints
                    .stream()
                    .noneMatch(uri -> {
                        String path = request.getURI().getPath();
                        return path.equals(uri) || path.startsWith(uri + "/");
                    });

}
