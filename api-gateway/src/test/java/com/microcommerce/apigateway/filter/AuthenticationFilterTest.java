package com.microcommerce.apigateway.filter;

import com.microcommerce.apigateway.util.JwtUtils;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Date;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class AuthenticationFilterTest {

    private static final String SECRET = "c2VjcmV0LXNlY3JldC1zZWNyZXQtc2VjcmV0LXNlY3JldC1zZWNyZXQ=";

    private final AtomicReference<ServerWebExchange> forwarded = new AtomicReference<>();
    private final GatewayFilterChain chain = exchange -> {
        forwarded.set(exchange);
        return Mono.empty();
    };
    private AuthenticationFilter filterFactory;

    @BeforeEach
    void setUp() {
        JwtUtils jwtUtils = new JwtUtils();
        ReflectionTestUtils.setField(jwtUtils, "secret", SECRET);
        filterFactory = new AuthenticationFilter();
        ReflectionTestUtils.setField(filterFactory, "jwtUtils", jwtUtils);
        ReflectionTestUtils.setField(filterFactory, "validator", new RouteValidator());
    }

    @Test
    void rejectsMissingToken() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/orders/1"));

        run(filter(null), exchange);

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(forwarded.get()).isNull();
    }

    @Test
    void rejectsTokenSignedWithAnotherKey() {
        String token = Jwts.builder().setSubject("a@example.com").claim("uid", 5).claim("role", "CUSTOMER")
                .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(
                        "b3RoZXItb3RoZXItb3RoZXItb3RoZXItb3RoZXItb3RoZXItb3RoZXI=")), SignatureAlgorithm.HS256)
                .compact();

        MockServerWebExchange exchange = exchange(MockServerHttpRequest.get("/api/v1/orders/1"), token);
        run(filter(null), exchange);

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(forwarded.get()).isNull();
    }

    @Test
    void replacesSpoofedIdentityHeadersWithTokenClaims() {
        MockServerHttpRequest.BaseBuilder<?> request = MockServerHttpRequest.get("/api/v1/orders/1")
                .header(AuthenticationFilter.USER_ID_HEADER, "1")
                .header(AuthenticationFilter.USER_ROLE_HEADER, "ADMIN");

        run(filter(null), exchange(request, token("CUSTOMER")));

        HttpHeaders headers = forwarded.get().getRequest().getHeaders();
        assertThat(headers.get(AuthenticationFilter.USER_ID_HEADER)).containsExactly("5");
        assertThat(headers.get(AuthenticationFilter.USER_ROLE_HEADER)).containsExactly("CUSTOMER");
        assertThat(headers.getFirst(AuthenticationFilter.USER_EMAIL_HEADER)).isEqualTo("a@example.com");
    }

    @Test
    void writeRoleBlocksCustomersFromWriting() {
        MockServerWebExchange exchange = exchange(MockServerHttpRequest.post("/api/v1/products"), token("CUSTOMER"));

        run(filter("ADMIN"), exchange);

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(forwarded.get()).isNull();
    }

    @Test
    void writeRoleStillAllowsCustomersToRead() {
        run(filter("ADMIN"), exchange(MockServerHttpRequest.get("/api/v1/products"), token("CUSTOMER")));

        assertThat(forwarded.get()).isNotNull();
    }

    @Test
    void openEndpointsStripIdentityHeaders() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.post("/api/v1/auth/register")
                .header(AuthenticationFilter.USER_ID_HEADER, "1"));

        run(filter(null), exchange);

        assertThat(forwarded.get().getRequest().getHeaders().containsKey(AuthenticationFilter.USER_ID_HEADER)).isFalse();
    }

    private GatewayFilter filter(String writeRole) {
        AuthenticationFilter.Config config = new AuthenticationFilter.Config();
        config.setWriteRole(writeRole);
        return filterFactory.apply(config);
    }

    private void run(GatewayFilter filter, ServerWebExchange exchange) {
        filter.filter(exchange, chain).block();
    }

    private static MockServerWebExchange exchange(MockServerHttpRequest.BaseBuilder<?> request, String token) {
        return MockServerWebExchange.from(request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token));
    }

    private static String token(String role) {
        return Jwts.builder()
                .setSubject("a@example.com")
                .claim("uid", 5)
                .claim("role", role)
                .setExpiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRET)), SignatureAlgorithm.HS256)
                .compact();
    }
}
