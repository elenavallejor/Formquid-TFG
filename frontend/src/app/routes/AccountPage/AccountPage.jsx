import { useEffect, useRef, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { MdChevronRight, MdSave, MdOutlineVisibility, MdOutlineVisibilityOff, MdOutlineWarningAmber, MdOutlineDelete } from 'react-icons/md';
import { deleteMyAccount, updateMyAccount } from '../../../api/usersApi.js';
import { Dropdown } from '../../../components/Dropdown/Dropdown.jsx';
import { ErrorMessage } from '../../../components/ErrorMessage/ErrorMessage.jsx';
import { PasswordConfirmModal } from '../../../components/PasswordConfirmModal/PasswordConfirmModal.jsx';
import { Skeleton } from '../../../components/Skeleton/Skeleton.jsx';
import { useAuth } from '../../../context/AuthContext.jsx';
import { loadUserData } from '../../../utils/loadUserData.js';
import { GENDER_OPTIONS, hasAccountFormErrors, hasFormChanges, validateEmailChange, validatePasswordChange, validatePersonalInfo } from '../../../utils/accountFormUtils.js';
import './AccountPage.css';

/**
 * Gestiona la información personal, seguridad, correo y baja de la cuenta del usuario.
 */
export function AccountPage() {
  const navigate = useNavigate();
  const redirectTimeoutRef = useRef(null);
  const { setAuthenticatedUser, clearAuthentication } = useAuth();

  const [isLoading, setIsLoading] = useState(true);
  const [isSaving, setIsSaving] = useState(false);
  const [initialPersonalData, setInitialPersonalData] = useState({ nombre: '', institucion: '', genero: 'MALE' });
  const [personalData, setPersonalData] = useState({ nombre: '', institucion: '', genero: 'MALE' });
  const [passwordData, setPasswordData] = useState({ newPassword: '', confirmPassword: '' });
  const [emailData, setEmailData] = useState({ currentEmail: '', newEmail: '', confirmNewEmail: '' });
  const [showPassword, setShowPassword] = useState({ new: false, confirm: false });
  const [globalError, setGlobalError] = useState('');
  const [globalSuccess, setGlobalSuccess] = useState('');
  const [hasBlockingSurveys, setHasBlockingSurveys] = useState(false);
  const [pendingAction, setPendingAction] = useState(null);
  const [isPwdModalOpen, setIsPwdModalOpen] = useState(false);
  const [pwdModalError, setPwdModalError] = useState('');

  useEffect(() => {
    let isMounted = true;

    async function fetchAccountData() {
      try {
        setIsLoading(true);
        setGlobalError('');

        const { personalData: loadedPersonal, email, hasActiveSurveys, hasOwnedSurveys } = await loadUserData();

        if (!isMounted) return;

        setInitialPersonalData(loadedPersonal);
        setPersonalData(loadedPersonal);
        setEmailData((prev) => ({ ...prev, currentEmail: email }));
        setHasBlockingSurveys(Boolean(hasOwnedSurveys ?? hasActiveSurveys));
      } catch (error) {
        if (!isMounted) return;
        setGlobalError(error.message || 'No se han podido cargar los datos de la cuenta.');
      } finally {
        if (isMounted) setIsLoading(false);
      }
    }

    void fetchAccountData();

    return () => {
      isMounted = false;
      if (redirectTimeoutRef.current) clearTimeout(redirectTimeoutRef.current);
    };
  }, []);

  const togglePasswordVisibility = (field) => {
    setShowPassword((prev) => ({ ...prev, [field]: !prev[field] }));
  };

  const handlePersonalChange = (event) => {
    const { name, value } = event.target;
    setPersonalData((prev) => ({ ...prev, [name]: value }));
  };

  const handlePasswordChange = (event) => {
    const { name, value } = event.target;
    setPasswordData((prev) => ({ ...prev, [name]: value }));
  };

  const handleEmailChange = (event) => {
    const { name, value } = event.target;
    setEmailData((prev) => ({ ...prev, [name]: value }));
  };

  const handleInitSave = (event) => {
    event.preventDefault();
    setGlobalError('');
    setGlobalSuccess('');

    if (!hasFormChanges(personalData, initialPersonalData, passwordData, emailData)) {
      setGlobalError('No se ha detectado ningún cambio para guardar.');
      return;
    }

    const personalErrors = validatePersonalInfo(personalData);
    if (hasAccountFormErrors(personalErrors)) {
      setGlobalError(Object.values(personalErrors)[0]);
      return;
    }

    const passwordErrors = validatePasswordChange(passwordData);
    if (hasAccountFormErrors(passwordErrors)) {
      setGlobalError(Object.values(passwordErrors)[0]);
      return;
    }

    const emailErrors = validateEmailChange(emailData);
    if (hasAccountFormErrors(emailErrors)) {
      setGlobalError(Object.values(emailErrors)[0]);
      return;
    }

    setPwdModalError('');
    setPendingAction('SAVE');
    setIsPwdModalOpen(true);
  };

  const handleInitDelete = () => {
    if (hasBlockingSurveys) return;

    setGlobalError('');
    setGlobalSuccess('');
    setPwdModalError('');
    setPendingAction('DELETE');
    setIsPwdModalOpen(true);
  };

  const handleConfirmPasswordModal = async (currentPassword) => {
    if (!pendingAction) return;

    setPwdModalError('');
    setIsSaving(true);

    try {
      if (pendingAction === 'DELETE') {
        await deleteMyAccount({ currentPassword });
        clearAuthentication();
        setPendingAction(null);
        setIsPwdModalOpen(false);
        navigate('/inicia-sesion', { replace: true });
        return;
      }

      if (pendingAction === 'SAVE') {
        const normalizedPersonalData = {
          name: personalData.nombre.trim(),
          institution: personalData.institucion.trim(),
          gender: personalData.genero,
        };

        const isPersonalChanged =
          normalizedPersonalData.name !== initialPersonalData.nombre.trim() ||
          normalizedPersonalData.institution !== initialPersonalData.institucion.trim() ||
          normalizedPersonalData.gender !== initialPersonalData.genero;

        const payload = {
          currentPassword,
          profile: isPersonalChanged ? normalizedPersonalData : null,
          password: passwordData.newPassword ? { newPassword: passwordData.newPassword } : null,
          email: emailData.newEmail ? { newEmail: emailData.newEmail.trim().toLowerCase() } : null,
        };

        const updatedUser = await updateMyAccount(payload);
        const normalizedUser = updatedUser?.user ?? updatedUser;

        if (isPersonalChanged) {
          const updatedPersonalData = {
            nombre: normalizedUser?.name ?? normalizedPersonalData.name,
            institucion: normalizedUser?.institution ?? normalizedPersonalData.institution,
            genero: normalizedUser?.gender ?? normalizedPersonalData.gender,
          };

          setPersonalData(updatedPersonalData);
          setInitialPersonalData(updatedPersonalData);
        }

        if (passwordData.newPassword) {
          setPasswordData({ newPassword: '', confirmPassword: '' });
        }

        if (emailData.newEmail) {
          const updatedEmail = normalizedUser?.email ?? emailData.newEmail.trim().toLowerCase();

          setEmailData({
            currentEmail: updatedEmail,
            newEmail: '',
            confirmNewEmail: '',
          });
        }

        setAuthenticatedUser(updatedUser);
        setPendingAction(null);
        setIsPwdModalOpen(false);
        setGlobalSuccess('Los cambios se han guardado correctamente. Redirigiendo al panel...');

        redirectTimeoutRef.current = setTimeout(() => {
          navigate('/panel');
        }, 1500);
      }
    } catch (error) {
      setPwdModalError(error.message || 'La contraseña introducida es incorrecta o no se ha podido procesar la solicitud.');
    } finally {
      setIsSaving(false);
    }
  };

  const handleClosePasswordModal = () => {
    if (isSaving) return;

    setIsPwdModalOpen(false);
    setPendingAction(null);
    setPwdModalError('');
  };

  if (isLoading) {
    return (
      <main className="account-page">
        <div className="account-container">
          <Skeleton variant="list" count={4} hasActions={false} />
        </div>
      </main>
    );
  }

  return (
    <main className="account-page">
      <div className="account-container">
        <nav className="breadcrumb-nav" aria-label="Migas de pan">
          <Link to="/panel">PANEL</Link>
          <MdChevronRight aria-hidden="true" />
          <span aria-current="page">CONFIGURACIÓN DE CUENTA</span>
        </nav>

        <header className="account-page-header">
          <div>
            <h1>MI CUENTA</h1>
            <p className="account-subtitle">
              Gestiona tu información personal, seguridad y preferencias.
            </p>
          </div>

          <button type="button" className="btn-save-changes" disabled={isSaving} onClick={handleInitSave}>
            <MdSave className="btn-save-icon" aria-hidden="true" />
            <span>{isSaving ? 'GUARDANDO...' : 'GUARDAR CAMBIOS'}</span>
          </button>
        </header>

        <ErrorMessage message={globalError} onClose={() => setGlobalError('')} />

        {globalSuccess && (
          <div className="account-success-banner" role="status">
            <span>{globalSuccess}</span>
          </div>
        )}

        <form onSubmit={handleInitSave} className="account-sections-wrapper">
          <section className="account-card-section">
            <h2 className="account-section-title">INFORMACIÓN PERSONAL</h2>

            <div className="account-grid-2">
              <div className="input-field-group">
                <label htmlFor="nombre">Nombre</label>
                <input type="text" id="nombre" name="nombre" value={personalData.nombre} onChange={handlePersonalChange} autoComplete="name" />
              </div>

              <div className="input-field-group">
                <label htmlFor="institucion">Institución</label>
                <input type="text" id="institucion" name="institucion" value={personalData.institucion} onChange={handlePersonalChange} autoComplete="organization" />
              </div>
            </div>

            <div className="account-grid-2 margin-top-field">
              <div className="input-field-group">
                <label htmlFor="genero">Género</label>
                <Dropdown id="genero" name="genero" value={personalData.genero} onChange={handlePersonalChange}>
                  {GENDER_OPTIONS.map((option) => (
                    <option key={option.value} value={option.value}>
                      {option.label}
                    </option>
                  ))}
                </Dropdown>
              </div>
            </div>
          </section>

          <section className="account-card-section">
            <h2 className="account-section-title">CAMBIAR CONTRASEÑA</h2>

            <div className="account-grid-2">
              <div className="input-field-group">
                <label htmlFor="newPassword">Nueva contraseña</label>
                <div className="password-input-wrapper">
                  <input type={showPassword.new ? 'text' : 'password'} id="newPassword" name="newPassword" value={passwordData.newPassword} onChange={handlePasswordChange} autoComplete="new-password" />
                  <button type="button" className="btn-toggle-eye" onClick={() => togglePasswordVisibility('new')} aria-label={showPassword.new ? 'Ocultar nueva contraseña' : 'Mostrar nueva contraseña'}>
                    {showPassword.new ? <MdOutlineVisibilityOff aria-hidden="true" /> : <MdOutlineVisibility aria-hidden="true" />}
                  </button>
                </div>
              </div>

              <div className="input-field-group">
                <label htmlFor="confirmPassword">Confirmar nueva contraseña</label>
                <div className="password-input-wrapper">
                  <input type={showPassword.confirm ? 'text' : 'password'} id="confirmPassword" name="confirmPassword" value={passwordData.confirmPassword} onChange={handlePasswordChange} autoComplete="new-password" />
                  <button type="button" className="btn-toggle-eye" onClick={() => togglePasswordVisibility('confirm')} aria-label={showPassword.confirm ? 'Ocultar confirmación de contraseña' : 'Mostrar confirmación de contraseña'}>
                    {showPassword.confirm ? <MdOutlineVisibilityOff aria-hidden="true" /> : <MdOutlineVisibility aria-hidden="true" />}
                  </button>
                </div>
              </div>
            </div>

            <div className="password-requirements">
              <p>La nueva contraseña debe cumplir:</p>
              <div className="requirement-tag">
                <span className={`status-dot ${passwordData.newPassword.length >= 8 ? 'valid' : ''}`} />
                <span>Mínimo 8 caracteres</span>
              </div>
            </div>
          </section>

          <section className="account-card-section">
            <h2 className="account-section-title">CAMBIAR CORREO</h2>

            <div className="margin-bottom-field">
              <div className="input-field-group max-w-disabled">
                <label htmlFor="currentEmail">Correo actual</label>
                <input type="email" id="currentEmail" name="currentEmail" value={emailData.currentEmail} disabled className="input-disabled" autoComplete="email" />
              </div>
            </div>

            <div className="account-grid-2">
              <div className="input-field-group">
                <label htmlFor="newEmail">Nuevo correo</label>
                <input type="email" id="newEmail" name="newEmail" value={emailData.newEmail} onChange={handleEmailChange} autoComplete="email" />
              </div>

              <div className="input-field-group">
                <label htmlFor="confirmNewEmail">Confirmar nuevo correo</label>
                <input type="email" id="confirmNewEmail" name="confirmNewEmail" value={emailData.confirmNewEmail} onChange={handleEmailChange} autoComplete="email" />
              </div>
            </div>
          </section>

          <section className="account-card-section">
            <h2 className="account-section-title">ELIMINAR CUENTA</h2>
            <p className="delete-account-subtext">
              Si decides eliminar tu cuenta, perderás el acceso a la plataforma y a tus datos asociados.
            </p>

            <div className="delete-account-row">
              {hasBlockingSurveys && (
                <div className="warning-banner-box">
                  <MdOutlineWarningAmber className="warning-box-icon" aria-hidden="true" />
                  <div className="warning-box-content">
                    <h3>No puedes solicitar la baja porque tienes encuestas propias.</h3>
                    <p>Debes eliminar o transferir la propiedad de tus encuestas antes de eliminar tu cuenta.</p>
                  </div>
                </div>
              )}

              <button type="button" className={`btn-delete-account ${hasBlockingSurveys ? 'disabled' : ''}`} disabled={hasBlockingSurveys} onClick={handleInitDelete}>
                <MdOutlineDelete className="btn-delete-icon" aria-hidden="true" />
                <span>Solicitar baja de cuenta</span>
              </button>
            </div>
          </section>
        </form>
      </div>

      <PasswordConfirmModal
        isOpen={isPwdModalOpen}
        onClose={handleClosePasswordModal}
        onConfirm={handleConfirmPasswordModal}
        isProcessing={isSaving}
        errorMessage={pwdModalError}
        onClearError={() => setPwdModalError('')}
      />
    </main>
  );
}
