import { useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { MdOutlineManageSearch, MdSearch } from 'react-icons/md';

import { getAllAdminSurveys } from '../../../api/surveysApi.js';
import { Breadcrumbs } from '../../../components/Breadcrumbs/Breadcrumbs.jsx';
import { Dropdown } from '../../../components/Dropdown/Dropdown.jsx';
import { ErrorMessage } from '../../../components/ErrorMessage/ErrorMessage.jsx';
import { Skeleton } from '../../../components/Skeleton/Skeleton.jsx';
import { Table } from '../../../components/Table/Table.jsx';

import './AdminSurveysPage.css';

const LIFECYCLE_LABELS = { DRAFT: 'Borrador', OPEN: 'Abierta', CLOSED: 'Cerrada' };
const ACCESS_LABELS = { PUBLIC: 'Público', PASSWORD_PROTECTED: 'Con contraseña', AUTHENTICATED: 'Usuarios autenticados' };
const VISIBILITY_LABELS = { LISTED: 'Listada', UNLISTED: 'Oculta' };
const TABLE_HEADERS = ['Título', 'Propietario', 'Ciclo de vida', 'Acceso', 'Visibilidad', 'Respuestas', 'Acción'];

export function AdminSurveysPage() {
  const [surveys, setSurveys] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState('');
  const [search, setSearch] = useState('');
  const [lifecycle, setLifecycle] = useState('ALL');
  const [visibility, setVisibility] = useState('ALL');

  useEffect(() => {
    let isMounted = true;
    getAllAdminSurveys()
      .then((response) => {
        if (isMounted) setSurveys(Array.isArray(response) ? response : []);
      })
      .catch((error) => {
        if (isMounted) setErrorMessage(error.message || 'No se han podido cargar las encuestas.');
      })
      .finally(() => {
        if (isMounted) setIsLoading(false);
      });
    return () => { isMounted = false; };
  }, []);

  const visibleSurveys = useMemo(() => {
    const query = search.trim().toLocaleLowerCase('es');
    return surveys.filter((survey) => {
      const matchesSearch = !query || survey.title?.toLocaleLowerCase('es').includes(query) || survey.editorName?.toLocaleLowerCase('es').includes(query);
      const matchesLifecycle = lifecycle === 'ALL' || survey.lifecycleStatus === lifecycle;
      const matchesVisibility = visibility === 'ALL' || survey.visibility === visibility;
      return matchesSearch && matchesLifecycle && matchesVisibility;
    });
  }, [lifecycle, search, surveys, visibility]);

  const tableData = visibleSurveys.map((survey) => [
    survey.title || 'Sin título',
    survey.editorName || 'Sin propietario',
    <span key={`lifecycle-${survey.id}`} className={`table-badge table-badge-${String(survey.lifecycleStatus || 'DRAFT').toLowerCase()}`}>{LIFECYCLE_LABELS[survey.lifecycleStatus] || survey.lifecycleStatus}</span>,
    ACCESS_LABELS[survey.accessMode] || survey.accessMode,
    <span key={`visibility-${survey.id}`} className={`table-badge table-badge-${String(survey.visibility || 'LISTED').toLowerCase()}`}>{VISIBILITY_LABELS[survey.visibility] || survey.visibility}</span>,
    survey.responses ?? 0,
    <Link key={`action-${survey.id}`} className="admin-surveys-action" to={`/encuestas/${survey.id}/editar`}><MdOutlineManageSearch aria-hidden="true" />Supervisar</Link>,
  ]);

  return (
    <main className="admin-surveys-page">
      <div className="admin-surveys-container">
        <Breadcrumbs items={[{ label: 'PANEL', to: '/panel' }, { label: 'ENCUESTAS' }]} />
        <header className="admin-surveys-header">
          <div>
            <h1>SUPERVISIÓN DE ENCUESTAS</h1>
            <p>Consulta todas las encuestas y accede a sus controles de estado y visibilidad.</p>
          </div>
        </header>

        <section className="admin-surveys-filters" aria-label="Filtros de encuestas">
          <div className="admin-surveys-search">
            <MdSearch aria-hidden="true" />
            <input type="search" value={search} placeholder="Buscar por título o propietario" aria-label="Buscar encuestas" onChange={(event) => setSearch(event.target.value)} />
          </div>
          <Dropdown value={lifecycle} aria-label="Filtrar por ciclo de vida" onChange={(event) => setLifecycle(event.target.value)}>
            <option value="ALL">Todos los estados</option>
            <option value="DRAFT">Borrador</option>
            <option value="OPEN">Abiertas</option>
            <option value="CLOSED">Cerradas</option>
          </Dropdown>
          <Dropdown value={visibility} aria-label="Filtrar por visibilidad" onChange={(event) => setVisibility(event.target.value)}>
            <option value="ALL">Todas las visibilidades</option>
            <option value="LISTED">Listadas</option>
            <option value="UNLISTED">Ocultas</option>
          </Dropdown>
        </section>

        {errorMessage && <ErrorMessage message={errorMessage} onClose={() => setErrorMessage('')} />}
        <section className="admin-surveys-results" aria-busy={isLoading}>
          {isLoading
            ? <Skeleton variant="table" count={6} />
            : <Table headers={TABLE_HEADERS} data={tableData} emptyMessage="No hay encuestas que coincidan con los filtros." />}
        </section>
      </div>
    </main>
  );
}
