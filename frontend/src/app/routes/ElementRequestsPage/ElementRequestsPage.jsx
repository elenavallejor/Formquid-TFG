import { useEffect, useMemo, useRef, useState } from 'react';
import {
  MdCheck,
  MdClose,
  MdExpandLess,
  MdExpandMore,
  MdKeyboardArrowRight,
  MdLayers,
  MdOutlineBusiness,
  MdOutlineCalendarToday,
  MdOutlineDescription,
  MdOutlineEmail,
  MdOutlineHelpOutline,
  MdOutlineWidgets,
  MdPublic,
} from 'react-icons/md';
import { getCountryName } from '../../../utils/countries.js';

import {
  approveElementRequest,
  getPendingElementRequests,
  rejectElementRequest,
} from '../../../api/elementRequestsApi.js';
import { Block } from '../../../components/Block/Block.jsx';
import { Breadcrumbs } from '../../../components/Breadcrumbs/Breadcrumbs.jsx';
import { ConfirmModal } from '../../../components/ConfirmModal/ConfirmModal.jsx';
import { Question } from '../../../components/Question/Question.jsx';
import { SearchToolbar } from '../../../components/SearchToolbar/SearchToolbar.jsx';
import { Skeleton } from '../../../components/Skeleton/Skeleton.jsx';
import { TextContent, TextContentIcon } from '../../../components/TextContent/TextContent.jsx';
import { buildBlockQuestionFromCatalog } from '../../../utils/blockBuilderUtils.js';
import { QUESTION_TYPES } from '../../../utils/questionBuilderUtils.js';
import { getTemplateItemNumbering } from '../../../utils/templateBuilderUtils.js';

import '../../../components/BlockBuilder/BlockBuilder.css';
import './ElementRequestsPage.css';

const TYPE_CONFIG = {
  FIXED_QUESTION: {
    label: 'Pregunta solicitada',
    structureLabel: 'Estructura de la pregunta',
    icon: MdOutlineHelpOutline,
  },
  BLOCK: {
    label: 'Bloque solicitado',
    structureLabel: 'Estructura del bloque',
    icon: MdOutlineWidgets,
  },
  TEMPLATE: {
    label: 'Plantilla solicitada',
    structureLabel: 'Estructura de la plantilla',
    icon: MdOutlineDescription,
  },
};

const TYPE_OPTIONS = [
  { value: 'FIXED_QUESTION', label: 'Preguntas' },
  { value: 'BLOCK', label: 'Bloques' },
  { value: 'TEMPLATE', label: 'Plantillas' },
];
const SORT_OPTIONS = [
  { value: 'NEWEST', label: 'Más reciente' },
  { value: 'OLDEST', label: 'Más antigua' },
];

function formatDateTime(value) {
  if (!value) return 'Fecha no disponible';
  return new Intl.DateTimeFormat('es-ES', {
    dateStyle: 'short',
    timeStyle: 'short',
  }).format(new Date(value));
}

function getRequestTitle(request) {
  const payload = request.requestPayload || {};
  if (request.elementType === 'FIXED_QUESTION') return payload.question?.statement || 'Pregunta sin título';
  if (request.elementType === 'BLOCK') return payload.name || 'Bloque sin título';
  return payload.title || 'Plantilla sin título';
}

function getQuestionData(question, required = false) {
  return {
    ...buildBlockQuestionFromCatalog(question || {}),
    isRequired: Boolean(required),
  };
}

function getReferencedQuestion(id) {
  return {
    statement: `Pregunta del catálogo #${id}`,
    questionType: 'TEXT',
    settings: {},
  };
}

function normalizeBlock(block = {}) {
  return {
    ...block,
    items: (block.items || []).map((item) => {
      if (item.itemType === 'TEXT') return item;
      return {
        ...item,
        question: item.question || getReferencedQuestion(item.fixedQuestionId),
      };
    }),
  };
}

