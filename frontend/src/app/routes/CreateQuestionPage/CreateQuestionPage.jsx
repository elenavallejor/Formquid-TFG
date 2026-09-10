import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { MdChevronRight, MdClose, MdSave } from 'react-icons/md';

import { createCatalogQuestion } from '../../../api/catalogApi.js';
import { Skeleton } from '../../../components/Skeleton/Skeleton.jsx';
import { QuestionBuilder } from '../../../components/QuestionBuilder/QuestionBuilder.jsx';
import { buildCreateFixedQuestionPayload, getQuestionBuilderValidationError, INITIAL_QUESTION_STATE } from '../../../utils/questionBuilderUtils.js';
import { useAuth } from '../../../context/AuthContext.jsx';

import './CreateQuestionPage.css';

/**
 * Vista de administración para crear preguntas fijas dentro del catálogo oficial.
 */
export function CreateQuestionPage() {
  const navigate = useNavigate();
  const { isInitializing } = useAuth();

  const [formData, setFormData] = useState(INITIAL_QUESTION_STATE);
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

    try {
      setIsSaving(true);
      await createCatalogQuestion(buildCreateFixedQuestionPayload(formData));
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
        <nav className="breadcrumb-nav" aria-label="Migas de pan">
          <Link to="/panel">PANEL</Link>
          <MdChevronRight aria-hidden="true" />
          <span aria-current="page">CREAR PREGUNTA FIJA</span>
        </nav>

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
          <div className="account-error-banner" role="alert">
            <span>{errorMsg}</span>
            <button
              type="button"
              className="error-banner-close-btn"
              aria-label="Cerrar mensaje de error"
              onClick={() => setErrorMsg('')}
            >
              <MdClose aria-hidden="true" />
            </button>
          </div>
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
          <QuestionBuilder data={formData} onChange={setFormData} isCatalog />
        )}
      </div>
    </main>
  );
}