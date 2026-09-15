import { useEffect, useState } from 'react';
import { Link, Navigate, useNavigate, useParams } from 'react-router-dom';
import {
  MdChevronRight,
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
import { Block } from '../../../components/Block/Block.jsx';
import { ConfirmModal } from '../../../components/ConfirmModal/ConfirmModal.jsx';
import { Question } from '../../../components/Question/Question.jsx';
import { Skeleton } from '../../../components/Skeleton/Skeleton.jsx';
import { TextContent, TextContentIcon } from '../../../components/TextContent/TextContent.jsx';
import { useAuth } from '../../../context/AuthContext.jsx';
import { buildBlockQuestionFromCatalog, getBlockItems } from '../../../utils/blockBuilderUtils.js';
import { formatDate } from '../../../utils/formatDate.js';
import { QUESTION_TYPES } from '../../../utils/questionBuilderUtils.js';

import './CatalogDetailPage.css';

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
    showUsage: true,
    usedLabel: 'usado',
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
    showUsage: true,
    usedLabel: 'usada',
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
    showUsage: false,
  },
};

function formatDateTime(value) {
  const formatted = formatDate(value, { includeTime: true });
  return formatted.time ? `${formatted.date}, ${formatted.time}` : formatted.date;
}

function CatalogBlockContent({ element }) {
  const items = Array.isArray(element.contentItems)
    ? element.contentItems
    : getBlockItems(element);
  let questionNumber = 0;

  return (
    <div className="catalog-detail-block-items">
      {items.map((item, index) => {
        const isText = item.itemType === 'TEXT';
        const questionData = isText
          ? null
          : {
              ...buildBlockQuestionFromCatalog(item.question || item),
              isRequired: Boolean(item.required ?? item.isRequired),
            };
        const typeConfig = isText
          ? null
          : QUESTION_TYPES.find((type) => type.id === questionData.questionType);
        const TypeIcon = typeConfig?.icon;

        if (!isText) questionNumber += 1;

        return (
          <div key={item.id || index} className="catalog-detail-block-item">
            <div className="catalog-detail-block-item-icon">
              {isText ? (
                <TextContentIcon />
              ) : TypeIcon ? (
                <span
                  className="catalog-detail-question-type-badge"
                  style={{ color: typeConfig.color, borderColor: `${typeConfig.color}60` }}
                  title={typeConfig.title}
                >
                  <TypeIcon aria-hidden="true" />
                </span>
              ) : null}
            </div>

            <div className="catalog-detail-block-item-preview">
              {isText ? (
                <TextContent data={item} />
              ) : (
                <Question
                  questionNumber={`${questionNumber}`}
                  data={questionData}
                  interactive={false}
                />
              )}
            </div>
          </div>
        );
      })}
    </div>
  );
}

function CatalogElementContent({ elementType, element }) {
  if (elementType === 'pregunta') {
    return (
      <div className="catalog-detail-question">
        <Question
          questionNumber=""
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

  let questionNumber = 0;

  return (
    <div className="catalog-detail-template-items">
      {(element.contentItems || []).map((item, index) => {
        if (item.itemType === 'TEXT') {
          return (
            <div key={item.id || index} className="catalog-detail-content-item">
              <TextContent data={item} />
            </div>
          );
        }

        if (item.itemType === 'BLOCK') {
          return (
            <div key={item.id || index} className="catalog-detail-content-item">
              <Block data={item.block} />
            </div>
          );
        }

        questionNumber += 1;
        return (
          <div key={item.id || index} className="catalog-detail-content-item catalog-detail-question">
            <Question
              questionNumber={`${questionNumber}`}
              data={{
                ...buildBlockQuestionFromCatalog(item.question),
                isRequired: Boolean(item.required),
              }}
              interactive={false}
            />
          </div>
        );
      })}
    </div>
  );
}

export function CatalogDetailPage() {
  const navigate = useNavigate();
  const { elementType, elementId } = useParams();
  const { user } = useAuth();
  const config = ELEMENT_CONFIG[elementType];

  const [element, setElement] = useState(null);
  const [isLoading, setIsLoading] = useState(true);
  const [loadError, setLoadError] = useState('');
  const [isDeleteOpen, setIsDeleteOpen] = useState(false);
  const [isDeleting, setIsDeleting] = useState(false);
  const [deleteError, setDeleteError] = useState('');

  useEffect(() => {
    if (!config) return undefined;

    let isMounted = true;
    setElement(null);
    setIsLoading(true);
    setLoadError('');

    config.load(elementId)
      .then((response) => {
        if (isMounted) setElement(response);
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
  }, [config, elementId]);

  if (!config) return <Navigate to="/catalogo" replace />;

  const canManage = Boolean(
    element
    && (user?.role === 'SUPERADMIN' || String(user?.id) === String(element.creatorId)),
  );
  const ElementIcon = config.icon;
  const title = element?.[config.titleKey] || `Sin ${config.label}`;

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
        <nav className="catalog-detail-breadcrumb" aria-label="Migas de pan">
          <Link to="/panel">PANEL</Link>
          <MdChevronRight aria-hidden="true" />
          <Link to={config.catalogPath}>CATÁLOGO</Link>
          <MdChevronRight aria-hidden="true" />
          <span aria-current="page">VER DETALLES</span>
        </nav>

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
          <article className="catalog-detail-card">
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
                  <dt>CREADOR:</dt>
                  <dd>{element.creatorName || '—'}</dd>
                </div>
                <div>
                  <dt>CREADO:</dt>
                  <dd>{formatDateTime(element.createdAt)}</dd>
                </div>
                <div>
                  <dt>ÚLTIMA ACTUALIZACIÓN:</dt>
                  <dd>{formatDateTime(element.updatedAt)}</dd>
                </div>
              </dl>

              {config.showUsage && (
                <p className="catalog-detail-usage">
                  Ha sido {config.usedLabel} {element.usageCount ?? 0}{' '}
                  {(element.usageCount ?? 0) === 1 ? 'vez' : 'veces'} en elementos del catálogo y encuestas.
                </p>
              )}

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
                        onClick={() => navigate(`${config.editPathPrefix}/${elementId}`)}
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
          </article>
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
