import { useState, useEffect, useCallback } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../../hooks/useAuth'
import { ApiError, apiGet, apiPost, apiPatch, apiPostCancel } from '../../services/api'
import type { SupplyReport, CropOption, ReportStatus } from '../../types'
import { tonnes } from '../../utils/format'
import React from 'react'

type Step = 1 | 2 | 3 | 4

const STEPS = ['Crop', 'Quantity', 'Dates & quality', 'Review']

interface FormErrors {
  crop?: string
  quantity?: string
  dates?: string
  region?: string
  general?: string
}

export default function ReportPage() {
  const navigate = useNavigate()

  const [step, setStep] = useState<Step>(1)
  const [submitting, setSubmitting] = useState(false)
  const [submitted, setSubmitted] = useState(false)
  const [lastReport, setLastReport] = useState<SupplyReport | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [formErrors, setFormErrors] = useState<FormErrors>({})
  const [crops, setCrops] = useState<CropOption[]>([])
  const [loadingCrops, setLoadingCrops] = useState(true)
  const [myReports, setMyReports] = useState<SupplyReport[]>([])

  const [cropId, setCropId] = useState<number | null>(null)
  const [qtyMin, setQtyMin] = useState('')
  const [qtyMax, setQtyMax] = useState('')
  const [harvestStart, setHarvestStart] = useState('')
  const [harvestEnd, setHarvestEnd] = useState('')
  const [quality, setQuality] = useState('')
  const [region, setRegion] = useState('')

  const fetchCrops = useCallback(async () => {
    try {
      setLoadingCrops(true)
      const list = await apiGet<CropOption[]>('/api/crops')
      setCrops(list)
    } catch {
      setCrops([])
    } finally {
      setLoadingCrops(false)
    }
  }, [])

  const fetchMyReports = useCallback(async () => {
    try {
      const list = await apiGet<SupplyReport[]>('/api/reports/mine')
      setMyReports(list)
    } catch {
      setMyReports([])
    }
  }, [])

  useEffect(() => {
    fetchCrops()
    fetchMyReports()
  }, [fetchCrops, fetchMyReports])

  function validateStep(s: Step): boolean {
    const errors: FormErrors = {}
    if (s === 1) {
      if (!cropId) errors.crop = 'Select a crop'
    }
    if (s === 2) {
      const min = parseFloat(qtyMin)
      const max = parseFloat(qtyMax)
      if (qtyMin === '' || isNaN(min) || min < 0) errors.quantity = 'Min must be 0 or more'
      else if (qtyMax === '' || isNaN(max) || max <= 0) errors.quantity = 'Max must be above 0'
      else if (min > max) errors.quantity = 'Min cannot exceed max'
    }
    if (s === 3) {
      if (!harvestStart || !harvestEnd) errors.dates = 'Both dates required'
      else if (harvestStart > harvestEnd) errors.dates = 'Start must be on or before end'
      if (!quality.trim()) errors.dates = 'Quality level required'
    }
    if (s === 4) {
      if (!region.trim()) errors.region = 'Region required'
    }
    setFormErrors(errors)
    return Object.keys(errors).length === 0
  }

  async function onSubmit() {
    setSubmitting(true)
    setError(null)
    try {
      if (lastReport) {
        const body: Record<string, unknown> = {
          quantityMinTonnes: parseFloat(qtyMin),
          quantityMaxTonnes: parseFloat(qtyMax),
          harvestStart,
          harvestEnd,
          quality,
        }
        const res = await apiPatch<SupplyReport>(`/api/reports/${lastReport.id}`, body)
        setLastReport(res)
        setMyReports((prev) => prev.map((r) => (r.id === res.id ? res : r)))
        setSubmitted(true)
      } else {
        const body = {
          cropId,
          quantityMinTonnes: parseFloat(qtyMin),
          quantityMaxTonnes: parseFloat(qtyMax),
          harvestStart,
          harvestEnd,
          quality,
          region: region.trim(),
        }
        const res = await apiPost<SupplyReport>('/api/reports', body)
        setLastReport(res)
        setMyReports((prev) => [...prev, res])
        setSubmitted(true)
      }
    } catch (err) {
      if (err instanceof ApiError) {
        setError(err.message)
      } else {
        setError('Could not submit report. Check your connection.')
      }
    } finally {
      setSubmitting(false)
    }
  }

  async function onCancel() {
    if (!lastReport) return
    setSubmitting(true)
    setError(null)
    try {
      const res = await apiPostCancel<SupplyReport>(`/api/reports/${lastReport.id}/cancel`)
      setMyReports((prev) => prev.map((r) => (r.id === res.id ? res : r)))
      resetForm()
    } catch (err) {
      if (err instanceof ApiError) setError(err.message)
      else setError('Could not cancel report')
    } finally {
      setSubmitting(false)
    }
  }

  function resetForm() {
    setStep(1)
    setCropId(null)
    setQtyMin('')
    setQtyMax('')
    setHarvestStart('')
    setHarvestEnd('')
    setQuality('')
    setRegion('')
    setLastReport(null)
    setSubmitted(false)
    setFormErrors({})
    setError(null)
  }

  function canProceed() {
    if (step === 1) return !!cropId
    if (step === 2) {
      const min = parseFloat(qtyMin)
      const max = parseFloat(qtyMax)
      return !isNaN(min) && !isNaN(max) && min >= 0 && max > 0 && min <= max
    }
    if (step === 3) return !!harvestStart && !!harvestEnd && harvestStart <= harvestEnd && !!quality.trim()
    if (step === 4) return !!region.trim()
    return false
  }

  function statusLabel(status: ReportStatus) {
    switch (status) {
      case 'SUBMITTED': return 'Submitted'
      case 'VALIDATED': return 'Validated'
      case 'REJECTED': return 'Rejected'
      case 'CANCELLED': return 'Cancelled'
      default: return status
    }
  }

  // Success screen
  if (submitted && lastReport) {
    return (
      <div className="space-y-4">
        <section className="rounded-2xl bg-white p-6 shadow-sm">
          <div className="text-center">
            <div className="mx-auto flex h-16 w-16 items-center justify-center rounded-full bg-green-100 text-green-700 text-2xl">
              ✓
            </div>
            <h1 className="mt-4 text-xl font-bold text-neutral-900">
              {lastReport.status === 'CANCELLED' ? 'Report cancelled' : 'Report submitted'}
            </h1>
            <p className="mt-2 text-sm text-neutral-600">
              {lastReport.status === 'CANCELLED'
                ? 'Your supply report has been withdrawn. You can create a new one anytime.'
                : `Your ${lastReport.cropName} supply report is in. We'll validate it and notify you.`}
            </p>
          </div>
          <dl className="mt-5 space-y-3 text-sm">
            <div className="flex justify-between">
              <dt className="text-neutral-500">Crop</dt>
              <dd className="font-semibold">{lastReport.cropName}</dd>
            </div>
            <div className="flex justify-between">
              <dt className="text-neutral-500">Quantity</dt>
              <dd className="font-semibold">
                {tonnes(lastReport.quantityMinTonnes)} – {tonnes(lastReport.quantityMaxTonnes)}
              </dd>
            </div>
            <div className="flex justify-between">
              <dt className="text-neutral-500">Region</dt>
              <dd className="font-semibold">{lastReport.region}</dd>
            </div>
            <div className="flex justify-between">
              <dt className="text-neutral-500">Confidence</dt>
              <dd className="font-semibold">{Math.round(lastReport.trustScore)}%</dd>
            </div>
            <div className="flex justify-between">
              <dt className="text-neutral-500">Status</dt>
              <dd>
                <span className="inline-flex items-center rounded-full border px-2.5 py-0.5 text-xs font-medium bg-neutral-100 text-neutral-700">
                  {statusLabel(lastReport.status)}
                </span>
              </dd>
            </div>
          </dl>
        </section>

        <section className="rounded-2xl bg-primary-50 p-5">
          <h2 className="text-sm font-semibold text-primary-900">Your data stays yours</h2>
          <p className="mt-1 text-xs text-primary-700">
            Your supply report feeds aggregated regional intelligence only. Exact farm location
            and personal details are never shared with other farmers or buyers — only regional
            ranges and confidence scores appear publicly.
          </p>
        </section>

        <button
          onClick={() => void resetForm()}
          className="w-full rounded-xl border border-primary-700 px-4 py-3 text-center font-semibold text-primary-700"
        >
          {lastReport.status === 'CANCELLED' ? 'Create new report' : 'Submit another report'}
        </button>
      </div>
    )
  }

  // Edit/continue existing report
  const editing = !!lastReport

  return (
    <div className="space-y-4">
      <section className="rounded-2xl bg-white p-6 shadow-sm">
        <h1 className="text-xl font-bold text-neutral-900">
          {editing ? 'Update your report' : 'Report your harvest'}
        </h1>
        <p className="mt-1 text-sm text-neutral-500">
          {editing
            ? `Report #${lastReport.id} · ${lastReport.cropName}`
            : 'Tell the region what you plan to bring to market'}
        </p>

        {/* Step indicators */}
        <div className="mt-5 flex items-center gap-1" role="tablist" aria-label="Form steps">
          {STEPS.map((label, i) => {
            const idx = (i + 1) as Step
            const active = step === idx
            const done = step > idx
            return (
              <>
                <button
                  type="button"
                  role="tab"
                  aria-selected={active}
                  onClick={() => {
                    if (done || idx <= step) setStep(idx)
                  }}
                  className={`flex items-center gap-1.5 rounded-full px-3 py-1.5 text-xs font-medium ${
                    active
                      ? 'bg-primary-700 text-white'
                      : done
                        ? 'bg-green-100 text-green-700'
                        : 'bg-neutral-200 text-neutral-500'
                  }`}
                >
                  {done && !active ? '✓' : idx} {label}
                </button>
                {i < STEPS.length - 1 && (
                  <div
                    className={`h-1 w-8 rounded ${step > idx ? 'bg-green-400' : 'bg-neutral-200'}`}
                    aria-hidden="true"
                  />
                )}
              </>
            )
          })}
        </div>

        {/* Step 1: Crop */}
        {step === 1 && (
          <div className="mt-5 space-y-4">
            <label className="block">
              <span className="text-sm text-neutral-600">What are you growing?</span>
              {loadingCrops ? (
                <p className="mt-1 text-sm text-neutral-400">Loading crops…</p>
              ) : crops.length === 0 ? (
                <p className="mt-1 text-sm text-red-600">No crops available. Check back later.</p>
              ) : (
                <select
                  value={cropId ?? ''}
                  onChange={(e) => setCropId(Number(e.target.value) || null)}
                  className="mt-1 w-full rounded-xl border border-neutral-300 bg-white px-4 py-3 text-neutral-900"
                >
                  <option value="">Choose a crop</option>
                  {crops.map((c) => (
                    <option key={c.id} value={c.id}>
                      {c.name}
                    </option>
                  ))}
                </select>
              )}
            </label>
            {formErrors.crop && (
              <p role="alert" className="text-sm text-red-600">{formErrors.crop}</p>
            )}
          </div>
        )}

        {/* Step 2: Quantity */}
        {step === 2 && (
          <div className="mt-5 space-y-4">
            <p className="text-sm text-neutral-500">Enter your quantity range in tonnes</p>
            <label className="block">
              <span className="text-sm text-neutral-600">Minimum</span>
              <input
                type="number"
                min="0"
                step="0.1"
                value={qtyMin}
                onChange={(e) => setQtyMin(e.target.value)}
                placeholder="e.g. 5"
                className="mt-1 w-full rounded-xl border border-neutral-300 px-4 py-3 text-neutral-900"
              />
            </label>
            <label className="block">
              <span className="text-sm text-neutral-600">Maximum</span>
              <input
                type="number"
                min="0.1"
                step="0.1"
                value={qtyMax}
                onChange={(e) => setQtyMax(e.target.value)}
                placeholder="e.g. 15"
                className="mt-1 w-full rounded-xl border border-neutral-300 px-4 py-3 text-neutral-900"
              />
            </label>
            {formErrors.quantity && (
              <p role="alert" className="text-sm text-red-600">{formErrors.quantity}</p>
            )}
          </div>
        )}

        {/* Step 3: Dates & Quality */}
        {step === 3 && (
          <div className="mt-5 space-y-4">
            <label className="block">
              <span className="text-sm text-neutral-600">Harvest start</span>
              <input
                type="date"
                value={harvestStart}
                onChange={(e) => setHarvestStart(e.target.value)}
                className="mt-1 w-full rounded-xl border border-neutral-300 px-4 py-3 text-neutral-900"
              />
            </label>
            <label className="block">
              <span className="text-sm text-neutral-600">Harvest end</span>
              <input
                type="date"
                value={harvestEnd}
                onChange={(e) => setHarvestEnd(e.target.value)}
                className="mt-1 w-full rounded-xl border border-neutral-300 px-4 py-3 text-neutral-900"
              />
            </label>
            <label className="block">
              <span className="text-sm text-neutral-600">Quality level</span>
              <select
                value={quality}
                onChange={(e) => setQuality(e.target.value)}
                className="mt-1 w-full rounded-xl border border-neutral-300 bg-white px-4 py-3 text-neutral-900"
              >
                <option value="">Select quality</option>
                <option value="Excellent">Excellent</option>
                <option value="Good">Good</option>
                <option value="Fair">Fair</option>
                <option value="Standard">Standard</option>
              </select>
            </label>
            {formErrors.dates && (
              <p role="alert" className="text-sm text-red-600">{formErrors.dates}</p>
            )}
          </div>
        )}

        {/* Step 4: Region & Review */}
        {step === 4 && (
          <div className="mt-5 space-y-4">
            <label className="block">
              <span className="text-sm text-neutral-600">Region / village cluster</span>
              <input
                value={region}
                onChange={(e) => setRegion(e.target.value)}
                placeholder="e.g. Nashik"
                className="mt-1 w-full rounded-xl border border-neutral-300 px-4 py-3 text-neutral-900"
              />
            </label>
            {formErrors.region && (
              <p role="alert" className="text-sm text-red-600">{formErrors.region}</p>
            )}

            {/* Review summary */}
            <div className="rounded-xl bg-neutral-50 p-4 space-y-2 text-sm">
              <h3 className="font-semibold text-neutral-900">Review before submitting</h3>
              <div className="flex justify-between text-neutral-600">
                <span>Crop</span>
                <span className="font-medium text-neutral-900">
                  {crops.find((c) => c.id === cropId)?.name ?? '—'}
                </span>
              </div>
              <div className="flex justify-between text-neutral-600">
                <span>Quantity</span>
                <span className="font-medium text-neutral-900">
                  {tonnes(parseFloat(qtyMin) || 0)} – {tonnes(parseFloat(qtyMax) || 0)}
                </span>
              </div>
              <div className="flex justify-between text-neutral-600">
                <span>Harvest</span>
                <span className="font-medium text-neutral-900">
                  {harvestStart} – {harvestEnd}
                </span>
              </div>
              <div className="flex justify-between text-neutral-600">
                <span>Quality</span>
                <span className="font-medium text-neutral-900">{quality || '—'}</span>
              </div>
              <div className="flex justify-between text-neutral-600">
                <span>Region</span>
                <span className="font-medium text-neutral-900">{region || '—'}</span>
              </div>
            </div>
          </div>
        )}

        {/* Navigation buttons */}
        <div className="mt-5 flex items-center gap-3">
          {step > 1 && !editing && (
            <button
              type="button"
              onClick={() => setStep((step - 1) as Step)}
              className="rounded-xl border border-neutral-300 px-5 py-3 font-semibold text-neutral-700"
            >
              Back
            </button>
          )}
          {editing && step > 1 && (
            <button
              type="button"
              onClick={() => setStep((step - 1) as Step)}
              className="rounded-xl border border-neutral-300 px-5 py-3 font-semibold text-neutral-700"
            >
              Back
            </button>
          )}
          <div className="flex-1" />
          {step < 4 ? (
            <button
              type="button"
              onClick={() => {
                if (validateStep(step)) setStep((step + 1) as Step)
              }}
              disabled={!canProceed()}
              className={`rounded-xl px-5 py-3 font-semibold text-white ${
                canProceed() ? 'bg-primary-700' : 'bg-neutral-300'
              }`}
            >
              Next
            </button>
          ) : (
            <button
              type="button"
              onClick={() => {
                if (validateStep(step)) onSubmit()
              }}
              disabled={submitting || !canProceed()}
              className={`rounded-xl px-5 py-3 font-semibold text-white ${
                submitting || !canProceed() ? 'bg-neutral-300' : 'bg-green-700'
              }`}
            >
              {submitting ? 'Submitting…' : editing ? 'Update report' : 'Submit report'}
            </button>
          )}
        </div>

        {/* Cancel button when editing an active report */}
        {editing && lastReport.status !== 'CANCELLED' && lastReport.status !== 'VALIDATED' && (
          <button
            type="button"
            onClick={() => void onCancel()}
            disabled={submitting}
            className="mt-3 w-full rounded-xl bg-red-50 px-4 py-3 font-semibold text-red-700"
          >
            Cancel this report
          </button>
        )}

        {/* General error */}
        {error && (
          <p role="alert" className="mt-3 text-center text-sm text-red-600">
            {error}
          </p>
        )}
      </section>

      {/* My reports list */}
      <section className="rounded-2xl bg-white p-6 shadow-sm">
        <h2 className="text-lg font-bold text-neutral-900">My reports</h2>
        {myReports.length === 0 ? (
          <p className="mt-2 text-sm text-neutral-400">No reports yet. Submit your first above.</p>
        ) : (
          <ul className="mt-3 space-y-2">
            {myReports.map((r) => (
              <li
                key={r.id}
                className="flex items-center justify-between rounded-xl border border-neutral-200 p-3"
              >
                <div>
                  <p className="font-semibold text-neutral-900">{r.cropName}</p>
                  <p className="text-xs text-neutral-500">
                    {tonnes(r.quantityMinTonnes)} – {tonnes(r.quantityMaxTonnes)} · {r.region}
                  </p>
                </div>
                <div className="flex items-center gap-2">
                  {r.status === 'VALIDATED' && (
                    <span className="text-xs font-semibold text-green-700">
                      {Math.round(r.trustScore)}%
                    </span>
                  )}
                  <span className="rounded-full bg-neutral-100 px-2 py-0.5 text-xs text-neutral-600">
                    {statusLabel(r.status)}
                  </span>
                </div>
              </li>
            ))}
          </ul>
        )}
      </section>
    </div>
  )
}
