package com.microcommerce.apigateway.filter;

import com.microcommerce.apigateway.util.JwtUtils;
import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
public class AuthenticationFilter extends AbstractGatewayFilterFactory<AuthenticationFilter.Config> {

    // Identity headers trusted by the downstream services. They are always
    // overwritten from the verified JWT so clients cannot spoof them.
    public static final String USER_ID_HEADER = "X-User-Id";
    public static final String USER_EMAIL_HEADER = "X-User-Email";
    public static final String USER_ROLE_HEADER = "X-User-Role";

    @Autowired
    private RouteValidator validator;

    @Autowired
    private JwtUtils jwtUtils;

    public AuthenticationFilter() {
        super(Config.class);
    }

    @Override
    public List<String> shortcutFieldOrder() {
        return List.of("writeRole");
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            if (!validator.isSecured.test(exchange.getRequest())) {
                return chain.filter(stripIdentityHeaders(exchange));
            }

            String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
            if (authHeader == null) {
                return reject(exchange, HttpStatus.UNAUTHORIZED, "Missing authorization header");
            }
            if (!authHeader.startsWith("Bearer ")) {
                return reject(exchange, HttpStatus.UNAUTHORIZED, "Authorization header must contain a Bearer token");
            }

            Claims claims;
            try {
                claims = jwtUtils.validateToken(authHeader.substring(7));
            } catch (Exception e) {
                // Return a proper 401 instead of throwing (which produced a 500).
                return reject(exchange, HttpStatus.UNAUTHORIZED, "Unauthorized access to application");
            }

            Object userId = claims.get("uid");
            String role = claims.get("role", String.class);
            if (userId == null || role == null || claims.getSubject() == null) {
                return reject(exchange, HttpStatus.UNAUTHORIZED, "Unauthorized access to application");
            }

            if (config.getWriteRole() != null && isWrite(exchange.getRequest().getMethod())
                    && !config.getWriteRole().equals(role)) {
                return reject(exchange, HttpStatus.FORBIDDEN, "Insufficient permissions");
            }

            ServerHttpRequest request = exchange.getRequest().mutate()
                    .headers(h -> {
                        h.set(USER_ID_HEADER, String.valueOf(userId));
                        h.set(USER_EMAIL_HEADER, claims.getSubject());
                        h.set(USER_ROLE_HEADER, role);
                    })
                    .build();
            return chain.filter(exchange.mutate().request(request).build());
        };
    }

    private static boolean isWrite(HttpMethod method) {
        return !(HttpMethod.GET.equals(method) || HttpMethod.HEAD.equals(method) || HttpMethod.OPTIONS.equals(method));
    }

    private static ServerWebExchange stripIdentityHeaders(ServerWebExchange exchange) {
        ServerHttpRequest request = exchange.getRequest().mutate()
                .headers(h -> {
                    h.remove(USER_ID_HEADER);
                    h.remove(USER_EMAIL_HEADER);
                    h.remove(USER_ROLE_HEADER);
                })
                .build();
        return exchange.mutate().request(request).build();
    }

    private Mono<Void> reject(ServerWebExchange exchange, HttpStatus status, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        byte[] bytes = ("{\"status\":" + status.value() + ",\"error\":\"" + status.getReasonPhrase()
                + "\",\"message\":\"" + message + "\"}")
                .getBytes(StandardCharsets.UTF_8);
        DataBuffer buffer = response.bufferFactory().wrap(bytes);
        return response.writeWith(Mono.just(buffer));
    }

    public static class Config {
        // When set, non-read requests (POST/PUT/PATCH/DELETE) require this role.
        private String writeRole;

        public String getWriteRole() {
            return writeRole;
        }

        public void setWriteRole(String writeRole) {
            this.writeRole = writeRole;
        }
    }
}
