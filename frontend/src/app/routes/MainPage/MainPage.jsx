import { useEffect, useMemo, useState } from 'react';
import { MdFilterList, MdLockOutline, MdSearch } from 'react-icons/md';

import { getPublicSurveys, getRegisteredSurveys } from '../../../api/surveysApi.js';
import bannerSmall from '../../../assets/images/banner-small.png';
import { Skeleton } from '../../../components/Skeleton/Skeleton.jsx';
import { SurveyCard } from '../../../components/SurveyCard/SurveyCard.jsx';
import { useAuth } from '../../../context/AuthContext.jsx';

import './MainPage.css';

const ACCESS_FILTERS = {
  ALL: 'ALL',
  DIRECT: 'DIRECT',
  PASSWORD: 'PASSWORD_PROTECTED',
};

function normalizeSearchText(value) {
  return String(value || '')
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .toLocaleLowerCase('es');
}

function getLoadErrorMessage(error) {
  return error?.status
    ? error.message
    : 'No hemos podido conectar con el servidor. Comprueba tu conexión e inténtalo de nuevo.';
}

/**
 * Página principal de FormQuid.
 *
 * Presenta las encuestas disponibles y separa las de acceso directo de las
 * que requieren contraseña.
 */
export function MainPage() {
  const { isAuthenticated, isAuthLoading } = useAuth();
  const [surveys, setSurveys] = useState([]);
  const [search, setSearch] = useState('');
  const [accessFilter, setAccessFilter] = useState(ACCESS_FILTERS.ALL);
  const [filtersOpen, setFiltersOpen] = useState(false);
  const [isLoading, setIsLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState('');
  const [requestVersion, setRequestVersion] = useState(0);

  useEffect(() => {
    if (isAuthLoading) return undefined;

    let isActive = true;
    const request = isAuthenticated ? getRegisteredSurveys : getPublicSurveys;

    Promise.resolve().then(() => {
      if (!isActive) return;
      setIsLoading(true);
      setErrorMessage('');
    });

    request()
      .then((response) => {
        if (isActive) {
          setSurveys(Array.isArray(response) ? response : []);
        }
      })
      .catch((error) => {
        if (!isActive) return;
        setSurveys([]);
        setErrorMessage(getLoadErrorMessage(error));
      })
      .finally(() => {
        if (isActive) setIsLoading(false);
      });

    return () => {
      isActive = false;
    };
  }, [isAuthenticated, isAuthLoading, requestVersion]);

  const filteredSurveys = useMemo(() => {
    const normalizedSearch = normalizeSearchText(search.trim());

    return surveys.filter((survey) => {
      const matchesSearch = !normalizedSearch || normalizeSearchText(
        `${survey.title || ''} ${survey.description || ''} ${(survey.tags || []).join(' ')}`,
      ).includes(normalizedSearch);
      const isPasswordProtected = survey.accessMode === 'PASSWORD_PROTECTED';
      const matchesAccess = accessFilter === ACCESS_FILTERS.ALL
        || (accessFilter === ACCESS_FILTERS.PASSWORD && isPasswordProtected)
        || (accessFilter === ACCESS_FILTERS.DIRECT && !isPasswordProtected);

      return matchesSearch && matchesAccess;
    });
  }, [accessFilter, search, surveys]);

  const directSurveys = filteredSurveys.filter(
    (survey) => survey.accessMode !== 'PASSWORD_PROTECTED',
  );
  const passwordSurveys = filteredSurveys.filter(
    (survey) => survey.accessMode === 'PASSWORD_PROTECTED',
  );
  const hasResults = filteredSurveys.length > 0;
  const showDirectSection = accessFilter !== ACCESS_FILTERS.PASSWORD;
  const showPasswordSection = accessFilter !== ACCESS_FILTERS.DIRECT;

  return (
    <main className="main-page" aria-labelledby="main-page-title">
      <section className="main-page-hero" aria-labelledby="main-page-title">
        <picture className="main-page-hero-picture" aria-hidden="true">
          <img src={bannerSmall} alt="" />
        </picture>
        <div className="main-page-hero-content">
          <h1 id="main-page-title">La tecnología<br />desde tu perspectiva</h1>
          <p>
            Descubre cómo la infancia vive la tecnología y participa en
            encuestas que nos ayudan a comprender su mundo digital.
          </p>
        </div>
      </section>

      <div className="main-page-surveys">
        <div className="main-page-surveys-header">
          <div>
            <h2 id="available-surveys-title">NUESTRAS ENCUESTAS</h2>
            <p>Explora las encuestas disponibles y comparte tu opinión.</p>
          </div>

          <div className="main-page-controls" role="search">
            <label className="main-page-search">
              <span className="sr-only">Buscar encuestas</span>
              <MdSearch aria-hidden="true" />
              <input
                type="search"
                value={search}
                placeholder="Buscar encuestas"
                onChange={(event) => setSearch(event.target.value)}
              />
            </label>
            <button
              type="button"
              className="main-page-filter-button"
              aria-expanded={filtersOpen}
              aria-controls="main-page-access-filters"
              onClick={() => setFiltersOpen((current) => !current)}
            >
              <MdFilterList aria-hidden="true" />
              <span>Filtrar</span>
            </button>
          </div>
        </div>

        {filtersOpen && (
          <fieldset id="main-page-access-filters" className="main-page-filter-panel">
            <legend>Tipo de acceso</legend>
            <label>
              <input
                type="radio"
                name="access-filter"
                value={ACCESS_FILTERS.ALL}
                checked={accessFilter === ACCESS_FILTERS.ALL}
                onChange={(event) => setAccessFilter(event.target.value)}
              />
              Todas
            </label>
            <label>
              <input
                type="radio"
                name="access-filter"
                value={ACCESS_FILTERS.DIRECT}
                checked={accessFilter === ACCESS_FILTERS.DIRECT}
                onChange={(event) => setAccessFilter(event.target.value)}
              />
              Acceso directo
            </label>
            <label>
              <input
                type="radio"
                name="access-filter"
                value={ACCESS_FILTERS.PASSWORD}
                checked={accessFilter === ACCESS_FILTERS.PASSWORD}
                onChange={(event) => setAccessFilter(event.target.value)}
              />
              Con contraseña
            </label>
          </fieldset>
        )}

        {!isLoading && !errorMessage && (
          <p className="main-page-results-status" role="status" aria-live="polite">
            {filteredSurveys.length === 1
              ? '1 encuesta disponible'
              : `${filteredSurveys.length} encuestas disponibles`}
          </p>
        )}

        {errorMessage && (
          <div className="main-page-error" role="alert">
            <p>{errorMessage}</p>
            <button
              type="button"
              onClick={() => {
                setIsLoading(true);
                setErrorMessage('');
                setRequestVersion((version) => version + 1);
              }}
            >
              Reintentar
            </button>
          </div>
        )}

        <div className="main-page-results" aria-busy={isLoading} aria-labelledby="available-surveys-title">
          {isLoading ? (
            <>
              <span className="sr-only" role="status">Cargando encuestas...</span>
              <Skeleton variant="grid" count={3} hasActions={false} />
            </>
          ) : !errorMessage && !hasResults ? (
            <div className="main-page-empty" role="status">
              <p>
                {search || accessFilter !== ACCESS_FILTERS.ALL
                  ? 'No encontramos encuestas que coincidan con tu búsqueda.'
                  : 'No hay encuestas disponibles en este momento.'}
              </p>
              {(search || accessFilter !== ACCESS_FILTERS.ALL) && (
                <button
                  type="button"
                  onClick={() => {
                    setSearch('');
                    setAccessFilter(ACCESS_FILTERS.ALL);
                  }}
                >
                  Ver todas las encuestas
                </button>
              )}
            </div>
          ) : !errorMessage && (
            <>
              {showDirectSection && directSurveys.length > 0 && (
                <section className="main-page-survey-section" aria-label="Encuestas de acceso directo">
                  <div className="main-page-survey-grid">
                    {directSurveys.map((survey) => (
                      <SurveyCard
                        key={survey.publicCode}
                        survey={survey}
                        headingLevel={3}
                        variant="public"
                        accessHref={!isAuthenticated
                          ? `/encuestas/${encodeURIComponent(survey.publicCode)}/responder`
                          : undefined}
                      />
                    ))}
                  </div>
                </section>
              )}

              {showPasswordSection && passwordSurveys.length > 0 && (
                <section className="main-page-survey-section main-page-password-section" aria-labelledby="password-surveys-title">
                  <header>
                    <MdLockOutline aria-hidden="true" />
                    <div>
                      <h2 id="password-surveys-title">ENCUESTAS CON CONTRASEÑA</h2>
                      <p>Necesitarás la contraseña de la encuesta para poder participar.</p>
                    </div>
                  </header>
                  <div className="main-page-survey-grid">
                    {passwordSurveys.map((survey) => (
                      <SurveyCard
                        key={survey.publicCode}
                        survey={survey}
                        headingLevel={3}
                        variant="public"
                        accessHref={!isAuthenticated
                          ? `/encuestas/${encodeURIComponent(survey.publicCode)}/responder`
                          : undefined}
                      />
                    ))}
                  </div>
                </section>
              )}
            </>
          )}
        </div>
      </div>
    </main>
  );
}
