# Stage 1: Build Keycloak with captcha provider
FROM quay.io/keycloak/keycloak:26.0.2 AS builder

WORKDIR /opt/keycloak

# Copy SPI JAR (contains captcha provider)
COPY target/keycloak-remiges-spi-22.0.7.jar /opt/keycloak/providers/

# Build Keycloak with providers
RUN /opt/keycloak/bin/kc.sh build

# Stage 2: Runtime image
FROM quay.io/keycloak/keycloak:26.0.2

# Copy the optimized build from builder stage
COPY --from=builder /opt/keycloak/ /opt/keycloak/

# Use the standard Keycloak entrypoint
ENTRYPOINT ["/opt/keycloak/bin/kc.sh"]
CMD ["start", "--optimized"]

