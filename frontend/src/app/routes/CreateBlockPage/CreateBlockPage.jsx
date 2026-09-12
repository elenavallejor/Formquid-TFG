import { useRef, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { MdChevronRight, MdClose, MdSave } from 'react-icons/md';

import { createCatalogBlock } from '../../../api/catalogApi.js';
import { BlockBuilder } from '../../../components/BlockBuilder/BlockBuilder.jsx';
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
      await createCatalogBlock(buildCreateCatalogBlockPayload(formData));
      setSuccessMsg("Bloque creado con éxito. Redirigiendo...");

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
          <nav className="breadcrumb-nav" aria-label="Migas de pan">
            <Link to="/panel">PANEL</Link>
            <MdChevronRight aria-hidden="true" />
            <span aria-current="page">CREAR BLOQUE</span>
          </nav>

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
