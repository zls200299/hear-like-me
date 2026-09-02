import { addOrUpdate, deleteById, getById, pageGet } from './common'

const BASE = '/file/asset'

export interface FileAsset {
  id?: string
  assetType?: string
  originalFilename?: string
  fileExt?: string
  mimeType?: string
  fileSize?: number
  objectKey?: string
  status?: string
  createTime?: string
}

export const listFileAsset = (params: Record<string, unknown>) => pageGet<FileAsset>(BASE, params)
export const getFileAsset = (id: string) => getById<FileAsset>(BASE, id)
export const saveFileAsset = (data: FileAsset) => addOrUpdate(BASE, data)
export const removeFileAsset = (id: string) => deleteById(BASE, id)
