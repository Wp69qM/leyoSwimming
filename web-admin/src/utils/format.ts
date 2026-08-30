export function maskPhone(phone: string): string {
  if (!phone) return '';
  if (phone.length === 11) {
    return `${phone.slice(0, 3)}****${phone.slice(7)}`;
  }
  return phone;
}

export function maskIdCard(idCard: string): string {
  if (!idCard) return '-';
  if (idCard.length <= 10) return idCard;
  return `${idCard.slice(0, 6)}${'*'.repeat(idCard.length - 10)}${idCard.slice(-4)}`;
}

export function formatDateTime(value: string | Date | undefined): string {
  if (!value) return '-';
  const date = typeof value === 'string' ? new Date(value) : value;
  if (Number.isNaN(date.getTime())) return '-';
  const pad = (n: number) => n.toString().padStart(2, '0');
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`;
}

export function formatDate(value: string | Date | undefined): string {
  if (!value) return '-';
  const date = typeof value === 'string' ? new Date(value) : value;
  if (Number.isNaN(date.getTime())) return '-';
  const pad = (n: number) => n.toString().padStart(2, '0');
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`;
}

export function calculateTenure(
  startAt: string | Date | undefined,
  endAt: string | Date | undefined = new Date()
): string {
  if (!startAt) return '-';
  const start = typeof startAt === 'string' ? new Date(startAt) : startAt;
  const end = typeof endAt === 'string' ? new Date(endAt) : endAt;
  if (Number.isNaN(start.getTime()) || Number.isNaN(end.getTime())) return '-';

  let years = end.getFullYear() - start.getFullYear();
  let months = end.getMonth() - start.getMonth();
  if (end.getDate() < start.getDate()) {
    months--;
  }
  if (months < 0) {
    years--;
    months += 12;
  }

  if (years > 0 && months > 0) {
    return `${years}年${months}个月`;
  }
  if (years > 0) {
    return `${years}年`;
  }
  if (months > 0) {
    return `${months}个月`;
  }
  return '1个月内';
}
