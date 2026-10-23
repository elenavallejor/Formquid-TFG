import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { MdSave } from 'react-icons/md';

import { createCatalogQuestion } from '../../../api/catalogApi.js';
import { Breadcrumbs } from '../../../components/Breadcrumbs/Breadcrumbs.jsx';
import { CategorySelect } from '../../../components/CategorySelect/CategorySelect.jsx';
import { UnsavedElementGuard } from '../../../components/ConfirmModal/UnsavedElementGuard.jsx';
import { ErrorMessage } from '../../../components/ErrorMessage/ErrorMessage.jsx';
import { Skeleton } from '../../../components/Skeleton/Skeleton.jsx';
import { QuestionBuilder } from '../../../components/QuestionBuilder/QuestionBuilder.jsx';
import { buildCreateFixedQuestionPayload, getQuestionBuilderValidationError, INITIAL_QUESTION_STATE } from '../../../utils/questionBuilderUtils.js';
import { useAuth } from '../../../context/useAuth.js';

import '../CreateBlockPage/CreateBlockPage.css';
import './CreateQuestionPage.css';

/**
 * Vista de administración para crear preguntas fijas dentro del catálogo oficial.
 */
export function CreateQuestionPage() {
  const navigate = useNavigate();
  const { isInitializing } = useAuth();

  const [formData, setFormData] = useState(INITIAL_QUESTION_STATE);
  const [category, setCategory] = useState('');
  const [categoryError, setCategoryError] = useState('');
  const [hasUnsavedChanges, setHasUnsavedChanges] = useState(false);
  const [isSaving, setIsSaving] = useState(false);
  const [errorMsg, setErrorMsg] = useState('');
  const [successMsg, setSuccessMsg] = useState('');

  async function handleSaveQuestion() {
    setErrorMsg('');
    setSuccessMsg('');

    const validationError = getQuestionBuilderValidationError(formData);

    if (validationError) {
      setErrorMsg(validationError);
      return;
    }

    if (!category) {
      setCategoryError('Selecciona una categoría para la pregunta fija.');
      document.getElementById('question-category')?.focus();
      return;
    }

    try {
      setIsSaving(true);
      const payload = buildCreateFixedQuestionPayload(formData);
      payload.category = category;
      await createCatalogQuestion(payload);
      setHasUnsavedChanges(false);
      setSuccessMsg('Pregunta fija creada con éxito. Redirigiendo...');

      window.setTimeout(() => {
        navigate('/catalogo/preguntas');
      }, 1200);
    } catch (error) {
      setErrorMsg(error.message || 'No se ha podido guardar la pregunta fija.');
    } finally {
      setIsSaving(false);
    }
  }

  return (
    <main className="create-question-page">
      <div className="create-question-container">
        <Breadcrumbs items={[
          { label: 'PANEL', to: '/panel' },
          { label: 'CATÁLOGO', to: '/catalogo/preguntas' },
          { label: 'CREAR PREGUNTA FIJA' },
        ]} />

        <header className="page-main-header">
          <div>
            <h1>CREA TU PREGUNTA FIJA</h1>
            <p className="page-main-subtitle">Crea y configura preguntas para el catálogo.</p>
          </div>

          <button
            type="button"
            className="btn-create-submit"
            disabled={isSaving || isInitializing}
            onClick={handleSaveQuestion}
          >
            <MdSave aria-hidden="true" />
            <span>{isSaving ? 'Guardando...' : 'Crear pregunta'}</span>
          </button>
        </header>
        {errorMsg && (
          <div className="create-element-message"><ErrorMessage message={errorMsg} onClose={() => setErrorMsg('')} /></div>
        )}

        {successMsg && (
          <div className="account-success-banner" role="status">
            <span>{successMsg}</span>
          </div>
        )}

        {isInitializing ? (
          <div className="builder-skeleton-wrapper">
            <Skeleton variant="list" count={3} hasActions={false} />
          </div>
        ) : (
          <QuestionBuilder data={formData} onChange={(nextData) => { setFormData(nextData); setHasUnsavedChanges(true); }} isCatalog
            afterPrimaryField={<CategorySelect id="question-category" value={category} onChange={(value) => { setCategory(value); setCategoryError(''); setHasUnsavedChanges(true); }} placeholder="Selecciona la categoría de tu pregunta fija" required validationError={categoryError} />} />
        )}
      </div>
      <UnsavedElementGuard hasChanges={hasUnsavedChanges} elementName="pregunta fija" />
    </main>
  );
}
