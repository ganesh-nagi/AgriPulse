package com.agripulse.user;

/** Application roles. Used for JWT claims and method-level authorization. */
public enum Role {
  FARMER,
  BUYER,
  FPO,
  STORAGE_OPERATOR,
  TRANSPORTER,
  ADMIN
}
