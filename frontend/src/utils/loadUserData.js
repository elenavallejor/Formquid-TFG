import { getMe } from '../api/usersApi.js';

/**
 * Obtiene y transforma los datos del usuario autenticado para la pantalla de configuración.
 */
export async function loadUserData() {
  const accountInfo = await getMe();
  const hasOwnedSurveys = accountInfo?.hasOwnedSurveys === undefined ? undefined : Boolean(accountInfo.hasOwnedSurveys);

  return {
    personalData: {
      nombre: accountInfo?.name ?? '',
      institucion: accountInfo?.institution ?? '',
      genero: accountInfo?.gender ?? 'MALE',
    },
    email: accountInfo?.email ?? '',
    hasOwnedSurveys,
    hasActiveSurveys: Boolean(accountInfo?.hasActiveSurveys),
  };
}