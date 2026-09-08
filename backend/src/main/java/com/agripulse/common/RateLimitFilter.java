package com.agripulse.common;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Simple in-memory fixed-window rate limiter for sensitive endpoints
 * (auth, verification). Keyed by client IP + endpoint group.
 *
 * <p>MVP scope: single instance. A multi-instance deployment should replace
 * this with a shared store (Redis) or a library such as Bucket4j.
 */
@Component
@Order(1)
public class RateLimitFilter extends OncePerRequestFilter {

  private final int maxRequests;
  private final long windowMs;
  private final Map<String, Window> windows = new ConcurrentHashMap<>();

  public RateLimitFilter(
      @Value("${app.rate-limit.max-requests:20}") int maxRequests,
      @Value("${app.rate-limit.window-ms:60000}") long windowMs) {
    this.maxRequests = maxRequests;
    this.windowMs = windowMs;
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    String path = request.getRequestURI();
    return !path.startsWith("/api/auth/")
        && !path.startsWith("/api/verification/")
        && !path.startsWith("/api/fpo/validations");
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    String key = clientIp(request) + "|" + request.getRequestURI();
    long now = System.currentTimeMillis();
    Window window = windows.computeIfAbsent(key, k -> new Window(now));
    synchronized (window) {
      if (now - window.startMs >= windowMs) {
        window.startMs = now;
        window.count = 0;
      }
      window.count++;
      if (window.count > maxRequests) {
        response.setStatus(429);
        response.setContentType("application/json");
        response
            .getWriter()
            .write("{\"status\":429,\"message\":\"Too many requests, try again later\"}");
        return;
      }
    }
    if (windows.size() > 10_000) {
      windows.clear();
    }
    chain.doFilter(request, response);
  }

  private String clientIp(HttpServletRequest request) {
    String forwarded = request.getHeader("X-Forwarded-For");
    if (forwarded != null && !forwarded.isBlank()) {
      return forwarded.split(",")[0].trim();
    }
    return request.getRemoteAddr();
  }

  private static final class Window {
    long startMs;
    int count;

    Window(long startMs) {
      this.startMs = startMs;
    }
  }
}
