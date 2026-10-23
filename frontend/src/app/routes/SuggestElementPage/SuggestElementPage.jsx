import { useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  MdOutlineDescription,
  MdOutlineHelpOutline,
  MdOutlineSend,
  MdOutlineWidgets,
} from 'react-icons/md';

import { createElementRequest } from '../../../api/elementRequestsApi.js';
import { BlockBuilder } from '../../../components/BlockBuilder/BlockBuilder.jsx';
import { Breadcrumbs } from '../../../components/Breadcrumbs/Breadcrumbs.jsx';
import { CategorySelect } from '../../../components/CategorySelect/CategorySelect.jsx';
import { CatalogSidebar } from '../../../components/CatalogSidebar/CatalogSidebar.jsx';
import { ErrorMessage } from '../../../components/ErrorMessage/ErrorMessage.jsx';
import { QuestionBuilder } from '../../../components/QuestionBuilder/QuestionBuilder.jsx';
import { Skeleton } from '../../../components/Skeleton/Skeleton.jsx';
import { TemplateBuilder } from '../../../components/TemplateBuilder/TemplateBuilder.jsx';
import { useAuth } from '../../../context/useAuth.js';
import { containsCatalogItem } from '../../../utils/catalogCategoryUtils.js';
import {
  INITIAL_BLOCK_STATE,
  buildBlockQuestionFromCatalog,
  buildCreateCatalogBlockPayload,
  getBlockBuilderValidationError,
  getBlockItems,
} from '../../../utils/blockBuilderUtils.js';
import {
  INITIAL_QUESTION_STATE,
  buildCreateFixedQuestionPayload,
  getQuestionBuilderValidationError,
} from '../../../utils/questionBuilderUtils.js';
import {
  INITIAL_TEMPLATE_STATE,
  buildCreateCatalogTemplatePayload,
  buildTemplateBlockFromCatalog,
  buildTemplateQuestionFromCatalog,
  getTemplateBuilderValidationError,
} from '../../../utils/templateBuilderUtils.js';

import '../CreateBlockPage/CreateBlockPage.css';
import './SuggestElementPage.css';

const ELEMENT_TYPES = {
  FIXED_QUESTION: {
    label: 'Pregunta fija',
    categoryName: 'pregunta fija',
    categoryTarget: 'la pregunta fija',
    description: 'Propón una nueva pregunta para el catálogo.',
    icon: MdOutlineHelpOutline,
    initialState: INITIAL_QUESTION_STATE,
    validate: getQuestionBuilderValidationError,
    toPayload: buildCreateFixedQuestionPayload,
  },
  BLOCK: {
    label: 'Bloque',
    categoryName: 'bloque',
    categoryTarget: 'el bloque',
    description: 'Agrupa preguntas y textos reutilizables.',
    icon: MdOutlineWidgets,
    initialState: INITIAL_BLOCK_STATE,
    validate: getBlockBuilderValidationError,
    toPayload: buildCreateCatalogBlockPayload,
  },
  TEMPLATE: {
    label: 'Plantilla',
    categoryName: 'plantilla',
    categoryTarget: 'la plantilla',
    description: 'Combina preguntas, bloques y textos.',
    icon: MdOutlineDescription,
    initialState: INITIAL_TEMPLATE_STATE,
    validate: getTemplateBuilderValidationError,
    toPayload: buildCreateCatalogTemplatePayload,
  },
};

