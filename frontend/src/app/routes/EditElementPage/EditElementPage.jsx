import { useEffect, useRef, useState } from 'react';
import { Link, Navigate, useBlocker, useNavigate, useParams } from 'react-router-dom';
import { MdChevronRight, MdSave } from 'react-icons/md';

import {
  getCatalogBlockById,
  getCatalogQuestionById,
  getCatalogTemplateById,
  updateCatalogBlock,
  updateCatalogQuestion,
  updateCatalogTemplate,
} from '../../../api/catalogApi.js';
import { BlockBuilder } from '../../../components/BlockBuilder/BlockBuilder.jsx';
import { CatalogSidebar } from '../../../components/CatalogSidebar/CatalogSidebar.jsx';
import { ConfirmModal } from '../../../components/ConfirmModal/ConfirmModal.jsx';
import { ErrorMessage } from '../../../components/ErrorMessage/ErrorMessage.jsx';
import { QuestionBuilder } from '../../../components/QuestionBuilder/QuestionBuilder.jsx';
import { Skeleton } from '../../../components/Skeleton/Skeleton.jsx';
import { TemplateBuilder } from '../../../components/TemplateBuilder/TemplateBuilder.jsx';
import { useAuth } from '../../../context/AuthContext.jsx';
import {
  buildBlockQuestionFromCatalog,
  buildCreateCatalogBlockPayload,
  getBlockBuilderValidationError,
  getBlockItems,
} from '../../../utils/blockBuilderUtils.js';
import {
  buildCreateFixedQuestionPayload,
  getQuestionBuilderValidationError,
} from '../../../utils/questionBuilderUtils.js';
import {
  buildCreateCatalogTemplatePayload,
  buildEditableTemplateBlock,
  buildTemplateBlockFromCatalog,
  buildTemplateQuestionFromCatalog,
  buildTemplateStateFromResponse,
  getTemplateBuilderValidationError,
} from '../../../utils/templateBuilderUtils.js';

import '../CreateBlockPage/CreateBlockPage.css';
import '../CreateQuestionPage/CreateQuestionPage.css';
import './EditElementPage.css';

const ELEMENT_CONFIG = {
  pregunta: {
    label: 'PREGUNTA FIJA',
    title: 'EDITA TU PREGUNTA FIJA',
    subtitle: 'Modifica la configuración de la pregunta.',
    catalogPath: '/catalogo/preguntas',
    load: getCatalogQuestionById,
    update: updateCatalogQuestion,
    toState: buildBlockQuestionFromCatalog,
    toPayload: buildCreateFixedQuestionPayload,
    validate: getQuestionBuilderValidationError,
    loadError: 'No se ha podido cargar la pregunta fija.',
    saveError: 'No se ha podido actualizar la pregunta fija.',
    successMessage: 'Pregunta fija actualizada con éxito. Redirigiendo...',
  },
  bloque: {
    label: 'BLOQUE',
    title: 'EDITA TU BLOQUE',
    subtitle: 'Modifica las preguntas y textos del bloque.',
    catalogPath: '/catalogo/bloques',
    load: getCatalogBlockById,
    update: updateCatalogBlock,
    toState: buildEditableTemplateBlock,
    toPayload: buildCreateCatalogBlockPayload,
    validate: getBlockBuilderValidationError,
    loadError: 'No se ha podido cargar el bloque.',
    saveError: 'No se ha podido actualizar el bloque.',
    successMessage: 'Bloque actualizado con éxito. Redirigiendo...',
  },
  plantilla: {
    label: 'PLANTILLA',
    title: 'EDITA TU PLANTILLA',
    subtitle: 'Modifica los bloques y preguntas de la plantilla.',
    catalogPath: '/catalogo/plantillas',
    load: getCatalogTemplateById,
    update: updateCatalogTemplate,
    toState: buildTemplateStateFromResponse,
    toPayload: buildCreateCatalogTemplatePayload,
    validate: getTemplateBuilderValidationError,
    loadError: 'No se ha podido cargar la plantilla.',
    saveError: 'No se ha podido actualizar la plantilla.',
    successMessage: 'Plantilla actualizada con éxito. Redirigiendo...',
  },
};

