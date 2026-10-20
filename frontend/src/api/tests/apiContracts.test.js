import { beforeEach, describe, expect, it, vi } from 'vitest';

import { apiClient } from '../apiClient.js';
import * as auth from '../authApi.js';
import * as categories from '../categoriesApi.js';
import * as collaboration from '../collaborationsApi.js';
import * as dashboard from '../dashboardApi.js';
import * as elements from '../elementRequestsApi.js';
import * as participation from '../participationApi.js';
import * as registration from '../registrationRequestsApi.js';
import * as statistics from '../statisticsApi.js';
import * as content from '../surveyContentApi.js';
import * as users from '../usersApi.js';

vi.mock('../apiClient.js', () => ({ apiClient: vi.fn() }));

function expectRequest(action, path, options) {
  action();
  expect(apiClient).toHaveBeenLastCalledWith(path, options);
}

describe('contratos de las API del frontend', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('envía credenciales y cambios de cuenta con el método esperado', () => {
    expectRequest(() => auth.login({ email: 'ana@example.com' }), '/auth/login', { method: 'POST', body: { email: 'ana@example.com' } });
    expectRequest(auth.getCurrentUser, '/users/me', { method: 'GET' });
    expectRequest(auth.logout, '/auth/logout', { method: 'POST' });
    expectRequest(users.getMe, '/users/me', { method: 'GET' });
    expectRequest(() => users.updateMyAccount({ name: 'Ana' }), '/users/me/account', { method: 'PATCH', body: { name: 'Ana' } });
    expectRequest(() => users.deleteMyAccount({ password: 'clave' }), '/users/me', { method: 'DELETE', body: { password: 'clave' } });
  });

  it('codifica filtros de usuarios y operaciones administrativas', () => {
    users.getUsers({ page: 2, size: 15, search: '  Ana  ', searchFields: ['NAME', 'EMAIL'], sortOrder: 'NAME_DESC' });
    const [path, options] = apiClient.mock.lastCall;
    const url = new URL(path, 'https://example.test');
    expect(url.pathname).toBe('/users');
    expect(Object.fromEntries(url.searchParams)).toEqual({ page: '2', size: '15', sortOrder: 'NAME_DESC', searchFields: 'NAME,EMAIL', search: 'Ana' });
    expect(options).toEqual({ method: 'GET' });
    expectRequest(() => users.updateUserRole(3, 'ADMIN'), '/users/3/role', { method: 'PATCH', body: { role: 'ADMIN' } });
    expectRequest(() => users.deleteUser(3), '/users/3', { method: 'DELETE' });
  });

  it('gestiona categorías y vistas de administración', () => {
    categories.getCategories({ surveyOnly: true });
    expect(apiClient).toHaveBeenLastCalledWith('/categories?surveyOnly=true');
    expectRequest(() => categories.createCategory('Salud', 'heart'), '/categories', { method: 'POST', body: { name: 'Salud', icon: 'heart' } });
    expectRequest(() => categories.updateCategoryIcon('a/b', 'leaf'), '/categories/a%2Fb/icon', { method: 'PATCH', body: { icon: 'leaf' } });
    expectRequest(dashboard.getAdminDashboard, '/admin/dashboard', { method: 'GET' });
    expectRequest(elements.getPendingElementRequests, '/admin/element-requests', { method: 'GET' });
    expectRequest(() => elements.createElementRequest({ type: 'QUESTION' }), '/element-requests', { method: 'POST', body: { type: 'QUESTION' } });
    expectRequest(() => elements.approveElementRequest(8), '/admin/element-requests/8/approve', { method: 'POST' });
    expectRequest(() => elements.rejectElementRequest(8), '/admin/element-requests/8', { method: 'DELETE' });
  });

  it('resuelve solicitudes de registro y colaboración', () => {
    expectRequest(() => registration.createRegistrationRequest({ email: 'ana@example.com' }), '/registration-requests', { method: 'POST', body: { email: 'ana@example.com' } });
    expectRequest(registration.getPendingRegistrationRequests, '/admin/registration-requests', { method: 'GET' });
    expectRequest(() => registration.approveRegistrationRequest(9), '/admin/registration-requests/9/approve', { method: 'POST' });
    expectRequest(() => registration.rejectRegistrationRequest(9), '/admin/registration-requests/9', { method: 'DELETE' });
    expectRequest(() => collaboration.getCollaborators(4), '/surveys/4/collaborators', { method: 'GET' });
    expectRequest(() => collaboration.getCollaborationRequests(4), '/surveys/4/collaboration-requests', { method: 'GET' });
    expectRequest(collaboration.getCollaborationOverview, '/collaboration-requests', { method: 'GET' });
    expectRequest(() => collaboration.createCollaborationRequest(4, { email: 'ana@example.com' }), '/surveys/4/collaboration-requests', { method: 'POST', body: { email: 'ana@example.com' } });
    expectRequest(() => collaboration.acceptCollaborationRequest(4, 9), '/surveys/4/collaboration-requests/9/accept', { method: 'POST' });
    expectRequest(() => collaboration.rejectCollaborationRequest(4, 9), '/surveys/4/collaboration-requests/9', { method: 'DELETE' });
    expectRequest(() => collaboration.removeCollaborator(4, 9), '/surveys/4/collaborators/9', { method: 'DELETE' });
    expectRequest(() => collaboration.updateCollaboratorPermission(4, 9, 'EDIT'), '/surveys/4/collaborators/9', { method: 'PATCH', body: { permission: 'EDIT' } });
    expectRequest(() => collaboration.requestPublication(4, '  Revisar  '), '/surveys/4/publication-requests', { method: 'POST', body: { message: 'Revisar' } });
    expectRequest(() => collaboration.getPublicationRequests(4), '/surveys/4/publication-requests', { method: 'GET' });
    expectRequest(() => collaboration.acceptPublicationRequest(4, 9), '/surveys/4/publication-requests/9/accept', { method: 'POST' });
    expectRequest(() => collaboration.rejectPublicationRequest(4, 9), '/surveys/4/publication-requests/9', { method: 'DELETE' });
  });

  it('consulta resultados y envía participaciones por el canal adecuado', () => {
    const answers = { answers: [{ questionId: 1, textValue: 'Sí' }] };
    expectRequest(() => participation.submitPublicParticipation(4, answers), '/public/surveys/4/participations', { method: 'POST', body: answers });
    expectRequest(() => participation.submitAuthenticatedParticipation(4, answers), '/surveys/4/participations', { method: 'POST', body: answers });
    const signal = new AbortController().signal;
    expectRequest(() => participation.getSurveyParticipations(4, { page: 2, size: 5, signal }), '/surveys/4/participations?page=2&size=5', { method: 'GET', signal });
    expectRequest(() => participation.getSurveyParticipation(4, 8), '/surveys/4/participations/8', { method: 'GET' });
    expectRequest(() => statistics.getSurveyStatistics(4), '/surveys/4/statistics', { method: 'GET' });
  });

  it('sustituye el contenido de una encuesta', () => {
    const item = { textBody: 'Introducción' };
    expectRequest(() => content.replaceSurveyContent(4, [item]), '/surveys/4/content', { method: 'PUT', body: [item] });
  });
});
