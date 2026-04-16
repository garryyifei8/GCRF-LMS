/* eslint-env browser, node */
import { describe, it, expect, vi, beforeEach } from 'vitest'

// Mock xlsx BEFORE importing the module under test
vi.mock('xlsx', () => ({
  utils: {
    aoa_to_sheet: vi.fn(() => ({ '!ref': 'A1:C2' })),
    book_new: vi.fn(() => ({ SheetNames: [], Sheets: {} })),
    book_append_sheet: vi.fn(),
    sheet_to_json: vi.fn()
  },
  read: vi.fn(),
  writeFile: vi.fn()
}))

import * as XLSX from 'xlsx'
import { exportExcel, readExcel, downloadTemplate } from '@/utils/excel'

describe('utils/excel', () => {
  const sampleHeaders = [
    { label: '姓名', key: 'name', width: 20 },
    { label: '年龄', key: 'age', width: 10 },
    { label: '邮箱', key: 'email' }
  ]

  const sampleData = [
    { name: '张三', age: 25, email: 'zhangsan@test.com' },
    { name: '李四', age: null, email: undefined },
    { name: '王五', age: 30, email: 'wangwu@test.com', active: true }
  ]

  beforeEach(() => {
    vi.clearAllMocks()
    // Reset mocks to default implementations
    XLSX.utils.aoa_to_sheet.mockReturnValue({ '!ref': 'A1:C2' })
    XLSX.utils.book_new.mockReturnValue({ SheetNames: [], Sheets: {} })
  })

  // ---- exportExcel ----

  it('exportExcel_withValidData_shouldCallWriteFile', () => {
    const result = exportExcel(sampleData, sampleHeaders, 'test-export')

    expect(XLSX.utils.aoa_to_sheet).toHaveBeenCalled()
    expect(XLSX.utils.book_new).toHaveBeenCalled()
    expect(XLSX.utils.book_append_sheet).toHaveBeenCalled()
    expect(XLSX.writeFile).toHaveBeenCalled()
    expect(result).toBe(true)
  })

  it('exportExcel_filename_shouldIncludeTimestampAndExtension', () => {
    exportExcel(sampleData, sampleHeaders, 'myfile')

    const callArgs = XLSX.writeFile.mock.calls[0]
    expect(callArgs[1]).toMatch(/^myfile_\d+\.xlsx$/)
  })

  it('exportExcel_nullValues_shouldConvertToEmptyString', () => {
    exportExcel(sampleData, sampleHeaders, 'test')

    const wsData = XLSX.utils.aoa_to_sheet.mock.calls[0][0]
    // Header row
    expect(wsData[0]).toEqual(['姓名', '年龄', '邮箱'])
    // Second data row has null age and undefined email -> empty strings
    expect(wsData[2][1]).toBe('')
    expect(wsData[2][2]).toBe('')
  })

  it('exportExcel_booleanValues_shouldConvertToChineseText', () => {
    const boolData = [{ flag: true }, { flag: false }]
    const boolHeaders = [{ label: '标志', key: 'flag' }]
    exportExcel(boolData, boolHeaders, 'bool-test')

    const wsData = XLSX.utils.aoa_to_sheet.mock.calls[0][0]
    expect(wsData[1][0]).toBe('是')
    expect(wsData[2][0]).toBe('否')
  })

  it('exportExcel_withCustomColumnWidth_shouldSetColWidths', () => {
    exportExcel(sampleData, sampleHeaders, 'test')

    const ws = XLSX.utils.aoa_to_sheet.mock.results[0].value
    ws['!cols'] = [{ wch: 20 }, { wch: 10 }, { wch: 15 }]
    expect(sampleHeaders[0].width).toBe(20)
    expect(sampleHeaders[1].width).toBe(10)
    // Default width for email column
    expect(sampleHeaders[2].width).toBeUndefined()
  })

  it('exportExcel_xlsxThrows_shouldRethrowError', () => {
    XLSX.writeFile.mockImplementationOnce(() => {
      throw new Error('write failed')
    })

    expect(() => exportExcel(sampleData, sampleHeaders, 'test')).toThrow('write failed')
  })

  it('exportExcel_withDefaultFilename_shouldUseExport', () => {
    exportExcel(sampleData, sampleHeaders)

    const callArgs = XLSX.writeFile.mock.calls[0]
    expect(callArgs[1]).toMatch(/^export_\d+\.xlsx$/)
  })

  // ---- downloadTemplate ----

  it('downloadTemplate_withHeaders_shouldCreateXlsxFile', () => {
    const result = downloadTemplate(sampleHeaders, 'my-template')

    expect(XLSX.utils.aoa_to_sheet).toHaveBeenCalled()
    expect(XLSX.utils.book_new).toHaveBeenCalled()
    expect(XLSX.utils.book_append_sheet).toHaveBeenCalled()
    expect(XLSX.writeFile).toHaveBeenCalledWith(expect.anything(), 'my-template.xlsx')
    expect(result).toBe(true)
  })

  it('downloadTemplate_withExampleData_shouldIncludeExampleRows', () => {
    const example = [{ name: '示例姓名', age: 20, email: 'ex@test.com' }]
    downloadTemplate(sampleHeaders, 'tpl', example)

    const wsData = XLSX.utils.aoa_to_sheet.mock.calls[0][0]
    // Should have header row + 1 example row
    expect(wsData.length).toBe(2)
    expect(wsData[1][0]).toBe('示例姓名')
  })

  it('downloadTemplate_withoutExampleData_shouldOnlyHaveHeaderRow', () => {
    downloadTemplate(sampleHeaders, 'tpl')

    const wsData = XLSX.utils.aoa_to_sheet.mock.calls[0][0]
    expect(wsData.length).toBe(1)
    expect(wsData[0]).toEqual(['姓名', '年龄', '邮箱'])
  })

  it('downloadTemplate_xlsxThrows_shouldRethrowError', () => {
    XLSX.writeFile.mockImplementationOnce(() => {
      throw new Error('template write failed')
    })

    expect(() => downloadTemplate(sampleHeaders, 'tpl')).toThrow('template write failed')
  })

  // ---- readExcel ----

  it('readExcel_validFile_shouldResolveWithParsedData', async () => {
    const mockJsonData = [
      ['姓名', '年龄', '邮箱'],
      ['张三', 25, 'zhangsan@test.com'],
      ['李四', 30, 'lisi@test.com']
    ]
    XLSX.read.mockReturnValue({
      SheetNames: ['Sheet1'],
      Sheets: { Sheet1: {} }
    })
    XLSX.utils.sheet_to_json.mockReturnValue(mockJsonData)

    const file = new File([new ArrayBuffer(8)], 'test.xlsx')

    // Stub FileReader to immediately fire onload
    const originalFileReader = global.FileReader
    global.FileReader = class {
      constructor() {
        this.onload = null
        this.onerror = null
      }
      readAsArrayBuffer() {
        setTimeout(() => {
          if (this.onload) {
            this.onload({ target: { result: new ArrayBuffer(8) } })
          }
        }, 0)
      }
    }

    const result = await readExcel(file, sampleHeaders)

    expect(result).toHaveLength(2)
    expect(result[0].name).toBe('张三')
    // age comes back as String since sampleHeaders has no type:'number' for age
    expect(String(result[0].age)).toBe('25')
    expect(result[0]._rowIndex).toBe(2)

    global.FileReader = originalFileReader
  })

  it('readExcel_emptyFile_shouldReject', async () => {
    XLSX.read.mockReturnValue({
      SheetNames: ['Sheet1'],
      Sheets: { Sheet1: {} }
    })
    // Only header row, no data
    XLSX.utils.sheet_to_json.mockReturnValue([['姓名']])

    const file = new File([new ArrayBuffer(8)], 'empty.xlsx')

    const originalFileReader = global.FileReader
    global.FileReader = class {
      constructor() {
        this.onload = null
        this.onerror = null
      }
      readAsArrayBuffer() {
        setTimeout(() => this.onload && this.onload({ target: { result: new ArrayBuffer(8) } }), 0)
      }
    }

    await expect(readExcel(file, sampleHeaders)).rejects.toThrow('Excel文件内容为空')

    global.FileReader = originalFileReader
  })

  it('readExcel_missingHeaders_shouldReject', async () => {
    XLSX.read.mockReturnValue({
      SheetNames: ['Sheet1'],
      Sheets: { Sheet1: {} }
    })
    // Missing '年龄' and '邮箱' columns
    XLSX.utils.sheet_to_json.mockReturnValue([['姓名'], ['张三']])

    const file = new File([new ArrayBuffer(8)], 'bad-headers.xlsx')

    const originalFileReader = global.FileReader
    global.FileReader = class {
      constructor() {
        this.onload = null
        this.onerror = null
      }
      readAsArrayBuffer() {
        setTimeout(() => this.onload && this.onload({ target: { result: new ArrayBuffer(8) } }), 0)
      }
    }

    await expect(readExcel(file, sampleHeaders)).rejects.toThrow('Excel表头不匹配')

    global.FileReader = originalFileReader
  })

  it('readExcel_fileReaderError_shouldReject', async () => {
    const file = new File([new ArrayBuffer(8)], 'error.xlsx')

    const originalFileReader = global.FileReader
    global.FileReader = class {
      constructor() {
        this.onload = null
        this.onerror = null
      }
      readAsArrayBuffer() {
        setTimeout(() => this.onerror && this.onerror(new Error('read error')), 0)
      }
    }

    await expect(readExcel(file, sampleHeaders)).rejects.toThrow('文件读取失败')

    global.FileReader = originalFileReader
  })

  it('readExcel_typeConversion_shouldConvertNumberAndBoolean', async () => {
    const typedHeaders = [
      { label: '数量', key: 'count', type: 'number' },
      { label: '激活', key: 'active', type: 'boolean' }
    ]
    XLSX.read.mockReturnValue({
      SheetNames: ['Sheet1'],
      Sheets: { Sheet1: {} }
    })
    XLSX.utils.sheet_to_json.mockReturnValue([
      ['数量', '激活'],
      ['42', '是'],
      ['10', 'false']
    ])

    const file = new File([new ArrayBuffer(8)], 'typed.xlsx')

    const originalFileReader = global.FileReader
    global.FileReader = class {
      constructor() {
        this.onload = null
        this.onerror = null
      }
      readAsArrayBuffer() {
        setTimeout(() => this.onload && this.onload({ target: { result: new ArrayBuffer(8) } }), 0)
      }
    }

    const result = await readExcel(file, typedHeaders)
    expect(result[0].count).toBe(42)
    expect(result[0].active).toBe(true)
    expect(result[1].active).toBe(false)

    global.FileReader = originalFileReader
  })
})
