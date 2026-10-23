import { useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { MdSave } from 'react-icons/md';

import { createCatalogBlock } from '../../../api/catalogApi.js';
import { BlockBuilder } from '../../../components/BlockBuilder/BlockBuilder.jsx';
import { Breadcrumbs } from '../../../components/Breadcrumbs/Breadcrumbs.jsx';
import { CategorySelect } from '../../../components/CategorySelect/CategorySelect.jsx';
import { CatalogSidebar } from '../../../components/CatalogSidebar/CatalogSidebar.jsx';
import { UnsavedElementGuard } from '../../../components/ConfirmModal/UnsavedElementGuard.jsx';
import { ErrorMessage } from '../../../components/ErrorMessage/ErrorMessage.jsx';
import { Skeleton } from '../../../components/Skeleton/Skeleton.jsx';
import { useAuth } from '../../../context/useAuth.js';
import { containsCatalogItem } from '../../../utils/catalogCategoryUtils.js';
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
  const [categoryDetails, setCategoryDetails] = useState(null);
  const [categoryError, setCategoryError] = useState('');
  const [hasUnsavedChanges, setHasUnsavedChanges] = useState(false);
  const [isSaving, setIsSaving] = useState(false);
  const [errorMsg, setErrorMsg] = useState("");
  const [successMsg, setSuccessMsg] = useState("");

  function handleAddQuestionFromSidebar(question, targetIndex) {
    if (!category || question.category !== category) {
      setErrorMsg('Solo puedes añadir preguntas del catálogo de la categoría seleccionada.');
      return false;
    }
    setErrorMsg("");
    setHasUnsavedChanges(true);
    const newQuestion = buildBlockQuestionFromCatalog(question);

    setFormData((current) => {
      const items = [...getBlockItems(current)];
      const insertionIndex = Number.isInteger(targetIndex)
        ? Math.max(0, Math.min(targetIndex, items.length))
        : items.length;
      items.splice(insertionIndex, 0, newQuestion);
      return { ...current, items };
    });
    return true;
  }

  function handleCategoryChange(value, details) {
    if (value !== category && containsCatalogItem(getBlockItems(formData))) {
      setErrorMsg('Quita primero las preguntas añadidas del catálogo para cambiar la categoría del bloque.');
      return;
    }
    setCategory(value);
    setCategoryDetails(details);
    setCategoryError('');
    setErrorMsg('');
    setHasUnsavedChanges(true);
  }

  async function handleSaveBlock() {
    setErrorMsg("");
    setSuccessMsg("");

    const validationError = getBlockBuilderValidationError(formData);

    if (validationError) {
      setErrorMsg(validationError);
      return;
    }

    if (!category) {
      setCategoryError('Selecciona una categoría para el bloque.');
      document.getElementById('block-category')?.focus();
      return;
    }

    if (saveLockRef.current) return;
    saveLockRef.current = true;

    try {
      setIsSaving(true);
      const payload = buildCreateCatalogBlockPayload(formData);
      payload.category = category;
      await createCatalogBlock(payload);
      setHasUnsavedChanges(false);
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
          categoryFilter={category}
          categoryDetails={categoryDetails}
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
            <BlockBuilder
              data={formData}
              onChange={(nextData) => { setFormData(nextData); setHasUnsavedChanges(true); }}
              categoryFilter={category}
              categoryDetails={categoryDetails}
              afterPrimaryField={<CategorySelect id="block-category" value={category} onChange={handleCategoryChange} placeholder="Selecciona la categoría de tu bloque" required validationError={categoryError} />}
              onDropQuestionFromSidebar={handleAddQuestionFromSidebar}
            />
          )}
        </div>
      </main>
      <UnsavedElementGuard hasChanges={hasUnsavedChanges} elementName="bloque" />
    </div>
  );
}
