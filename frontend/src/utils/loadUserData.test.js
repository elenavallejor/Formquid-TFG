import { describe, it, expect, vi } from 'vitest';
import { getMe } from '../api/usersApi.js';
import { loadUserData } from './loadUserData.js';
vi.mock('../api/usersApi.js', () => ({ getMe: vi.fn() }));

describe('loadUserData', () => {
  it.each([null, {}])('normaliza una cuenta sin datos opcionales: %j', async (account) => {
    getMe.mockResolvedValue(account);
    await expect(loadUserData()).resolves.toEqual({
      personalData: { nombre: '', institucion: '', pais: '' },
      email: '', profileImageKey: null, hasOwnedSurveys: undefined, hasActiveSurveys: false,
    });
  });
  it('conserva la ausencia del indicador de propiedad para cuentas heredadas', async () => {
    getMe.mockResolvedValue({ name: 'Ana', hasActiveSurveys: true });
    await expect(loadUserData()).resolves.toMatchObject({ personalData: { nombre: 'Ana' }, hasOwnedSurveys: undefined, hasActiveSurveys: true });
  });
});
