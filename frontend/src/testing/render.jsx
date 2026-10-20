import { render } from '@testing-library/react';
import { createMemoryRouter, MemoryRouter, RouterProvider } from 'react-router-dom';

import { AuthContext } from '../context/authContext.js';

const anonymousAuth = {
  user: null,
  isAuthenticated: false,
  isAuthLoading: false,
  isInitializing: false,
};

export function renderWithRouter(ui, { route = '/', auth = {} } = {}) {
  return render(
    <AuthContext.Provider value={{ ...anonymousAuth, ...auth }}>
      <MemoryRouter initialEntries={[route]}>{ui}</MemoryRouter>
    </AuthContext.Provider>,
  );
}

export function renderWithDataRouter(routes, { route = '/', auth = {} } = {}) {
  const router = createMemoryRouter(routes, { initialEntries: [route] });
  return {
    ...render(
      <AuthContext.Provider value={{ ...anonymousAuth, ...auth }}>
        <RouterProvider router={router} />
      </AuthContext.Provider>,
    ),
    router,
  };
}
