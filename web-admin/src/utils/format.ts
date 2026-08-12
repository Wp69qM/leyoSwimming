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