export function EditElementPage() {
  const navigate = useNavigate();
  const { elementType, elementId } = useParams();
  const { isInitializing } = useAuth();
  const config = ELEMENT_CONFIG[elementType];
  const saveLockRef = useRef(false);
  const [formData, setFormData] = useState(null);
  const [isLoading, setIsLoading] = useState(true);
  const [isSaving, setIsSaving] = useState(false);
  const [hasUnsavedChanges, setHasUnsavedChanges] = useState(false);
  const [errorMsg, setErrorMsg] = useState('');
  const [successMsg, setSuccessMsg] = useState('');
  const navigationBlocker = useBlocker(hasUnsavedChanges);

  useEffect(() => {
    if (!config) return undefined;

    let isMounted = true;
    setIsLoading(true);
    setFormData(null);
    setHasUnsavedChanges(false);
    setErrorMsg('');
    setSuccessMsg('');

    config.load(elementId)
      .then((element) => {
        if (!isMounted) return;
        setFormData(config.toState(element));
      })
      .catch((error) => {
        if (isMounted) setErrorMsg(error.message || config.loadError);
      })
      .finally(() => {
        if (isMounted) setIsLoading(false);
      });

    return () => { isMounted = false; };
  }, [config, elementId]);

  useEffect(() => {
    function handleBeforeUnload(event) {
      if (!hasUnsavedChanges) return;
      event.preventDefault();
      event.returnValue = '';
    }

    window.addEventListener('beforeunload', handleBeforeUnload);
    return () => window.removeEventListener('beforeunload', handleBeforeUnload);
  }, [hasUnsavedChanges]);

  if (!config) return <Navigate to="/catalogo" replace />;

  function addItem(item, targetIndex) {
    setErrorMsg('');
    setHasUnsavedChanges(true);
    setFormData((current) => {
      const currentItems = elementType === 'bloque' ? getBlockItems(current) : current.items;
      const items = [...currentItems];
      const insertionIndex = Number.isInteger(targetIndex)
        ? Math.max(0, Math.min(targetIndex, items.length))
        : items.length;
      items.splice(insertionIndex, 0, item);
      return { ...current, items };
    });
  }

  function addQuestion(question, targetIndex) {
    const item = elementType === 'bloque'
      ? buildBlockQuestionFromCatalog(question)
      : buildTemplateQuestionFromCatalog(question);
    addItem(item, targetIndex);
  }

  function addBlock(block, targetIndex) {
    addItem(buildTemplateBlockFromCatalog(block), targetIndex);
  }

  function handleFormChange(nextFormData) {
    setHasUnsavedChanges(true);
    setFormData(nextFormData);
  }

  async function handleSave() {
    setErrorMsg('');
    setSuccessMsg('');

    const validationError = config.validate(formData);
    if (validationError) {
      setErrorMsg(validationError);
      return;
    }

    if (saveLockRef.current) return;
    saveLockRef.current = true;

    try {
      setIsSaving(true);
      await config.update(elementId, config.toPayload(formData));
      setHasUnsavedChanges(false);
      setSuccessMsg(config.successMessage);
      window.setTimeout(() => navigate(config.catalogPath), 1200);
    } catch (error) {
      setErrorMsg(error.message || config.saveError);
    } finally {
      saveLockRef.current = false;
      setIsSaving(false);
    }
  }

  function renderBuilder() {
    if (elementType === 'pregunta') {
      return <QuestionBuilder data={formData} onChange={handleFormChange} isCatalog />;
    }

    if (elementType === 'bloque') {
      return (
        <BlockBuilder
          data={formData}
          onChange={handleFormChange}
          onDropQuestionFromSidebar={addQuestion}
        />
      );
    }

    return (
      <TemplateBuilder
        data={formData}
        onChange={handleFormChange}
        onDropQuestionFromSidebar={addQuestion}
        onDropBlockFromSidebar={addBlock}
      />
    );
  }

  const content = (
    <>
      <nav className="breadcrumb-nav" aria-label="Migas de pan">
        <Link to="/panel">PANEL</Link>
        <MdChevronRight aria-hidden="true" />
        <span aria-current="page">EDITAR {config.label}</span>
      </nav>

      <header className="page-main-header">
        <div>
          <h1>{config.title}</h1>
          <p className="page-main-subtitle">{config.subtitle}</p>
        </div>
        <button
          type="button"
          className="btn-create-submit"
          disabled={isSaving || isInitializing || isLoading || !formData}
          onClick={handleSave}
        >
          <MdSave aria-hidden="true" />
          <span>{isSaving ? 'Guardando...' : 'Guardar cambios'}</span>
        </button>
      </header>

      {errorMsg && (
        <div className="edit-element-message">
          <ErrorMessage message={errorMsg} onClose={() => setErrorMsg('')} />
        </div>
      )}
      {successMsg && <div className="account-success-banner" role="status"><span>{successMsg}</span></div>}
      {isInitializing || isLoading || !formData
        ? <div className="builder-skeleton-wrapper"><Skeleton variant="list" count={3} hasActions={false} /></div>
        : renderBuilder()}

      <ConfirmModal
        isOpen={navigationBlocker.state === 'blocked'}
        onClose={() => navigationBlocker.reset()}
        onConfirm={() => navigationBlocker.proceed()}
        title="Salir sin guardar"
        message="Los cambios realizados no se guardarán. ¿Confirmas que deseas salir de esta página?"
        confirmLabel="Salir sin guardar"
        variant="primary"
      />
    </>
  );

  if (elementType === 'pregunta') {
    return <main className="create-question-page"><div className="create-question-container">{content}</div></main>;
  }

  return (
    <div className="create-block-layout">
      {!isInitializing && !isLoading && formData && (
        <CatalogSidebar
          showQuestions
          showBlocks={elementType === 'plantilla'}
          defaultOpen={false}
          onAddQuestion={addQuestion}
          onAddBlock={elementType === 'plantilla' ? addBlock : undefined}
        />
      )}
      <main className="create-block-main">
        <div className="create-block-container">{content}</div>
      </main>
    </div>
  );
}
