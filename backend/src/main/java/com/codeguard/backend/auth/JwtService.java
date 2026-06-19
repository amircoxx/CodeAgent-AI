package com.codeguard.backend.auth;

import com.codeguard.backend.config.JwtProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

  private static final Base64.Encoder BASE64_URL_ENCODER = Base64.getUrlEncoder().withoutPadding();
  private static final Base64.Decoder BASE64_URL_DECODER = Base64.getUrlDecoder();
  private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
  };

  private final JwtProperties properties;
  private final ObjectMapper objectMapper;

  public JwtService(JwtProperties properties, ObjectMapper objectMapper) {
    this.properties = properties;
    this.objectMapper = objectMapper;
  }

  public String generateToken(String email) {
    long now = Instant.now().toEpochMilli();
    Map<String, Object> header = Map.of(
        "alg", "HS256",
        "typ", "JWT"
    );
    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("sub", email);
    payload.put("iat", now / 1000);
    payload.put("exp", (now + properties.expirationMs()) / 1000);

    String headerSegment = encodeJson(header);
    String payloadSegment = encodeJson(payload);
    String unsignedToken = headerSegment + "." + payloadSegment;
    return unsignedToken + "." + sign(unsignedToken);
  }

  public boolean isTokenValid(String token) {
    try {
      String[] segments = splitToken(token);
      String unsignedToken = segments[0] + "." + segments[1];
      if (!constantTimeEquals(sign(unsignedToken), segments[2])) {
        return false;
      }

      Map<String, Object> payload = parsePayload(segments[1]);
      Object expiresAt = payload.get("exp");
      if (!(expiresAt instanceof Number number)) {
        return false;
      }
      return Instant.now().getEpochSecond() < number.longValue();
    } catch (RuntimeException exception) {
      return false;
    }
  }

  public String extractSubject(String token) {
    String[] segments = splitToken(token);
    Map<String, Object> payload = parsePayload(segments[1]);
    Object subject = payload.get("sub");
    if (!(subject instanceof String email) || email.isBlank()) {
      throw new IllegalArgumentException("JWT subject is missing");
    }
    return email;
  }

  private String encodeJson(Map<String, Object> value) {
    try {
      return BASE64_URL_ENCODER.encodeToString(objectMapper.writeValueAsBytes(value));
    } catch (Exception exception) {
      throw new IllegalStateException("Could not encode JWT", exception);
    }
  }

  private Map<String, Object> parsePayload(String payloadSegment) {
    try {
      byte[] payload = BASE64_URL_DECODER.decode(payloadSegment);
      return objectMapper.readValue(payload, MAP_TYPE);
    } catch (Exception exception) {
      throw new IllegalArgumentException("Invalid JWT payload", exception);
    }
  }

  private String sign(String unsignedToken) {
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(properties.secret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
      return BASE64_URL_ENCODER.encodeToString(mac.doFinal(unsignedToken.getBytes(StandardCharsets.UTF_8)));
    } catch (Exception exception) {
      throw new IllegalStateException("Could not sign JWT", exception);
    }
  }

  private String[] splitToken(String token) {
    String[] segments = token.split("\\.");
    if (segments.length != 3) {
      throw new IllegalArgumentException("JWT must contain three segments");
    }
    return segments;
  }

  private boolean constantTimeEquals(String expected, String actual) {
    byte[] expectedBytes = expected.getBytes(StandardCharsets.UTF_8);
    byte[] actualBytes = actual.getBytes(StandardCharsets.UTF_8);
    if (expectedBytes.length != actualBytes.length) {
      return false;
    }

    int result = 0;
    for (int index = 0; index < expectedBytes.length; index++) {
      result |= expectedBytes[index] ^ actualBytes[index];
    }
    return result == 0;
  }
}