function QuestionPreview({ question, number, required = false, headingLevel = 5 }) {
  const data = getQuestionData(question, required);
  const typeConfig = QUESTION_TYPES.find((type) => type.id === data.questionType);
  const TypeIcon = typeConfig?.icon;

  return (
    <div className="element-request-preview-row">
      {TypeIcon && (
        <span
          className="question-type-badge"
          style={{ color: typeConfig.color, borderColor: `${typeConfig.color}60` }}
          title={typeConfig.title}
        >
          <TypeIcon aria-hidden="true" />
        </span>
      )}
      <div className="element-request-preview-body">
        <Question
          questionNumber={`${number}`}
          headingLevel={headingLevel}
          data={data}
          interactive={false}
        />
      </div>
    </div>
  );
}

function TemplatePreview({ payload }) {
  const items = payload.items || [];
  const numbering = getTemplateItemNumbering(items);

  return (
    <div className="element-request-template-items">
      {items.map((item, index) => {
        if (item.itemType === 'TEXT') {
          return (
            <div className="element-request-preview-row" key={`${item.itemType}-${index}`}>
              <TextContentIcon />
              <div className="element-request-preview-body"><TextContent data={item} /></div>
            </div>
          );
        }

        if (item.itemType === 'BLOCK') {
          if (!item.block) {
            return (
              <div className="element-request-reference" key={`${item.itemType}-${index}`}>
                <MdLayers aria-hidden="true" />
                <span>{numbering[index].blockLabel}. Bloque del catálogo #{item.blockId}</span>
              </div>
            );
          }

          return (
            <Block
              key={`${item.itemType}-${index}`}
              data={normalizeBlock(item.block)}
              blockLabel={numbering[index].blockLabel}
              headingLevel={5}
              questionNumberStart={numbering[index].questionNumberStart}
            />
          );
        }

        return (
          <QuestionPreview
            key={`${item.itemType}-${index}`}
            question={item.question || getReferencedQuestion(item.fixedQuestionId)}
            number={numbering[index].questionNumber}
            required={item.required}
          />
        );
      })}
    </div>
  );
}

function ElementStructure({ request }) {
  const payload = request.requestPayload || {};

  if (request.elementType === 'FIXED_QUESTION') {
    return <QuestionPreview question={payload.question} number="" />;
  }
  if (request.elementType === 'BLOCK') {
    return <Block data={normalizeBlock(payload)} headingLevel={5} />;
  }
  return <TemplatePreview payload={payload} />;
}

function ElementRequestCard({ request, isExpanded, onToggle, onAccept, onDeny }) {
  const config = TYPE_CONFIG[request.elementType] || TYPE_CONFIG.FIXED_QUESTION;
  const TypeIcon = config.icon;
  const applicantName = request.requestedByName || 'Usuario';

  return (
    <article className="element-request-card" aria-labelledby={`element-request-${request.id}`}>
      <header className="element-request-card-header">
        <span className="element-request-card-icon"><TypeIcon aria-hidden="true" /></span>
        <div className="element-request-card-heading">
          <span className="element-request-card-type">{config.label}</span>
          <h2 id={`element-request-${request.id}`}>{getRequestTitle(request)}</h2>
        </div>
        <button
          type="button"
          className="element-request-card-toggle"
          onClick={() => onToggle(request.id)}
          aria-expanded={isExpanded}
          aria-controls={`element-request-content-${request.id}`}
        >
          {isExpanded ? 'Ocultar solicitud' : 'Ver solicitud'}
          {isExpanded ? <MdExpandLess aria-hidden="true" /> : <MdExpandMore aria-hidden="true" />}
        </button>
      </header>

      <div className="element-request-card-summary">
        <section className="element-request-message" aria-label="Mensaje del solicitante">
          <h3>Mensaje del solicitante</h3>
          <p>{request.message}</p>
        </section>
      </div>

      <div id={`element-request-content-${request.id}`} className="element-request-card-content" hidden={!isExpanded}>
        {isExpanded && (
          <>
            <h3 className="element-request-applicant">{applicantName}</h3>
            <ul className="element-request-user-data">
              <li><MdOutlineEmail aria-hidden="true" /><span>{request.requestedByEmail || '—'}</span></li>
              <li><MdOutlineBusiness aria-hidden="true" /><span>{request.requestedByInstitution || '—'}</span></li>
              <li><MdPublic aria-hidden="true" /><span>{getCountryName(request.requestedByCountry)}</span></li>
              <li>
                <MdOutlineCalendarToday aria-hidden="true" />
                <time dateTime={request.createdAt}>{formatDateTime(request.createdAt)}</time>
              </li>
            </ul>

            <section className="element-request-structure">
              <div className="element-request-structure-header">
                <h3>{config.structureLabel}</h3>
              </div>

              <div className="element-request-structure-content">
                <ElementStructure request={request} />
              </div>
            </section>

            <div className="element-request-actions">
              <button type="button" className="element-request-deny" onClick={() => onDeny(request)}>
                <MdClose aria-hidden="true" />
                Denegar
              </button>
              <button type="button" className="element-request-accept" onClick={() => onAccept(request)}>
                <MdCheck aria-hidden="true" />
                Aceptar
              </button>
            </div>
          </>
        )}
      </div>
    </article>
  );
}

