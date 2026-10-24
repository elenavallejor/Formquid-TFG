const DESTINATION_LABELS = {
  BLOCK: 'Ir al catálogo',
  COLLABORATION_REQUEST: 'Gestionar colaboración',
  ELEMENT_REQUEST: 'Gestionar catálogo',
  PUBLICATION_REQUEST: 'Gestionar publicación',
  QUESTION: 'Ir al catálogo',
  REGISTRATION_REQUEST: 'Gestionar solicitud',
  SURVEY: 'Ir a la encuesta',
  TEMPLATE: 'Ir al catálogo',
  USER: 'Gestionar usuario',
};

export function getNotificationDestinationLabel(targetType) {
  return DESTINATION_LABELS[targetType] ?? null;
}

export function getNotificationPageSize(viewportHeight = window.innerHeight) {
  return Math.min(8, Math.max(3, Math.floor((viewportHeight - 360) / 130)));
}
