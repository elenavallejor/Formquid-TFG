import { useState } from 'react';

import { login } from '../api/authApi';

const LOGIN_ERROR_MESSAGE =
  'No se ha podido iniciar sesión. Inténtalo de nuevo.';

export function useLogin() {
  const [isLoading, setIsLoading] = useState(false);
  const [loginError, setLoginError] = useState('');

  async function submitLogin(credentials) {
    setIsLoading(true);
    setLoginError('');

    try {
      const data = await login(credentials);

      return {
        success: true,
        data,
      };
    } catch {
      setLoginError(LOGIN_ERROR_MESSAGE);

      return {
        success: false,
        data: null,
      };
    } finally {
      setIsLoading(false);
    }
  }

  function clearLoginError() {
    setLoginError('');
  }

  return {
    clearLoginError,
    isLoading,
    loginError,
    submitLogin,
  };
}