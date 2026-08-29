/**
 * 创建音频处理任务（占位，后续接入）
 * @param {Object} data
 * @returns {Promise<never>}
 */
function createTask(data) {
  return Promise.reject({
    message: 'createTask 尚未实现',
    data
  })
}

/**
 * 查询音频处理任务（占位，后续接入）
 * @param {string} taskNo
 * @returns {Promise<never>}
 */
function getTask(taskNo) {
  return Promise.reject({
    message: 'getTask 尚未实现',
    taskNo
  })
}

module.exports = {
  createTask,
  getTask
}