export function SuggestElementPage() {
  const navigate = useNavigate();
  const { isInitializing } = useAuth();
  const saveLockRef = useRef(false);
  const [elementType, setElementType] = useState('');
  const [formData, setFormData] = useState(null);
  const [category, setCategory] = useState('');
  const [categoryDetails, setCategoryDetails] = useState(null);
  const [categoryError, setCategoryError] = useState('');
  const [message, setMessage] = useState('');
  const [isSaving, setIsSaving] = useState(false);
  const [errorMsg, setErrorMsg] = useState('');
  const [successMsg, setSuccessMsg] = useState('');
  const config = ELEMENT_TYPES[elementType];

  function selectElementType(nextType) {
    setElementType(nextType);
    setFormData(structuredClone(ELEMENT_TYPES[nextType].initialState));
    setCategory('');
    setCategoryDetails(null);
    setCategoryError('');
    setErrorMsg('');
    setSuccessMsg('');
  }

  function handleCategoryChange(value, details) {
    const items = elementType === 'BLOCK' ? getBlockItems(formData) : formData?.items || [];
    if (value !== category && containsCatalogItem(items)) {
      setErrorMsg(`Quita primero los elementos añadidos del catálogo para cambiar la categoría de ${config.categoryTarget}.`);
      return;
    }
    setCategory(value);
    setCategoryDetails(details);
    setCategoryError('');
    setErrorMsg('');
  }

  function addItem(item, targetIndex) {
    setErrorMsg('');
    setFormData((current) => {
      const currentItems = elementType === 'BLOCK' ? getBlockItems(current) : current.items;
      const items = [...currentItems];
      const insertionIndex = Number.isInteger(targetIndex)
        ? Math.max(0, Math.min(targetIndex, items.length))
        : items.length;
      items.splice(insertionIndex, 0, item);
      return { ...current, items };
    });
  }

  function addQuestion(question, targetIndex) {
    if (!category || question.category !== category) {
      setErrorMsg('Solo puedes añadir preguntas del catálogo de la categoría seleccionada.');
      return false;
    }
    const item = elementType === 'BLOCK'
      ? buildBlockQuestionFromCatalog(question)
      : buildTemplateQuestionFromCatalog(question);
    addItem(item, targetIndex);
    return true;
  }

  function addBlock(block, targetIndex) {
    if (!category || block.category !== category) {
      setErrorMsg('Solo puedes añadir bloques del catálogo de la categoría seleccionada.');
      return false;
    }
    addItem(buildTemplateBlockFromCatalog(block), targetIndex);
    return true;
  }

  async function handleSubmit() {
    setErrorMsg('');
    setSuccessMsg('');

    if (!config || !formData) {
      setErrorMsg('Selecciona el tipo de elemento que quieres sugerir.');
      return;
    }
    if (!message.trim()) {
      setErrorMsg('Explica brevemente el motivo de la propuesta.');
      return;
    }

    const validationError = config.validate(formData);
    if (validationError) {
      setErrorMsg(validationError);
      return;
    }
    if (!category) {
      setCategoryError(`Selecciona una categoría para ${config.categoryTarget}.`);
      document.getElementById('suggest-element-category')?.focus();
      return;
    }
    if (saveLockRef.current) return;
    saveLockRef.current = true;

    try {
      setIsSaving(true);
      await createElementRequest({
        elementType,
        message: message.trim(),
        requestPayload: { ...config.toPayload(formData), category },
      });
      setSuccessMsg('Solicitud enviada con éxito. Redirigiendo...');
      window.setTimeout(() => navigate('/panel'), 1200);
    } catch (error) {
      setErrorMsg(error.message || 'No se ha podido enviar la solicitud.');
    } finally {
      saveLockRef.current = false;
      setIsSaving(false);
    }
  }

  function renderBuilder() {
    if (elementType === 'FIXED_QUESTION') {
      return <QuestionBuilder data={formData} onChange={setFormData} isCatalog afterPrimaryField={renderCategorySelect()} />;
    }
    if (elementType === 'BLOCK') {
      return (
        <BlockBuilder
          data={formData}
          onChange={setFormData}
          categoryFilter={category}
          categoryDetails={categoryDetails}
          afterPrimaryField={renderCategorySelect()}
          onDropQuestionFromSidebar={addQuestion}
        />
      );
    }
    return (
      <TemplateBuilder
        data={formData}
        onChange={setFormData}
        categoryFilter={category}
        categoryDetails={categoryDetails}
        afterPrimaryField={renderCategorySelect()}
        onDropQuestionFromSidebar={addQuestion}
        onDropBlockFromSidebar={addBlock}
      />
    );
  }

  function renderCategorySelect() {
    return <CategorySelect id="suggest-element-category" value={category} onChange={handleCategoryChange} placeholder={`Selecciona la categoría de tu ${config.categoryName}`} required validationError={categoryError} />;
  }

  return (
    <div className="create-block-layout">
      {!isInitializing && (elementType === 'BLOCK' || elementType === 'TEMPLATE') && (
        <CatalogSidebar
          showQuestions
          showBlocks={elementType === 'TEMPLATE'}
          categoryFilter={category}
          categoryDetails={categoryDetails}
          defaultOpen={false}
          onAddQuestion={addQuestion}
          onAddBlock={elementType === 'TEMPLATE' ? addBlock : undefined}
        />
      )}

      <main className="create-block-main">
        <div className="create-block-container">
          <Breadcrumbs items={[
            { label: 'PANEL', to: '/panel' },
            { label: 'SUGERIR ELEMENTO' },
          ]} />

          <header className="page-main-header">
            <div>
              <h1>SUGIERE UN ELEMENTO</h1>
              <p className="page-main-subtitle">
                Crea una propuesta para que se valore su incorporación al catálogo, para elementos como bloques y plantilla puedes reutilizar elementos del catálogo
              </p>
            </div>
            <button
              type="button"
              className="btn-create-submit"
              disabled={isSaving || isInitializing || !config}
              onClick={handleSubmit}
            >
              <MdOutlineSend aria-hidden="true" />
              <span>{isSaving ? 'Enviando...' : 'Enviar solicitud'}</span>
            </button>
          </header>

          {errorMsg && (
            <div className="suggest-element-message">
              <ErrorMessage message={errorMsg} onClose={() => setErrorMsg('')} />
            </div>
          )}
          {successMsg && <div className="account-success-banner" role="status"><span>{successMsg}</span></div>}

          <section className="suggest-element-card" aria-labelledby="suggest-element-type-title">
            <h2 id="suggest-element-type-title">TIPO DE ELEMENTO</h2>
            <div className="suggest-element-types">
              {Object.entries(ELEMENT_TYPES).map(([type, option]) => {
                const Icon = option.icon;
                return (
                  <button
                    key={type}
                    type="button"
                    className={`suggest-element-type ${elementType === type ? 'selected' : ''}`.trim()}
                    aria-pressed={elementType === type}
                    onClick={() => selectElementType(type)}
                  >
                    <Icon aria-hidden="true" />
                    <span><strong>{option.label}</strong><small>{option.description}</small></span>
                  </button>
                );
              })}
            </div>
          </section>

          {config && (
            <>
              <section className="suggest-element-card" aria-labelledby="suggest-element-message-title">
                <h2 id="suggest-element-message-title">MOTIVO DE LA PROPUESTA</h2>
                <label htmlFor="suggest-element-message">
                  Mensaje para el equipo administrador <span aria-hidden="true">*</span>
                </label>
                <textarea
                  id="suggest-element-message"
                  value={message}
                  placeholder="Explica por qué este elemento debería añadirse al catálogo."
                  onChange={(event) => setMessage(event.target.value)}
                />
              </section>

              {isInitializing
                ? (
                  <div className="builder-skeleton-wrapper">
                    <Skeleton variant="list" count={3} hasActions={false} />
                  </div>
                )
                : renderBuilder()}
            </>
          )}
        </div>
      </main>
    </div>
  );
}
