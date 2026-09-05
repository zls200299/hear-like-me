/**
 * Hear Like Me 业务枚举 → 中文标签的本地字典。
 *
 * 这里收纳的都是「技术枚举」：值由后端代码 / 数据库 CHECK 约束写死，
 * 不会由运营在后台增删，因此不走若依的 sys_dict 字典表，避免两处维护。
 * 改中文名只需改本文件。
 */

export interface DictItem {
  label: string
  tag?: 'primary' | 'success' | 'warning' | 'danger' | 'info'
}

/** 内置示例音编码（vowel/tone/melody） */
export const SAMPLE_CODE_DICT: Record<string, DictItem> = {
  vowel: { label: '元音', tag: 'primary' },
  tone: { label: '声调', tag: 'primary' },
  melody: { label: '旋律', tag: 'primary' }
}

/** 示例音生成方式 generator_type */
export const GENERATOR_TYPE_DICT: Record<string, DictItem> = {
  PYTHON_GENERATED: { label: 'Python 生成', tag: 'info' },
  PREGENERATED: { label: '预生成文件', tag: 'success' }
}

/** 文件资源类型 file_asset.asset_type */
export const ASSET_TYPE_DICT: Record<string, DictItem> = {
  AUDIO_SOURCE: { label: '音频源', tag: 'primary' },
  AUDIO_NORMALIZED: { label: '标准化音频', tag: 'info' },
  AUDIO_OUTPUT: { label: '模拟输出', tag: 'success' },
  SAMPLE_AUDIO: { label: '示例音频', tag: 'primary' },
  READ_IMAGE: { label: '点读图片', tag: 'info' },
  READ_AUDIO: { label: '点读音频', tag: 'info' },
  READ_AUDIO_PROCESSED: { label: '点读模拟音频', tag: 'success' },
  CONTENT_IMAGE: { label: '内容图片', tag: 'info' },
  OTHER: { label: '其它', tag: 'info' }
}

/** 文件资源状态 file_asset.status */
export const FILE_STATUS_DICT: Record<string, DictItem> = {
  ACTIVE: { label: '有效', tag: 'success' },
  EXPIRED: { label: '已过期', tag: 'warning' },
  DELETED: { label: '已删除', tag: 'danger' }
}

/** 业务配置值类型 system_config.value_type */
export const VALUE_TYPE_DICT: Record<string, DictItem> = {
  STRING: { label: '字符串', tag: 'info' },
  INT: { label: '整数', tag: 'info' },
  DECIMAL: { label: '小数', tag: 'info' },
  BOOL: { label: '布尔', tag: 'info' },
  JSON: { label: 'JSON', tag: 'info' }
}

/** 听音挑战题目状态 hearing_challenge.status */
export const CHALLENGE_STATUS_DICT: Record<string, DictItem> = {
  DRAFT: { label: '草稿', tag: 'info' },
  PUBLISHED: { label: '已发布', tag: 'success' },
  OFFLINE: { label: '已下线', tag: 'warning' }
}

/** 点读内容状态 read_aloud_item.status */
export const READ_ITEM_STATUS_DICT: Record<string, DictItem> = {
  DRAFT: { label: '草稿', tag: 'info' },
  PUBLISHED: { label: '已发布', tag: 'success' },
  OFFLINE: { label: '已下架', tag: 'warning' }
}

/** 字典里没有的值返回原值；空值返回 '-' */
export function dictLabel(dict: Record<string, DictItem>, value?: string | number | null): string {
  if (value == null || value === '') return '-'
  const item = dict[String(value)]
  return item ? item.label : String(value)
}

/** 返回 el-tag 的 type；字典里没有的值用 info */
export function dictTag(dict: Record<string, DictItem>, value?: string | number | null): string {
  if (value == null || value === '') return 'info'
  return dict[String(value)]?.tag || 'info'
}
