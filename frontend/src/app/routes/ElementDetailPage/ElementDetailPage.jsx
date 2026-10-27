import { useEffect, useState } from 'react';
import { Link, Navigate, useLocation, useNavigate, useParams } from 'react-router-dom';
import {
  MdDeleteOutline,
  MdEdit,
  MdOutlineDescription,
  MdOutlineHelpOutline,
  MdOutlineWidgets,
} from 'react-icons/md';

import {
  deleteCatalogBlock,
  deleteCatalogQuestion,
  deleteCatalogTemplate,
  getCatalogBlockById,
  getCatalogQuestionById,
  getCatalogTemplateById,
} from '../../../api/catalogApi.js';
import { BackButton } from '../../../components/BackButton/BackButton.jsx';
import { Breadcrumbs } from '../../../components/Breadcrumbs/Breadcrumbs.jsx';
import { CatalogBlockContent } from '../../../components/CatalogBlockDetailsModal/CatalogBlockContent.jsx';
import { CatalogContentItem } from '../../../components/CatalogBlockDetailsModal/CatalogContentItem.jsx';
import { ConfirmModal } from '../../../components/ConfirmModal/ConfirmModal.jsx';
import { Question } from '../../../components/Question/Question.jsx';
import { Skeleton } from '../../../components/Skeleton/Skeleton.jsx';
import { useAuth } from '../../../context/useAuth.js';
import { buildBlockQuestionFromCatalog } from '../../../utils/blockBuilderUtils.js';
import { formatDateTime } from '../../../utils/formatDate.js';
import { QUESTION_TYPES } from '../../../utils/questionBuilderUtils.js';
import { getTemplateItemNumbering } from '../../../utils/templateBuilderUtils.js';

import '../../../components/BlockBuilder/BlockBuilder.css';
import './ElementDetailPage.css';

const ELEMENT_CONFIG = {
  bloque: {
    label: 'bloque',
    titleKey: 'name',
    descriptionKey: 'description',
    structureTitle: 'ESTRUCTURA DEL BLOQUE',
    catalogPath: '/catalogo/bloques',
    editPathPrefix: '/catalogo/editar/bloque',
    icon: MdOutlineWidgets,
    load: getCatalogBlockById,
    deleteApi: deleteCatalogBlock,
  },
  pregunta: {
    label: 'pregunta',
    titleKey: 'statement',
    descriptionKey: null,
    structureTitle: 'ESTRUCTURA DE LA PREGUNTA',
    catalogPath: '/catalogo/preguntas',
    editPathPrefix: '/catalogo/editar/pregunta',
    icon: MdOutlineHelpOutline,
    load: getCatalogQuestionById,
    deleteApi: deleteCatalogQuestion,
  },
  plantilla: {
    label: 'plantilla',
    titleKey: 'title',
    descriptionKey: 'description',
    structureTitle: 'ESTRUCTURA DE LA PLANTILLA',
    catalogPath: '/catalogo/plantillas',
    editPathPrefix: '/catalogo/editar/plantilla',
    icon: MdOutlineDescription,
    load: getCatalogTemplateById,
    deleteApi: deleteCatalogTemplate,
  },
};

const SCALE_DISPLAY_LABELS = {
  NUMERIC: 'Numérica',
  STARS: 'Estrellas',
  FACES: 'Caras',
};

function getQuestionSettingRows(question) {
  const settings = question.settings || {};
  const questionType = QUESTION_TYPES.find((type) => type.id === question.questionType);
  const rows = [
    { label: 'Tipo de pregunta', value: questionType?.title || question.questionType },
  ];

  function add(label, value) {
    if (value !== null && value !== undefined && value !== '') {
      rows.push({ label, value });
    }
  }

  if (question.questionType === 'TEXT') {
    add('Longitud máxima', settings.maxLength);
  }

  if (['NUMBER', 'SCALE', 'MATRIX_SCALE'].includes(question.questionType)) {
    add('Valor mínimo', settings.minValue);
    add('Valor máximo', settings.maxValue);
    add('Paso', settings.step);
  }

  if (['SCALE', 'MATRIX_SCALE'].includes(question.questionType)) {
    add('Etiqueta inicial', settings.leftLabel);
    add('Etiqueta final', settings.rightLabel);
    add('Tipo visual', SCALE_DISPLAY_LABELS[settings.scaleDisplayType] || settings.scaleDisplayType);
  }

  if (question.questionType === 'MULTIPLE_CHOICE') {
    add('Selecciones mínimas', settings.minSelections);
    add('Selecciones máximas', settings.maxSelections);
  }

  if (question.questionType === 'BOOLEAN') {
    add('Respuesta afirmativa', settings.trueLabel);
    add('Respuesta negativa', settings.falseLabel);
  }

  return rows;
}