export function ElementRequestsPage() {
  const [requests, setRequests] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [loadError, setLoadError] = useState('');
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedTypes, setSelectedTypes] = useState(() => TYPE_OPTIONS.map((option) => option.value));
  const [sortOrder, setSortOrder] = useState('NEWEST');
  const [expandedIds, setExpandedIds] = useState(() => new Set());
  const [modalState, setModalState] = useState({ action: null, request: null });
  const [isProcessing, setIsProcessing] = useState(false);
  const [actionError, setActionError] = useState('');
  const actionLockRef = useRef(false);

  useEffect(() => {
    let isMounted = true;

    async function loadRequests() {
      try {
        const data = await getPendingElementRequests();
        if (isMounted) setRequests(Array.isArray(data) ? data : data?.content || []);
      } catch {
        if (isMounted) setLoadError('No se pudieron obtener las solicitudes del catálogo.');
      } finally {
        if (isMounted) setIsLoading(false);
      }
    }

    void loadRequests();
    return () => { isMounted = false; };
  }, []);

  const visibleRequests = useMemo(() => {
    const query = searchQuery.trim().toLocaleLowerCase('es');

    return requests
      .filter((request) => selectedTypes.includes(request.elementType))
      .filter((request) => !query || [
        getRequestTitle(request),
        request.requestedByName,
        request.requestedByEmail,
        request.requestedByInstitution,
        request.message,
      ].some((value) => value?.toLocaleLowerCase('es').includes(query)))
      .sort((left, right) => {
        const difference = new Date(right.createdAt).getTime() - new Date(left.createdAt).getTime();
        return sortOrder === 'NEWEST' ? difference : -difference;
      });
  }, [requests, searchQuery, selectedTypes, sortOrder]);

  function toggleRequest(id) {
    setExpandedIds((current) => {
      const next = new Set(current);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return next;
    });
  }

  function openModal(action, request) {
    if (actionLockRef.current) return;
    setActionError('');
    setModalState({ action, request });
  }

  function closeModal() {
    if (actionLockRef.current) return;
    setActionError('');
    setModalState({ action: null, request: null });
  }

  async function confirmAction() {
    const { action, request } = modalState;
    if (!request || actionLockRef.current) return;

    actionLockRef.current = true;
    setIsProcessing(true);
    setActionError('');

    try {
      if (action === 'ACCEPT') await approveElementRequest(request.id);
      else await rejectElementRequest(request.id);

      setRequests((current) => current.filter((item) => item.id !== request.id));
      setModalState({ action: null, request: null });
    } catch {
      setActionError('No se ha podido resolver la solicitud. Inténtalo de nuevo.');
    } finally {
      actionLockRef.current = false;
      setIsProcessing(false);
    }
  }

  const isAcceptAction = modalState.action === 'ACCEPT';
  const modalRequest = modalState.request;

  return (
    <main className="element-requests-page">
      <div className="element-requests-container">
        <Breadcrumbs
          items={[
            { label: 'PANEL', to: '/panel' },
            { label: 'SOLICITUDES CATÁLOGO' },
          ]}
          separator={MdKeyboardArrowRight}
        />

        <header className="element-requests-page-header">
          <h1>SOLICITUDES PARA EL CATÁLOGO</h1>
          <p>Revisa y gestiona las solicitudes de creación de nuevos elementos sugeridos por los editores.</p>
        </header>

        <SearchToolbar
          ariaLabel="Búsqueda y filtros de solicitudes del catálogo"
          searchLabel="Introduce una palabra para buscar solicitudes del catálogo"
          searchValue={searchQuery}
          onSearchChange={(event) => setSearchQuery(event.target.value)}
          filterOptions={TYPE_OPTIONS}
          filterOptionsControl="multiselect"
          filterOptionsAllLabel="Todos los tipos"
          filterOptionsNoneLabel="Ningún tipo"
          filterOptionsSelectedCountLabel="tipos seleccionados"
          filterOptionsLabel="Tipos de solicitud"
          selectedFilterValues={selectedTypes}
          onFilterChange={setSelectedTypes}
          sortAriaLabel="Ordenar solicitudes del catálogo"
          sortOptions={SORT_OPTIONS}
          sortValue={sortOrder}
          onSortChange={(event) => setSortOrder(event.target.value)}
        />

        <section className="element-requests-list" aria-label="Solicitudes pendientes del catálogo" aria-busy={isLoading}>
          {isLoading ? (
            <Skeleton variant="list" count={3} label="Cargando solicitudes para el catálogo…" />
          ) : loadError ? (
            <p className="element-requests-status error" role="alert">{loadError}</p>
          ) : visibleRequests.length === 0 ? (
            <div className="element-requests-empty" role="status">
              <h2>
                {requests.length === 0
                  ? 'No hay solicitudes pendientes'
                  : 'No hay coincidencias'}
              </h2>
              <p>
                {requests.length === 0
                  ? 'No se han recibido solicitudes para el catálogo.'
                  : selectedTypes.length === 0
                    ? 'Selecciona al menos un tipo de solicitud.'
                  : selectedTypes.length !== TYPE_OPTIONS.length && searchQuery.trim()
                    ? 'No hay solicitudes que coincidan con la búsqueda y los tipos seleccionados.'
                    : selectedTypes.length !== TYPE_OPTIONS.length
                      ? 'No hay solicitudes de los tipos seleccionados.'
                      : 'No hay solicitudes pendientes que coincidan con la búsqueda.'}
              </p>
            </div>
          ) : visibleRequests.map((request) => (
            <ElementRequestCard
              key={request.id}
              request={request}
              isExpanded={expandedIds.has(request.id)}
              onToggle={toggleRequest}
              onAccept={(item) => openModal('ACCEPT', item)}
              onDeny={(item) => openModal('DENY', item)}
            />
          ))}
        </section>
      </div>

      <ConfirmModal
        isOpen={Boolean(modalRequest)}
        onClose={closeModal}
        onConfirm={confirmAction}
        title={isAcceptAction ? 'Aceptar solicitud' : 'Denegar solicitud'}
        message={`¿Confirmas que deseas ${isAcceptAction ? 'aceptar' : 'denegar'} «${modalRequest ? getRequestTitle(modalRequest) : ''}»?`}
        confirmLabel={isAcceptAction ? 'Aceptar' : 'Denegar'}
        variant={isAcceptAction ? 'primary' : 'danger'}
        isProcessing={isProcessing}
        errorMessage={actionError}
      />
    </main>
  );
}
