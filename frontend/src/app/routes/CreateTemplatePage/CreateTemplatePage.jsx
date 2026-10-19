import { useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { MdSave } from 'react-icons/md';

import { createCatalogTemplate } from '../../../api/catalogApi.js';
import { CatalogSidebar } from '../../../components/CatalogSidebar/CatalogSidebar.jsx';
import { Breadcrumbs } from '../../../components/Breadcrumbs/Breadcrumbs.jsx';
import { CategorySelect } from '../../../components/CategorySelect/CategorySelect.jsx';
import { ErrorMessage } from '../../../components/ErrorMessage/ErrorMessage.jsx';
import { Skeleton } from '../../../components/Skeleton/Skeleton.jsx';
import { TemplateBuilder } from '../../../components/TemplateBuilder/TemplateBuilder.jsx';
import { useAuth } from '../../../context/useAuth.js';
import {
  INITIAL_TEMPLATE_STATE,
  buildCreateCatalogTemplatePayload,
  buildTemplateBlockFromCatalog,
  buildTemplateQuestionFromCatalog,
  getTemplateBuilderValidationError,
} from '../../../utils/templateBuilderUtils.js';

import '../CreateBlockPage/CreateBlockPage.css';
import './CreateTemplatePage.css';

export function CreateTemplatePage() {
  const navigate = useNavigate();
  const { isInitializing } = useAuth();
  const saveLockRef = useRef(false);
  const [formData, setFormData] = useState(INITIAL_TEMPLATE_STATE);
  const [category, setCategory] = useState('');
  const [isSaving, setIsSaving] = useState(false);
  const [errorMsg, setErrorMsg] = useState('');
  const [successMsg, setSuccessMsg] = useState('');

  function addItem(item, targetIndex) {
    setErrorMsg('');
    setFormData((current) => {
      const items = [...current.items];
      const insertionIndex = Number.isInteger(targetIndex) ? Math.max(0, Math.min(targetIndex, items.length)) : items.length;
      items.splice(insertionIndex, 0, item);
      return { ...current, items };
    });
  }

  async function handleSaveTemplate() {
    setErrorMsg('');
    setSuccessMsg('');
    const validationError = getTemplateBuilderValidationError(formData);
    if (validationError) {
      setErrorMsg(validationError);
      return;
    }
    if (saveLockRef.current) return;
    saveLockRef.current = true;

    try {
      setIsSaving(true);
      const payload = buildCreateCatalogTemplatePayload(formData);
      if (category) payload.category = category;
      await createCatalogTemplate(payload);
      setSuccessMsg('Plantilla creada con éxito. Redirigiendo...');
      window.setTimeout(() => navigate('/catalogo/plantillas'), 1200);
    } catch (error) {
      setErrorMsg(error.message || 'No se ha podido guardar la plantilla.');
    } finally {
      saveLockRef.current = false;
      setIsSaving(false);
    }
  }

  const addQuestion = (question, index) => addItem(buildTemplateQuestionFromCatalog(question), index);

  const addBlock = (block, index) => addItem(buildTemplateBlockFromCatalog(block), index);

  return (
    <div className="create-block-layout">
      {!isInitializing && <CatalogSidebar showQuestions showBlocks defaultOpen={false} onAddQuestion={addQuestion} onAddBlock={addBlock} />}
      <main className="create-block-main">
        <div className="create-block-container">
          <Breadcrumbs items={[
            { label: 'PANEL', to: '/panel' },
            { label: 'CATÁLOGO', to: '/catalogo/plantillas' },
            { label: 'CREAR PLANTILLA' },
          ]} />
          <header className="page-main-header">
            <div><h1>CREA TU PLANTILLA</h1><p className="page-main-subtitle">Crea y organiza los bloques y preguntas de tu nueva plantilla.</p></div>
            <button type="button" className="btn-create-submit" disabled={isSaving || isInitializing} onClick={handleSaveTemplate}><MdSave aria-hidden="true" /><span>{isSaving ? 'Guardando...' : 'Guardar plantilla'}</span></button>
          </header>
          <CategorySelect id="template-category" value={category} onChange={setCategory} />
          {errorMsg && <div className="create-template-message"><ErrorMessage message={errorMsg} onClose={() => setErrorMsg('')} /></div>}
          {successMsg && <div className="account-success-banner" role="status"><span>{successMsg}</span></div>}
          {isInitializing ? <div className="builder-skeleton-wrapper"><Skeleton variant="list" count={3} hasActions={false} /></div> : <TemplateBuilder data={formData} onChange={setFormData} onDropQuestionFromSidebar={addQuestion} onDropBlockFromSidebar={addBlock} />}
        </div>
      </main>
    </div>
  );
}
