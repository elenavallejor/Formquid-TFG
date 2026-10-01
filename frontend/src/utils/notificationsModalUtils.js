export const NOTIFICATION_TYPE_OPTIONS = [
  ['GENERAL', 'Avisos generales'],
  ['REGISTRATION_REQUEST', 'Solicitudes de registro'],
  ['REGISTRATION_APPROVED', 'Altas aprobadas'],
  ['REGISTRATION_REJECTED', 'Altas rechazadas'],
  ['ACCOUNT_UPDATED', 'Cuenta'],
  ['ACCOUNT_SECURITY_CHANGED', 'Seguridad de la cuenta'],
  ['COLLABORATION_INVITATION', 'Invitaciones'],
  ['COLLABORATION_REQUEST', 'Solicitudes de colaboración'],
  ['COLLABORATION_ACCEPTED', 'Colaboraciones aceptadas'],
  ['COLLABORATION_REJECTED', 'Colaboraciones rechazadas'],
  ['COLLABORATION_REVOKED', 'Colaboraciones revocadas'],
  ['COLLABORATION_PERMISSION_REDUCED', 'Cambios de permisos'],
  ['PUBLICATION_REQUEST', 'Solicitudes de publicación'],
  ['PUBLICATION_ACCEPTED', 'Publicaciones aceptadas'],
  ['PUBLICATION_REJECTED', 'Publicaciones rechazadas'],
  ['ELEMENT_REQUEST', 'Solicitudes de catálogo'],
  ['ELEMENT_REQUEST_RESOLVED', 'Solicitudes de catálogo resueltas'],
  ['SURVEY_STATUS_CHANGED', 'Cambios en encuestas'],
  ['SURVEY_OWNERSHIP_TRANSFERRED', 'Transferencias de encuestas'],
];

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
