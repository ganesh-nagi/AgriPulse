import { z } from 'zod'

/**
 * Client-side shape of a supply report. Mirrors backend validation;
 * the server re-validates everything (never trust the client).
 */
export const reportSchema = z
  .object({
    farmId: z.number().int().positive(),
    cropId: z.number().int().positive(),
    quantityMinTonnes: z.number().min(0),
    quantityMaxTonnes: z.number().min(0),
    harvestStart: z.string().min(1, 'Pick a start date'),
    harvestEnd: z.string().min(1, 'Pick an end date'),
    quality: z.string().optional(),
    region: z.string().min(1, 'Enter your region'),
  })
  .refine((v) => v.quantityMinTonnes <= v.quantityMaxTonnes, {
    message: 'Minimum must be below maximum',
    path: ['quantityMinTonnes'],
  })
  .refine((v) => v.harvestStart <= v.harvestEnd, {
    message: 'Start must be before end',
    path: ['harvestStart'],
  })

export type ReportFormValues = z.infer<typeof reportSchema>
