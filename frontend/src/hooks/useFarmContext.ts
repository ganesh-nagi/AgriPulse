import { useCallback, useEffect, useState } from 'react'
import { getCrops, getFarms } from '../services/farmer'
import type { CropOption, Farm } from '../types'

const CROP_KEY = 'agripulse.crop'

/** Shared farmer context: farms (for region), crops, selected crop. */
export function useFarmContext() {
  const [farms, setFarms] = useState<Farm[]>([])
  const [crops, setCrops] = useState<CropOption[]>([])
  const [cropId, setCropIdState] = useState<number | null>(() => {
    const saved = Number(localStorage.getItem(CROP_KEY))
    return Number.isFinite(saved) && saved > 0 ? saved : null
  })
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const load = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const [farmList, cropList] = await Promise.all([getFarms(), getCrops()])
      setFarms(farmList)
      setCrops(cropList)
      if (cropList.length > 0) {
        const saved = Number(localStorage.getItem(CROP_KEY))
        const valid = cropList.some((c) => c.id === saved)
        const chosen = valid ? saved : cropList[0].id
        localStorage.setItem(CROP_KEY, String(chosen))
        setCropIdState(chosen)
      }
    } catch {
      setError('load')
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    void load()
  }, [load])

  const setCropId = useCallback(
    (id: number) => {
      localStorage.setItem(CROP_KEY, String(id))
      setCropIdState(id)
    },
    [],
  )

  const region = farms.length > 0 ? farms[0].region : null
  const cropName = crops.find((c) => c.id === cropId)?.name ?? null

  return { farms, region, crops, cropId, cropName, setCropId, loading, error, reload: load }
}
