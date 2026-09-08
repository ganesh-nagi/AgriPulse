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

export type ReportStatus = 'DRAFT' | 'SUBMITTED' | 'VALIDATED' | 'REJECTED' | 'CANCELLED'

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
  trustLevel?: string
}

export interface CropOption {
  id: number
  name: string
  unit: string
}

export type PressureBand = 'LOW' | 'MODERATE' | 'HIGH' | 'CRITICAL'

/** Every demand/supply figure shown in the UI must carry one of these labels. */
export type DataSource = 'REAL' | 'SIMULATED' | 'ESTIMATED'

export interface Farm {
  id: number
  name: string
  region: string
  areaAcres: number | null
}

export interface PressureReason {
  component: string
  message: string
}

export interface PressureAssessment {
  band: PressureBand
  effectiveSupplyTonnes: number
  estimatedMinTonnes: number
  estimatedMaxTonnes: number
  reasons: PressureReason[]
}

export interface DemandProvenance {
  signal: string
  sources: DataSource[]
  detail: string
}

export interface DemandEstimate {
  cropName: string
  region: string
  windowStart: string
  windowEnd: string
  confirmedDemandTonnes: number
  confirmedBuyerCount: number
  absorptionMinTonnes: number
  absorptionMaxTonnes: number
  estimatedMinTonnes: number
  estimatedMaxTonnes: number
  confidence: number
  dataSource: DataSource
  provenance: DemandProvenance[]
}

export interface ResourceSnapshot {
  region: string
  cropName: string
  date: string
  storageAvailableTonnes: number
  transportAvailableTonnes: number
  processingAvailableTonnes: number
  marketAbsorptionMinTonnes: number
  marketAbsorptionMaxTonnes: number
}

export interface MarketNode {
  id: number
  name: string
  region: string
  absorptionMinTonnes: number
  absorptionMaxTonnes: number
  marketType: string | null
  latitude: number | null
  longitude: number | null
}

export interface ScenarioResult {
  label: string
  baseline: PressureAssessment
  scenario: PressureAssessment
}

export type GeoKind = 'MARKET' | 'STORAGE' | 'PROCESSING'

export interface GeoNode {
  kind: GeoKind
  id: number
  name: string
  region: string
  latitude: number
  longitude: number
  distanceKm: number
  summary: string
  availableTonnes: number | null
}

export interface BuyerRequirement {
  id: number
  cropName: string
  quantityTonnes: number
  quality: string | null
  requiredDate: string
  region: string
  status: string
  dataSource: DataSource
}

export interface RegionalSupply {
  region: string
  cropName: string
  reportCount: number
  quantityMinTotal: number
  quantityMaxTotal: number
  averageConfidence: number
  dataSource: DataSource
}

export interface StorageFacility {
  id: number
  name: string
  region: string
  capacityTonnes: number
  occupiedTonnes: number
  availableTonnes: number
}

export interface TransportResource {
  id: number
  capacityTonnes: number
  originRegion: string
  destRegion: string | null
  status: 'AVAILABLE' | 'BOOKED' | 'IN_TRANSIT' | 'MAINTENANCE'
}

export interface FpoMember {
  farmerUserId: number
  farmerName: string
  region: string
  verificationStatus: string
  onboardingState: string
  fpoValidated: boolean
  reportCount: number
}

export interface AdminOverview {
  totalUsers: number
  usersByRole: Record<string, number>
  reportsByStatus: Record<string, number>
  averageTrustScore: number
  openRequirements: number
  storageFacilities: number
  transportResources: number
}
