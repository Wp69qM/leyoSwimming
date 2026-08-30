import { formatPhoneInput, isValidPhone, maskPhone } from './phone';

describe('phone utils', () => {
  describe('isValidPhone', () => {
    test('returns true for valid mainland phone numbers', () => {
      expect(isValidPhone('13800138000')).toBe(true);
      expect(isValidPhone('15012345678')).toBe(true);
      expect(isValidPhone('19912345678')).toBe(true);
    });

    test('returns false for invalid phone numbers', () => {
      expect(isValidPhone('')).toBe(false);
      expect(isValidPhone('12345678901')).toBe(false);
      expect(isValidPhone('1380013800')).toBe(false);
      expect(isValidPhone('138001380000')).toBe(false);
      expect(isValidPhone('abcdefghijk')).toBe(false);
    });
  });

  describe('maskPhone', () => {
    test('masks middle digits of 11-digit phone number', () => {
      expect(maskPhone('13800138000')).toBe('138****8000');
    });

    test('returns original value when length is not 11', () => {
      expect(maskPhone('1380013800')).toBe('1380013800');
    });
  });

  describe('formatPhoneInput', () => {
    test('removes non-digit characters and limits to 11 digits', () => {
      expect(formatPhoneInput('138-0013-8000')).toBe('13800138000');
      expect(formatPhoneInput('13800138000000')).toBe('13800138000');
      expect(formatPhoneInput('abc')).toBe('');
    });
  });
});
