/**
 * Formatea una fecha para su visualización en la interfaz.
 */
export function formatDate(value, options = {}) {
  const {
    includeTime = false,
    locale = 'es-ES',
  } = options;

  if (!value) {
    return includeTime
      ? { date: '—', time: '' }
      : '—';
  }

  const date = new Date(value);

  if (Number.isNaN(date.getTime())) {
    return includeTime
      ? { date: '—', time: '' }
      : '—';
  }

  if (!includeTime) {
    return date.toLocaleDateString(locale);
  }

  return {
    date: date.toLocaleDateString(locale),
    time: date.toLocaleTimeString(locale, {
      hour: '2-digit',
      minute: '2-digit',
    }),
  };
}

export function formatDateTime(value) {
  const { date, time } = formatDate(value, { includeTime: true });
  return time ? `${date}, ${time}` : date;
}
