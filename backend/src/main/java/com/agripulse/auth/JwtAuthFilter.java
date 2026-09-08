package com.agripulse.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** Stateless JWT filter: validates Bearer tokens and sets the security context. */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

  private final JwtService jwtService;
  private final CustomUserDetailsService userDetailsService;
  private final TokenDenylist tokenDenylist;

  public JwtAuthFilter(
      JwtService jwtService,
      CustomUserDetailsService userDetailsService,
      TokenDenylist tokenDenylist) {
    this.jwtService = jwtService;
    this.userDetailsService = userDetailsService;
    this.tokenDenylist = tokenDenylist;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    String header = request.getHeader("Authorization");
    if (header != null && header.startsWith("Bearer ")) {
      String token = header.substring(7);
      jwtService
          .validateAndExtractIdentity(token)
          .filter(identity -> !tokenDenylist.isDenied(identity.jti()))
          .ifPresent(
              identity -> {
                if (SecurityContextHolder.getContext().getAuthentication() == null) {
                  try {
                    UserDetails details =
                        userDetailsService.loadUserByUsername(identity.email());
                    UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(
                            details.getUsername(), null, details.getAuthorities());
                    SecurityContextHolder.getContext().setAuthentication(auth);
                  } catch (Exception ex) {
                    SecurityContextHolder.clearContext();
                  }
                }
              });
    }
    chain.doFilter(request, response);
  }
}
