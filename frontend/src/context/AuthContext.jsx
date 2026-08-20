import { createContext, useCallback, useContext, useMemo, useRef, useState } from 'react';
import { getCurrentUser, logout as logoutRequest } from '../api/authApi';
import { ApiError } from '../api/apiClient';

const AuthContext = createContext(null);

const AUTH_STATUS = {
  AUTHENTICATED: 'authenticated',
  ANONYMOUS: 'anonymous',
  ERROR: 'error',
};

function getAuthenticatedUserFromResponse(response) {
  return response?.user ?? response ?? null;
}

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [authStatus, setAuthStatus] = useState(AUTH_STATUS.ANONYMOUS);
  const [authError, setAuthError] = useState('');

  const restorePromiseRef = useRef(null);

  const setAuthenticatedUser = useCallback((authenticatedUser) => {
    const nextUser = getAuthenticatedUserFromResponse(authenticatedUser);

    if (!nextUser) {
      return;
    }

    setUser(nextUser);
    setAuthStatus(AUTH_STATUS.AUTHENTICATED);
    setAuthError('');
  }, []);

  const clearAuthentication = useCallback(() => {
    setUser(null);
    setAuthStatus(AUTH_STATUS.ANONYMOUS);
    setAuthError('');
    restorePromiseRef.current = null;
  }, []);

  const restoreSession = useCallback(async () => {
    if (user) {
      return user;
    }

    if (restorePromiseRef.current) {
      return restorePromiseRef.current;
    }

    setAuthError('');

    const restorePromise = getCurrentUser()
      .then((response) => {
        const currentUser = getAuthenticatedUserFromResponse(response);

        if (!currentUser) {
          clearAuthentication();

          return null;
        }

        setAuthenticatedUser(currentUser);

        return currentUser;
      })
      .catch((error) => {
        if (error instanceof ApiError && error.status === 401) {
          clearAuthentication();

          return null;
        }

        setUser(null);
        setAuthStatus(AUTH_STATUS.ERROR);
        setAuthError(
          error.message ||
            'No se ha podido comprobar la sesión.'
        );

        return null;
      })
      .finally(() => {
        restorePromiseRef.current = null;
      });

    restorePromiseRef.current = restorePromise;

    return restorePromise;
  }, [
    clearAuthentication,
    setAuthenticatedUser,
    user,
  ]);

  const logout = useCallback(async () => {
    try {
      await logoutRequest();

      clearAuthentication();
    } catch (error) {
      if (error instanceof ApiError && error.status === 401) {
        clearAuthentication();

        return;
      }

      throw error;
    }
  }, [clearAuthentication]);

  const value = useMemo(
    () => ({
      user,
      authStatus,
      authError,

      isAuthenticated:
        authStatus === AUTH_STATUS.AUTHENTICATED,

      isAuthLoading: false,

      setAuthenticatedUser,
      restoreSession,
      clearAuthentication,
      logout,
    }),
    [
      user,
      authStatus,
      authError,
      setAuthenticatedUser,
      restoreSession,
      clearAuthentication,
      logout,
    ]
  );

  return (
    <AuthContext.Provider value={value}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const context = useContext(AuthContext);

  if (!context) {
    throw new Error(
      'useAuth debe utilizarse dentro de AuthProvider.'
    );
  }

  return context;
}