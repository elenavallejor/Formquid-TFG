import { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState } from 'react';

import { getCurrentUser, login as loginRequest, logout as logoutRequest } from '../api/authApi.js';
import { ApiError, setUnauthorizedHandler } from '../api/apiClient.js';

/**
 * Gestiona globalmente la autenticación, la restauración de sesión 
 * y el usuario autenticado.
 */
const AuthContext = createContext(null);

const AUTH_STATUS = {
  UNKNOWN: 'unknown',
  AUTHENTICATED: 'authenticated',
  ANONYMOUS: 'anonymous',
  ERROR: 'error',
};

function getAuthenticatedUserFromResponse(response) {
  const user = response?.user ?? response ?? null;

  if (!user?.id || !user?.email || !user?.role) {
    return null;
  }

  return user;
}

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [authStatus, setAuthStatus] = useState(AUTH_STATUS.UNKNOWN);
  const [authError, setAuthError] = useState('');
  const [isInitializing, setIsInitializing] = useState(true);

  const userRef = useRef(null);
  const restorePromiseRef = useRef(null);

  const setAuthenticatedUser = useCallback((authenticatedUser) => {
    const nextUser = getAuthenticatedUserFromResponse(authenticatedUser);
    if (!nextUser) return null;

    userRef.current = nextUser;
    setUser(nextUser);
    setAuthStatus(AUTH_STATUS.AUTHENTICATED);
    setAuthError('');

    return nextUser;
  }, []);

  const clearAuthentication = useCallback(() => {
    userRef.current = null;
    setUser(null);
    setAuthStatus(AUTH_STATUS.ANONYMOUS);
    setAuthError('');
  }, []);

  const restoreSession = useCallback(async () => {
    if (userRef.current) return userRef.current;
    if (restorePromiseRef.current) return restorePromiseRef.current;

    setAuthError('');

    const restorePromise = getCurrentUser()
      .then((response) => {
        const currentUser = getAuthenticatedUserFromResponse(response);

        if (!currentUser) {
          clearAuthentication();
          return null;
        }

        return setAuthenticatedUser(currentUser);
      })
      .catch((error) => {
        if (error instanceof ApiError && error.status === 401) {
          clearAuthentication();
          return null;
        }

        userRef.current = null;
        setUser(null);
        setAuthStatus(AUTH_STATUS.ERROR);
        setAuthError(
          error.message || 'No se ha podido comprobar la sesión.',
        );

        return null;
      })
      .finally(() => {
        restorePromiseRef.current = null;
      });

    restorePromiseRef.current = restorePromise;

    return restorePromise;
  }, [clearAuthentication, setAuthenticatedUser]);

  useEffect(() => {
    setUnauthorizedHandler(clearAuthentication);

    return () => {
      setUnauthorizedHandler(null);
    };
  }, [clearAuthentication]);

  useEffect(() => {
    let isMounted = true;

    async function initializeAuth() {
      try {
        await restoreSession();
      } finally {
        if (isMounted) {
          setIsInitializing(false);
        }
      }
    }

    void initializeAuth();

    return () => {
      isMounted = false;
    };
  }, [restoreSession]);

  const login = useCallback(
    async (credentials) => {
      try {
        setAuthError('');

        const response = await loginRequest(credentials);
        const authenticatedUser =
          getAuthenticatedUserFromResponse(response);

        if (!authenticatedUser) {
          throw new Error(
            'El servidor no ha devuelto los datos del usuario autenticado.',
          );
        }

        return setAuthenticatedUser(authenticatedUser);
      } catch (error) {
        setAuthError(error.message || 'Error al iniciar sesión.');
        throw error;
      }
    },
    [setAuthenticatedUser],
  );

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
      isAuthenticated: authStatus === AUTH_STATUS.AUTHENTICATED,
      isAuthLoading: isInitializing,
      isInitializing,
      login,
      logout,
      setAuthenticatedUser,
      restoreSession,
      clearAuthentication,
    }),
    [
      user,
      authStatus,
      authError,
      isInitializing,
      login,
      logout,
      setAuthenticatedUser,
      restoreSession,
      clearAuthentication,
    ],
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
    throw new Error('useAuth debe utilizarse dentro de AuthProvider.');
  }

  return context;
}