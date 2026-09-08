package com.agripulse.auth;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * In-memory logout list for access tokens (jti → expiry epoch ms).
 * Single-instance MVP: a multi-instance deployment needs Redis here instead.
 */
@Component
public class TokenDenylist {

  private final Map<String, Long> denied = new ConcurrentHashMap<>();

  public void deny(String jti, long expiresAtMs) {
    evictExpired();
    denied.put(jti, expiresAtMs);
  }

  public boolean isDenied(String jti) {
    Long expiry = denied.get(jti);
    if (expiry == null) {
      return false;
    }
    if (expiry < System.currentTimeMillis()) {
      denied.remove(jti);
      return false;
    }
    return true;
  }

  private void evictExpired() {
    long now = System.currentTimeMillis();
    denied.entrySet().removeIf(e -> e.getValue() < now);
  }
}
