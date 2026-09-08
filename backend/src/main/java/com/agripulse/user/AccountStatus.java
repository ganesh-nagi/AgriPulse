package com.agripulse.user;

/**
 * Login gate. LOCKED is temporary (too many failed attempts); DISABLED is
 * administrative and never clears itself.
 */
public enum AccountStatus {
  ACTIVE,
  LOCKED,
  DISABLED
}
