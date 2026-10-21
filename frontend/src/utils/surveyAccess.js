const ACCESS_LABELS = {
  PUBLIC: 'PÚBLICO',
  PASSWORD_PROTECTED: 'CON CONTRASEÑA',
  AUTHENTICATED: 'USUARIOS AUTENTICADOS',
};

export function getSurveyAccessBadge(lifecycleStatus, accessMode) {
  if (lifecycleStatus === 'DRAFT') {
    return { label: 'SIN PUBLICAR', variant: 'draft' };
  }

  return {
    label: ACCESS_LABELS[accessMode] || accessMode || '—',
    variant: String(accessMode || 'PUBLIC').toLowerCase(),
  };
}