function QuestionSettings({ question }) {
  const rows = getQuestionSettingRows(question);

  return (
    <section className="catalog-detail-settings" aria-labelledby="catalog-detail-settings-title">
      <header className="catalog-detail-settings-header">
        <h3 id="catalog-detail-settings-title">CONFIGURACIÓN DE LA PREGUNTA</h3>
      </header>
      <dl className="catalog-detail-settings-grid">
        {rows.map((row) => (
          <div key={row.label}>
            <dt>{row.label}</dt>
            <dd>{row.value ?? '—'}</dd>
          </div>
        ))}
      </dl>
    </section>
  );
}

function CatalogElementContent({ elementType, element }) {
  if (elementType === 'pregunta') {
    return (
      <div className="catalog-detail-question">
        <Question
          questionNumber=""
          headingLevel={4}
          data={buildBlockQuestionFromCatalog(element)}
          isCatalog
          interactive={false}
        />
      </div>
    );
  }

  if (elementType === 'bloque') {
    return <CatalogBlockContent element={element} />;
  }

  const items = element.contentItems || [];
  const itemNumbering = getTemplateItemNumbering(items);

  return (
    <div className="block-questions-list catalog-detail-template-items">
      {items.map((item, index) => (
        <CatalogContentItem
          key={item.id || index}
          item={item}
          numbering={itemNumbering[index]}
        />
      ))}
    </div>
  );
}

export function ElementDetailPage() {
  const { elementType, elementId } = useParams();
  return <ElementDetailContent key={`${elementType}:${elementId}`} />;
}

