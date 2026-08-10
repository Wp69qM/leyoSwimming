import { isValidPhone, maskPhone, formatPhoneInput, sanitizeCodeInput } from './phone'

describe('phone', () => {
  describe('isValidPhone', () => {
    test('returns true for valid Chinese mobile numbers', () => {
      expect(isValidPhone('13800138000')).toBe(true)
      expect(isValidPhone('15912345678')).toBe(true)
    })

    test('returns false for invalid numbers', () => {
      expect(isValidPhone('1380013800')).toBe(false)
      expect(isValidPhone('138001380000')).toBe(false)
      expect(isValidPhone('12345678901')).toBe(false)
      expect(isValidPhone('')).toBe(false)
    })
  })

  describe('maskPhone', () => {
    test('masks middle 4 digits for 11-digit phone', () => {
      expect(maskPhone('13800138000')).toBe('138****8000')
    })

    test('returns original value for non-11-digit input', () => {
      expect(maskPhone('1380013800')).toBe('1380013800')
    })
  })

  describe('formatPhoneInput', () => {
    test('removes non-digit characters and limits to 11 digits', () => {
      expect(formatPhoneInput('138-0013-8000')).toBe('13800138000')
      expect(formatPhoneInput('13800138000000')).toBe('13800138000')
      expect(formatPhoneInput('abc')).toBe('')
    })
  })

  describe('sanitizeCodeInput', () => {
    test('removes non-digit characters and limits to 6 digits', () => {
      expect(sanitizeCodeInput('123456')).toBe('123456')
      expect(sanitizeCodeInput('1234567')).toBe('123456')
      expect(sanitizeCodeInput('12a34b')).toBe('1234')
    })
  })
})
