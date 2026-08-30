const PHONE_REGEX = /^1[3-9]\d{9}$/;

export function isValidPhone(phone: string): boolean {
  return PHONE_REGEX.test(phone);
}

export function maskPhone(phone: string): string {
  if (phone.length !== 11) return phone;
  return `${phone.slice(0, 3)}****${phone.slice(7)}`;
}

export function formatPhoneInput(value: string): string {
  return value.replace(/\D/g, '').slice(0, 11);
}