function ElementDetailContent() {
  const location = useLocation();
  const navigate = useNavigate();
  const { elementType, elementId } = useParams();
  const { user } = useAuth();
  const config = ELEMENT_CONFIG[elementType];
  const initialElement = location.state?.element
    && location.state.elementType === elementType
    && String(location.state.elementId) === String(elementId)
    ? location.state.element
    : null;

  const [element, setElement] = useState(initialElement);
  const [isLoading, setIsLoading] = useState(!initialElement);
  const [loadError, setLoadError] = useState('');
  const [isDeleteOpen, setIsDeleteOpen] = useState(false);
  const [isDeleting, setIsDeleting] = useState(false);
  const [deleteError, setDeleteError] = useState('');

  useEffect(() => {
    if (!config || initialElement) return undefined;

    let isMounted = true;

    config.load(elementId)
      .then((response) => {
        if (!isMounted) return;
        setElement(response);
        navigate(location.pathname, {
          replace: true,
          state: {
            ...location.state,
            element: response,
            elementId,
            elementType,
          },
        });
      })
      .catch((error) => {
        if (isMounted) {
          setLoadError(error.message || 'No se ha podido cargar el elemento.');
        }
      })
      .finally(() => {
        if (isMounted) setIsLoading(false);
      });

    return () => {
      isMounted = false;
    };
  }, [config, elementId, elementType, initialElement, location.pathname, location.state, navigate]);

  if (!config) return <Navigate to="/catalogo" replace />;

  const canManage = Boolean(
    element
    && (user?.role === 'SUPERADMIN' || String(user?.id) === String(element.creatorId)),
  );
  const ElementIcon = config.icon;
  const rawTitle = element?.[config.titleKey] || `Sin ${config.label}`;
  const title = elementType === 'bloque' ? rawTitle.toUpperCase() : rawTitle;

  async function handleDelete() {
    setIsDeleting(true);
    setDeleteError('');

    try {
      await config.deleteApi(elementId);
      navigate(config.catalogPath, { replace: true });
    } catch (error) {
      setDeleteError(error.message || 'No se ha podido eliminar el elemento.');
      setIsDeleting(false);
    }
  }

  return (
    <main className="catalog-detail-page">
      <div className="catalog-detail-container">
        <BackButton fallbackTo={config.catalogPath} className="catalog-detail-back" />

        <Breadcrumbs
          className="catalog-detail-breadcrumb"
          items={[
            { label: 'PANEL', to: '/panel' },
            { label: 'CATÁLOGO', to: config.catalogPath },
            { label: 'VER DETALLES' },
          ]}
        />

        <header className="catalog-detail-page-header">
          <h1>VER DETALLES:</h1>
          <p>Aquí puedes ver los detalles del elemento</p>
        </header>

        {isLoading && (
          <div className="catalog-detail-loading">
            <Skeleton variant="list" count={3} hasActions={false} />
          </div>
        )}

        {!isLoading && loadError && (
          <section className="catalog-detail-error" role="alert">
            <p>{loadError}</p>
            <Link to={config.catalogPath}>Volver al catálogo</Link>
          </section>
        )}

        {!isLoading && element && (
          <div className="catalog-detail-card">
            <header className="catalog-detail-title-bar">
              <span className="catalog-detail-title-icon" aria-hidden="true">
                <ElementIcon />
              </span>
              <h2>{title}</h2>
            </header>

            <div className="catalog-detail-body">
              {config.descriptionKey && (
                <p className="catalog-detail-description">
                  {element[config.descriptionKey] || 'Sin descripción.'}
                </p>
              )}

              <dl className="catalog-detail-metadata">
                <div>
                  <dt>CREADOR</dt>
                  <dd>{element.creatorName || '—'}</dd>
                </div>
                <div>
                  <dt>CREADO</dt>
                  <dd>{formatDateTime(element.createdAt)}</dd>
                </div>
                <div>
                  <dt>ÚLTIMA ACTUALIZACIÓN</dt>
                  <dd>{formatDateTime(element.updatedAt)}</dd>
                </div>
              </dl>

              {elementType === 'pregunta' && <QuestionSettings question={element} />}

              <section
                className={`catalog-detail-structure ${elementType === 'bloque' ? 'catalog-detail-structure--block' : ''}`}
                aria-labelledby="catalog-detail-structure-title"
              >
                <header className="catalog-detail-structure-header">
                  <div>
                    <h3 id="catalog-detail-structure-title">{config.structureTitle}</h3>
                  </div>
                  {canManage && (
                    <div className="catalog-detail-actions">
                      <button
                        type="button"
                        aria-label={`Editar ${config.label}`}
                        onClick={() => navigate(`${config.editPathPrefix}/${elementId}`, {
                          state: {
                            element,
                            elementId,
                            elementType,
                            fromDetails: true,
                          },
                        })}
                      >
                        <MdEdit aria-hidden="true" />
                      </button>
                      <button
                        type="button"
                        className="catalog-detail-delete"
                        aria-label={`Eliminar ${config.label}`}
                        onClick={() => {
                          setDeleteError('');
                          setIsDeleteOpen(true);
                        }}
                      >
                        <MdDeleteOutline aria-hidden="true" />
                      </button>
                    </div>
                  )}
                </header>

                <div className="catalog-detail-structure-content">
                  <CatalogElementContent elementType={elementType} element={element} />
                </div>
              </section>
            </div>
          </div>
        )}
      </div>

      <ConfirmModal
        isOpen={isDeleteOpen}
        onClose={() => {
          if (isDeleting) return;
          setIsDeleteOpen(false);
          setDeleteError('');
        }}
        onConfirm={handleDelete}
        title={`Eliminar ${config.label}`}
        message={`¿Confirmas que deseas eliminar “${title}”? Esta acción no se puede deshacer.`}
        confirmLabel="Eliminar"
        variant="danger"
        isProcessing={isDeleting}
        errorMessage={deleteError}
      />
    </main>
  );
}
