import { useState } from 'react';

import { createRegistrationRequest } from '../features/auth/api/createRegistrationRequest';
import { ApiError } from '../lib/apiClient';

const DEFAULT_REGISTRATION_ERROR =
  'No se ha podido enviar la solicitud. Inténtalo de nuevo.';

function getRegistrationErrorMessage(error) {
  if (error instanceof ApiError && error.status === 409) {
    return 'Ya existe una cuenta o solicitud asociada a este correo.';
  }

  if (error instanceof ApiError && error.status === 400) {
    return 'Revisa los datos introducidos antes de enviar la solicitud.';
  }

  return DEFAULT_REGISTRATION_ERROR;
}

/**
 * Gestiona el envío de solicitudes públicas de registro.
 */
export function useRegistrationRequest() {
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [registrationError, setRegistrationError] = useState('');
  const [successMessage, setSuccessMessage] = useState('');

  async function submitRegistrationRequest(payload) {
    setIsSubmitting(true);
    setRegistrationError('');
    setSuccessMessage('');

    try {
      const data = await createRegistrationRequest(payload);

      setSuccessMessage(
        'Solicitud enviada correctamente. Un administrador revisará tus datos.'
      );

      return {
        success: true,
        data,
      };
    } catch (error) {
      setRegistrationError(getRegistrationErrorMessage(error));

      return {
        success: false,
        data: null,
      };
    } finally {
      setIsSubmitting(false);
    }
  }

  function clearRegistrationMessages() {
    setRegistrationError('');
    setSuccessMessage('');
  }

  return {
    clearRegistrationMessages,
    isSubmitting,
    registrationError,
    submitRegistrationRequest,
    successMessage,
  };
}