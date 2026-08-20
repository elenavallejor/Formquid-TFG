import { RouterProvider } from 'react-router-dom';

import { router } from './router';

export function AppProvider() {
  return <RouterProvider router={router} />;
}