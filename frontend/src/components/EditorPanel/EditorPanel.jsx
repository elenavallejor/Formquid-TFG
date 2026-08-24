import { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import {
  MdAdd,
  MdPeopleOutline,
  MdOutlineBarChart,
  MdOutlineLightbulb,
  MdSearch,
} from 'react-icons/md';

import { SurveyCard } from '../../components/SurveyCard/SurveyCard.jsx';
import './EditorPanel.css';

const ACTION_CARDS = [
  {
    title: 'Crear encuesta',
    description: 'Crea una nueva encuesta desde cero o a partir de una plantilla.',
    icon: MdAdd,
    to: '/encuestas/crear',
  },
  {
    title: 'Colaboraciones',
    description: 'Gestiona colaboradores, solicitudes y permisos de tus encuestas.',
    icon: MdPeopleOutline,
    to: '/colaboraciones',
  },
  {
    title: 'Resultados',
    description: 'Consulta y analiza los resultados de tus encuestas.',
    icon: MdOutlineBarChart,
    to: '/resultados',
  },
  {
    title: 'Sugerir elemento',
    description: 'Propón elementos que puedan ser útiles para otros editores.',
    icon: MdOutlineLightbulb,
    to: '/sugerir-elemento',
  },
];

export function EditorPanel() {
  const navigate = useNavigate();
  const [searchQuery, setSearchQuery] = useState('');
  const [statusFilter, setStatusFilter] = useState('ALL');
  const [sortOrder, setSortOrder] = useState('RECENT');

  // MOCK DE ENCUESTAS DEL EDITOR
  const [surveys] = useState([
    {
      id: '1',
      title: 'Uso de gafas inteligentes en aulas',
      status: 'OPEN',
      tags: ['Educación', 'Tecnología'],
    },
    {
      id: '2',
      title: 'Asistente de exploración del entorno',
      status: 'CLOSED',
      tags: ['Accesibilidad', 'Innovación'],
    },
    {
      id: '3',
      title: 'Historias de los abuelos con realidad aumentada',
      status: 'DRAFT',
      tags: ['Cultura', 'AR'],
    },
    {
      id: '4',
      title: 'Evaluación de servicios 2026',
      status: 'OPEN',
      tags: ['Calidad', 'Servicios'],
    },
  ]);

  const filteredSurveys = surveys.filter((survey) => {
    const matchSearch = survey.title.toLowerCase().includes(searchQuery.toLowerCase());
    const matchStatus = statusFilter === 'ALL' || survey.status === statusFilter;
    return matchSearch && matchStatus;
  });

  return (
    <main className="editor-panel-page">
      <section className="editor-top-section">
        <div className="editor-container">
          <header className="editor-header">
            <h1>PANEL DE EDITOR</h1>
            <p>Desde aquí puedes crear y gestionar elementos y encuestas.</p>
          </header>

          <div className="editor-actions-grid">
            {ACTION_CARDS.map((card, idx) => {
              const Icon = card.icon;
              return (
                <article key={idx} className="editor-action-card">
                  <div className="action-card-icon-circle">
                    <Icon aria-hidden="true" />
                  </div>
                  <h2 className="action-card-title">{card.title}</h2>
                  <p className="action-card-description">{card.description}</p>
                  <button
                    type="button"
                    className="action-card-btn"
                    onClick={() => navigate(card.to)}
                  >
                    Acceder
                  </button>
                </article>
              );
            })}
          </div>
        </div>
      </section>

      <section className="editor-surveys-section">
        <div className="editor-container">
          <div className="surveys-header-row">
            <div>
              <h2 className="surveys-section-title">Mis encuestas</h2>
              <p className="surveys-section-subtitle">
                Encuestas en las que has participado o creado.
              </p>
            </div>
            <Link to="/admin/encuestas" className="btn-see-all">
              Ver todas
            </Link>
          </div>

          <div className="surveys-filters-bar">
            <div className="surveys-search-wrapper">
              <MdSearch className="search-icon" aria-hidden="true" />
              <input
                type="text"
                placeholder="Buscar encuestas..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
              />
            </div>

            <div className="surveys-selects-group">
              <select
                value={statusFilter}
                onChange={(e) => setStatusFilter(e.target.value)}
              >
                <option value="ALL">Todos los estados</option>
                <option value="OPEN">Abierta</option>
                <option value="DRAFT">Borrador</option>
                <option value="CLOSED">Cerrada</option>
              </select>

              <select
                value={sortOrder}
                onChange={(e) => setSortOrder(e.target.value)}
              >
                <option value="RECENT">Más recientes primero</option>
                <option value="OLDEST">Más antiguas primero</option>
              </select>
            </div>
          </div>

          <div className="surveys-cards-grid">
            {filteredSurveys.map((survey) => (
              <SurveyCard
                key={survey.id}
                survey={survey}
                onEdit={(id) => navigate(`/encuestas/${id}/editar`)}
                onView={(id) => navigate(`/encuestas/${id}`)}
                onMoreOptions={(id) => console.log('Opciones', id)}
              />
            ))}
          </div>
        </div>
      </section>
    </main>
  );
}