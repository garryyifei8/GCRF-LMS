import * as XLSX from 'xlsx'

/**
 * 导出 Excel 文件
 * @param {Array} data - 数据数组
 * @param {Array} headers - 表头配置 [{ label: '姓名', key: 'name' }, ...]
 * @param {string} filename - 文件名(不含扩展名)
 */
export function exportExcel(data, headers, filename = 'export') {
  try {
    // 创建表头
    const headerRow = headers.map(h => h.label)

    // 转换数据
    const rows = data.map(item =>
      headers.map(h => {
        const value = item[h.key]
        // 处理空值
        if (value === null || value === undefined) return ''
        // 处理布尔值
        if (typeof value === 'boolean') return value ? '是' : '否'
        return value
      })
    )

    // 合并表头和数据
    const wsData = [headerRow, ...rows]

    // 创建工作表
    const ws = XLSX.utils.aoa_to_sheet(wsData)

    // 设置列宽
    const colWidths = headers.map(h => ({ wch: h.width || 15 }))
    ws['!cols'] = colWidths

    // 创建工作簿
    const wb = XLSX.utils.book_new()
    XLSX.utils.book_append_sheet(wb, ws, 'Sheet1')

    // 导出文件
    const timestamp = new Date().getTime()
    XLSX.writeFile(wb, `${filename}_${timestamp}.xlsx`)

    return true
  } catch (error) {
    console.error('导出Excel失败:', error)
    throw error
  }
}

/**
 * 读取 Excel 文件
 * @param {File} file - 文件对象
 * @param {Array} headers - 表头配置 [{ label: '姓名', key: 'name' }, ...]
 * @returns {Promise<Array>} 返回解析后的数据数组
 */
export function readExcel(file, headers) {
  return new Promise((resolve, reject) => {
    try {
      const reader = new FileReader()

      reader.onload = (e) => {
        try {
          // 读取文件
          const data = new Uint8Array(e.target.result)
          const workbook = XLSX.read(data, { type: 'array' })

          // 获取第一个工作表
          const firstSheetName = workbook.SheetNames[0]
          const worksheet = workbook.Sheets[firstSheetName]

          // 转换为JSON
          const jsonData = XLSX.utils.sheet_to_json(worksheet, { header: 1 })

          // 验证数据
          if (jsonData.length < 2) {
            reject(new Error('Excel文件内容为空'))
            return
          }

          // 第一行是表头,从第二行开始是数据
          const excelHeaders = jsonData[0]
          const dataRows = jsonData.slice(1)

          // 验证表头
          const headerLabels = headers.map(h => h.label)
          const missingHeaders = headerLabels.filter(label => !excelHeaders.includes(label))
          if (missingHeaders.length > 0) {
            reject(new Error(`Excel表头不匹配,缺少列: ${missingHeaders.join(', ')}`))
            return
          }

          // 创建表头索引映射
          const headerIndexMap = {}
          headers.forEach(h => {
            const index = excelHeaders.indexOf(h.label)
            if (index !== -1) {
              headerIndexMap[h.key] = index
            }
          })

          // 转换数据
          const result = dataRows
            .filter(row => row && row.length > 0 && row.some(cell => cell !== null && cell !== undefined && cell !== ''))
            .map((row, rowIndex) => {
              const item = {}
              headers.forEach(h => {
                const colIndex = headerIndexMap[h.key]
                if (colIndex !== undefined) {
                  let value = row[colIndex]

                  // 处理空值
                  if (value === null || value === undefined || value === '') {
                    value = h.required ? undefined : ''
                  }

                  // 类型转换
                  if (value !== undefined && value !== '') {
                    if (h.type === 'number') {
                      value = Number(value)
                    } else if (h.type === 'boolean') {
                      value = value === '是' || value === 'true' || value === true
                    } else {
                      value = String(value).trim()
                    }
                  }

                  item[h.key] = value
                }
              })

              // 添加行号用于错误提示
              item._rowIndex = rowIndex + 2 // Excel行号从1开始,且跳过表头

              return item
            })

          resolve(result)
        } catch (error) {
          console.error('解析Excel失败:', error)
          reject(new Error('Excel文件格式错误或内容无法解析'))
        }
      }

      reader.onerror = () => {
        reject(new Error('文件读取失败'))
      }

      reader.readAsArrayBuffer(file)
    } catch (error) {
      console.error('读取Excel失败:', error)
      reject(error)
    }
  })
}

/**
 * 下载Excel模板
 * @param {Array} headers - 表头配置
 * @param {string} filename - 文件名
 * @param {Array} exampleData - 示例数据(可选)
 */
export function downloadTemplate(headers, filename = 'template', exampleData = []) {
  try {
    // 创建表头
    const headerRow = headers.map(h => h.label)

    // 如果有示例数据,添加到模板中
    const rows = exampleData.length > 0
      ? exampleData.map(item =>
          headers.map(h => item[h.key] || '')
        )
      : []

    // 合并表头和数据
    const wsData = [headerRow, ...rows]

    // 创建工作表
    const ws = XLSX.utils.aoa_to_sheet(wsData)

    // 设置列宽
    const colWidths = headers.map(h => ({ wch: h.width || 15 }))
    ws['!cols'] = colWidths

    // 创建工作簿
    const wb = XLSX.utils.book_new()
    XLSX.utils.book_append_sheet(wb, ws, 'Sheet1')

    // 导出文件
    XLSX.writeFile(wb, `${filename}.xlsx`)

    return true
  } catch (error) {
    console.error('下载模板失败:', error)
    throw error
  }
}
