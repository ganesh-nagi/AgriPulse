package com.agripulse.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.agripulse.user.Role;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AuthServiceTest {

  @Autowired private AuthService authService;

  @Test
  void registerAndLoginWork() {
    RegisterRequest register = new RegisterRequest();
    register.setEmail("farmer@test.local");
    register.setPassword("password123");
    register.setRole(Role.FARMER);
    AuthResponse registered = authService.register(register);
    assertNotNull(registered.getToken());
    assertEquals("FARMER", registered.getRole());

    LoginRequest login = new LoginRequest();
    login.setEmail("farmer@test.local");
    login.setPassword("password123");
    AuthResponse loggedIn = authService.login(login);
    assertNotNull(loggedIn.getToken());
  }

  @Test
  void duplicateEmailIsRejected() {
    RegisterRequest register = new RegisterRequest();
    register.setEmail("dupe@test.local");
    register.setPassword("password123");
    register.setRole(Role.BUYER);
    authService.register(register);
    assertThrows(IllegalArgumentException.class, () -> authService.register(register));
  }

  @Test
  void adminCannotSelfRegister() {
    RegisterRequest register = new RegisterRequest();
    register.setEmail("admin@test.local");
    register.setPassword("password123");
    register.setRole(Role.ADMIN);
    assertThrows(AccessDeniedException.class, () -> authService.register(register));
  }

  @Test
  void wrongPasswordIsRejected() {
    RegisterRequest register = new RegisterRequest();
    register.setEmail("secure@test.local");
    register.setPassword("password123");
    register.setRole(Role.FARMER);
    authService.register(register);

    LoginRequest login = new LoginRequest();
    login.setEmail("secure@test.local");
    login.setPassword("wrong-password1");
    assertThrows(IllegalArgumentException.class, () -> authService.login(login));
  }

  @Test
  void weakPasswordIsRejected() {
    RegisterRequest register = new RegisterRequest();
    register.setEmail("weak@test.local");
    register.setPassword("short");
    register.setRole(Role.FARMER);
    assertThrows(IllegalArgumentException.class, () -> authService.register(register));
  }

  @Test
  void repeatedFailuresLockTheAccount() {
    RegisterRequest register = new RegisterRequest();
    register.setEmail("locked@test.local");
    register.setPassword("password123");
    register.setRole(Role.FARMER);
    authService.register(register);

    LoginRequest bad = new LoginRequest();
    bad.setEmail("locked@test.local");
    bad.setPassword("wrong-password1");
    for (int i = 0; i < 5; i++) {
      assertThrows(IllegalArgumentException.class, () -> authService.login(bad));
    }
    LoginRequest good = new LoginRequest();
    good.setEmail("locked@test.local");
    good.setPassword("password123");
    assertThrows(AccountLockedException.class, () -> authService.login(good));
  }

  @Test
  void refreshRotatesAndOldTokenIsRejected() {
    RegisterRequest register = new RegisterRequest();
    register.setEmail("refresh@test.local");
    register.setPassword("password123");
    register.setRole(Role.BUYER);
    AuthResponse first = authService.register(register);
    assertNotNull(first.getRefreshToken());

    AuthResponse second = authService.refresh(first.getRefreshToken());
    assertNotNull(second.getToken());
    assertThrows(
        IllegalArgumentException.class, () -> authService.refresh(first.getRefreshToken()));
  }

  @Test
  void logoutRevokesRefreshTokens() {
    RegisterRequest register = new RegisterRequest();
    register.setEmail("logout@test.local");
    register.setPassword("password123");
    register.setRole(Role.FARMER);
    AuthResponse auth = authService.register(register);

    authService.logout("logout@test.local", auth.getToken());
    assertThrows(
        IllegalArgumentException.class, () -> authService.refresh(auth.getRefreshToken()));
  }
}
