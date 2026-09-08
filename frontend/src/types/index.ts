/** Shared API contracts. Mirror the backend DTOs; no business logic here. */

export type UserRole =
  | 'FARMER'
  | 'BUYER'
  | 'FPO'
  | 'STORAGE_OPERATOR'
  | 'TRANSPORTER'
  | 'ADMIN'

export interface AuthResponse {
  token: string
  refreshToken: string
  email: string
  role: UserRole
}

export type OnboardingState =
  | 'ACCOUNT_CREATED'
  | 'PHONE_VERIFICATION_PENDING'
  | 'PHONE_VERIFIED'
  | 'IDENTITY_VERIFICATION_PENDING'
  | 'IDENTITY_VERIFIED'
  | 'REQUIRES_REVIEW'
  | 'FARM_REGION_VERIFICATION'
  | 'PROFILE_ACTIVE'

export interface OnboardingStatus {
  state: OnboardingState
  phoneVerified: boolean
  identityStatus: string
  fpoValidated: boolean
  regionConsistent: boolean | null
  evidenceCount: number
}

export type ReportStatus = 'DRAFT' | 'SUBMITTED' | 'VALIDATED' | 'REJECTED'

export interface SupplyReport {
  id: number
  farmId: number
  cropName: string
  quantityMinTonnes: number
  quantityMaxTonnes: number
  harvestStart: string
  harvestEnd: string
  quality: string | null
  region: string
  status: ReportStatus
  trustScore: number
}

export type PressureBand = 'LOW' | 'MODERATE' | 'HIGH' | 'CRITICAL'

/** Every demand/supply figure shown in the UI must carry one of these labels. */
export type DataSource = 'REAL' | 'SIMULATED' | 'ESTIMATED'
