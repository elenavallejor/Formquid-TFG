import { useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { MdClose, MdSave } from 'react-icons/md';

import { createCatalogBlock } from '../../../api/catalogApi.js';
import { BlockBuilder } from '../../../components/BlockBuilder/BlockBuilder.jsx';
import { Breadcrumbs } from '../../../components/Breadcrumbs/Breadcrumbs.jsx';
import { CategorySelect } from '../../../components/CategorySelect.jsx';
import { CatalogSidebar } from '../../../components/CatalogSidebar/CatalogSidebar.jsx';
import { Skeleton } from '../../../components/Skeleton/Skeleton.jsx';
import { useAuth } from '../../../context/AuthContext.jsx';
import { INITIAL_BLOCK_STATE, buildBlockQuestionFromCatalog, buildCreateCatalogBlockPayload, getBlockItems, getBlockBuilderValidationError } from "../../../utils/blockBuilderUtils.js";
import './CreateBlockPage.css';

/**
 * Vista de administración para construir y crear bloques de preguntas del catálogo.
 */
export function CreateBlockPage() {
  const navigate = useNavigate();
  const { isInitializing } = useAuth();
  const saveLockRef = useRef(false);

  const [formData, setFormData] = useState(INITIAL_BLOCK_STATE);
  const [category, setCategory] = useState('');
  const [isSaving, setIsSaving] = useState(false);
  const [errorMsg, setErrorMsg] = useState("");
  const [successMsg, setSuccessMsg] = useState("");

  function handleAddQuestionFromSidebar(question, targetIndex) {
    setErrorMsg("");
    const newQuestion = buildBlockQuestionFromCatalog(question);

    setFormData((current) => {
      const items = [...getBlockItems(current)];
      const insertionIndex = Number.isInteger(targetIndex)
        ? Math.max(0, Math.min(targetIndex, items.length))
        : items.length;
      items.splice(insertionIndex, 0, newQuestion);
      return { ...current, items };
    });
  }

  async function handleSaveBlock() {
    setErrorMsg("");
    setSuccessMsg("");

    const validationError = getBlockBuilderValidationError(formData);

    if (validationError) {
      setErrorMsg(validationError);
      return;
    }

    if (saveLockRef.current) return;
    saveLockRef.current = true;

    try {
      setIsSaving(true);
      const payload = buildCreateCatalogBlockPayload(formData);
      if (category) payload.category = category;
      await createCatalogBlock(payload);
      setSuccessMsg('Bloque creado con éxito. Redirigiendo...');

      window.setTimeout(() => {
        navigate("/catalogo/bloques");
      }, 1200);
    } catch (error) {
      setErrorMsg(error.message || "No se ha podido guardar el bloque.");
    } finally {
      saveLockRef.current = false;
      setIsSaving(false);
    }
  }

  return (
    <div className="create-block-layout">
      {!isInitializing && (
        <CatalogSidebar
          showQuestions
          showBlocks={false}
          defaultOpen={false}
          onAddQuestion={handleAddQuestionFromSidebar}
        />
      )}

      <main className="create-block-main">
        <div className="create-block-container">
          <Breadcrumbs items={[
            { label: 'PANEL', to: '/panel' },
            { label: 'CATÁLOGO', to: '/catalogo/bloques' },
            { label: 'CREAR BLOQUE' },
          ]} />

          <header className="page-main-header">
            <div>
              <h1>CREA TU BLOQUE</h1>
              <p className="page-main-subtitle">
                Combina preguntas propias, preguntas fijas y textos informativos
                en un único bloque.
              </p>
            </div>

            <button
              type="button"
              className="btn-create-submit"
              disabled={isSaving || isInitializing}
              onClick={handleSaveBlock}
            >
              <MdSave aria-hidden="true" />
              <span>{isSaving ? "Guardando..." : "Crear bloque"}</span>
            </button>
          </header>
          <div className="builder-field">
            <CategorySelect id="block-category" value={category} onChange={setCategory} />
          </div>

          {errorMsg && (
            <div className="account-error-banner" role="alert">
              <span>{errorMsg}</span>

              <button
                type="button"
                className="error-banner-close-btn"
                aria-label="Cerrar mensaje de error"
                onClick={() => setErrorMsg("")}
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
            <BlockBuilder
              data={formData}
              onChange={setFormData}
              onDropQuestionFromSidebar={handleAddQuestionFromSidebar}
            />
          )}
        </div>
      </main>
    </div>
  );
}
