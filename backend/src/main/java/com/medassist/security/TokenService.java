package com.medassist.security;

import com.medassist.common.config.AppProperties;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

/**
 * Issues short-lived access tokens (signed JWT, HS256) and generates opaque random
 * secrets for refresh tokens, invitations and email links. Opaque secrets are only
 * ever stored as SHA-256 hashes, so a database leak does not leak usable tokens.
 */
@Service
public class TokenService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final JwtEncoder encoder;
    private final AppProperties.Jwt props;
    private final Clock clock;

    public TokenService(AppProperties app, Clock clock) {
        this.props = app.jwt();
        this.clock = clock;
        this.encoder = new NimbusJwtEncoder(new ImmutableSecret<>(secretKey(props.secret())));
    }

    static SecretKey secretKey(String secret) {
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException("app.jwt.secret must be at least 32 bytes");
        }
        return new SecretKeySpec(bytes, "HmacSHA256");
    }

    public AccessToken issueAccessToken(UUID userId, String email, String name, UUID orgId, Role role) {
        Instant now = clock.instant();
        Instant exp = now.plus(props.accessTokenTtl());
        JwtClaimsSet.Builder claims = JwtClaimsSet.builder()
                .issuer(props.issuer())
                .subject(userId.toString())
                .issuedAt(now)
                .expiresAt(exp)
                .id(UUID.randomUUID().toString())
                .claim("email", email)
                .claim("name", name);
        if (orgId != null && role != null) {
            claims.claim("org", orgId.toString());
            claims.claim("role", role.name());
        }
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = encoder.encode(JwtEncoderParameters.from(header, claims.build())).getTokenValue();
        return new AccessToken(token, exp);
    }

    /** 256 bits of randomness, URL-safe. */
    public String newOpaqueToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public static String sha256(String value) {
        try {
            var digest = java.security.MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    public record AccessToken(String value, Instant expiresAt) {
    }
}
