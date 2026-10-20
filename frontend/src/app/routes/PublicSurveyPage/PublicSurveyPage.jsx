import { useEffect, useRef, useState } from 'react';
import {
  MdArrowBack,
  MdCheckCircle,
  MdClose,
  MdInfoOutline,
  MdLockOutline,
} from 'react-icons/md';
import { Link, useParams } from 'react-router-dom';

import {
  submitAuthenticatedParticipation,
  submitPublicParticipation,
} from '../../../api/participationApi.js';
import {
  accessPasswordProtectedSurvey,
  getPublicSurveyById,
  getRegisteredSurveyByCode,
} from '../../../api/surveysApi.js';
import errorImage from '../../../assets/images/error-page-background.png';
import happyFormImage from '../../../assets/images/happy-form-white.png';
import { Skeleton } from '../../../components/Skeleton/Skeleton.jsx';
import { Survey } from '../../../components/Survey/Survey.jsx';
import { SURVEY_MODES } from '../../../components/Survey/SurveyModes.js';
import { useAuth } from '../../../context/useAuth.js';

import '../ErrorPage/ErrorPage.css';
import './PublicSurveyPage.css';

const ALREADY_SUBMITTED_CODE = 'PARTICIPATION_ALREADY_SUBMITTED';

function getRequestErrorMessage(error, fallback) {
  return error?.status && error.message ? error.message : fallback;
}

/** Permite consultar y responder una encuesta disponible. */
export function PublicSurveyPage() {
  const { publicCode } = useParams();
  const { isAuthenticated, isAuthLoading } = useAuth();
  const passwordInputRef = useRef(null);
  const statusRef = useRef(null);
  const surveyContentRef = useRef(null);
  const [survey, setSurvey] = useState(null);
  const [requiresPassword, setRequiresPassword] = useState(false);
  const [accessPassword, setAccessPassword] = useState('');
  const [passwordError, setPasswordError] = useState('');
  const [loadError, setLoadError] = useState('');
  const [submitError, setSubmitError] = useState('');
  const [alreadyParticipated, setAlreadyParticipated] = useState(false);
  const [isLoading, setIsLoading] = useState(true);
  const [isUnlocking, setIsUnlocking] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [submitted, setSubmitted] = useState(false);
  const [requestVersion, setRequestVersion] = useState(0);

  useEffect(() => {
    if (isAuthLoading) return undefined;

    let isActive = true;

    Promise.resolve().then(() => {
      if (!isActive) return;
      setIsLoading(true);
      setLoadError('');
      setRequiresPassword(false);
      setSurvey(null);
      setSubmitted(false);
      setAlreadyParticipated(false);
      setSubmitError('');
    });

    const getSurvey = isAuthenticated
      ? getRegisteredSurveyByCode
      : getPublicSurveyById;

    getSurvey(publicCode)
      .then((response) => {
        if (!isActive) return;
        if (response.accessMode === 'PASSWORD_PROTECTED') {
          setRequiresPassword(true);
          setSurvey(null);
          return;
        }
        setSurvey(response);
      })
      .catch((error) => {
        if (!isActive) return;
        setSurvey(null);
        if (error.status === 403) {
          setRequiresPassword(true);
          return;
        }
        setLoadError(getRequestErrorMessage(
          error,
          'No hemos podido conectar con el servidor. Comprueba tu conexión e inténtalo de nuevo.',
        ));
      })
      .finally(() => {
        if (isActive) setIsLoading(false);
      });

    return () => {
      isActive = false;
    };
  }, [isAuthenticated, isAuthLoading, publicCode, requestVersion]);

  useEffect(() => {
    if (submitted) statusRef.current?.focus();
  }, [submitted]);

  async function handleUnlock(event) {
    event.preventDefault();

    if (!accessPassword.trim()) {
      setPasswordError('Escribe la contraseña de la encuesta.');
      passwordInputRef.current?.focus();
      return;
    }

    setIsUnlocking(true);
    setPasswordError('');

    try {
      const response = await accessPasswordProtectedSurvey(publicCode, accessPassword);
      setSurvey(response);
      setRequiresPassword(false);
      window.requestAnimationFrame(() => surveyContentRef.current?.focus());
    } catch (error) {
      setPasswordError(error.status === 403
        ? 'La contraseña no es correcta. Inténtalo de nuevo.'
        : getRequestErrorMessage(
          error,
          'No hemos podido comprobar la contraseña. Comprueba tu conexión e inténtalo de nuevo.',
        ));
      window.requestAnimationFrame(() => passwordInputRef.current?.focus());
    } finally {
      setIsUnlocking(false);
    }
  }

  async function handleSubmit(answers) {
    const requiresPassword = survey.accessMode === 'PASSWORD_PROTECTED';
    if (requiresPassword && !accessPassword.trim()) {
      setPasswordError('Escribe la contraseña de la encuesta.');
      passwordInputRef.current?.focus();
      return;
    }

    setIsSubmitting(true);
    setPasswordError('');
    setSubmitError('');
    setAlreadyParticipated(false);

    try {
      const submitParticipation = isAuthenticated
        ? submitAuthenticatedParticipation
        : submitPublicParticipation;
      await submitParticipation(publicCode, {
        accessPassword: requiresPassword ? accessPassword : null,
        answers,
      });
      setSubmitted(true);
    } catch (error) {
      if (requiresPassword && error.status === 403) {
        setSurvey(null);
        setRequiresPassword(true);
        setPasswordError('La contraseña no es correcta. Inténtalo de nuevo.');
        window.requestAnimationFrame(() => passwordInputRef.current?.focus());
      } else if (
        error.status === 409
        && error.data?.code === ALREADY_SUBMITTED_CODE
      ) {
        setAlreadyParticipated(true);
        window.requestAnimationFrame(() => statusRef.current?.focus());
      } else {
        setSubmitError(getRequestErrorMessage(
          error,
          'No se han podido enviar las respuestas. Comprueba tu conexión e inténtalo de nuevo.',
        ));
        window.requestAnimationFrame(() => statusRef.current?.focus());
      }
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <main className="public-survey-page">
      <div className="public-survey-container">
        <Link className="public-survey-back" to="/">
          <MdArrowBack aria-hidden="true" />
          <span>Volver a las encuestas</span>
        </Link>

        {isLoading && (
          <section aria-busy="true" aria-label="Cargando encuesta">
            <Skeleton variant="list" count={3} hasActions={false} label="Cargando encuesta…" />
          </section>
        )}

        {!isLoading && loadError && (
          <section className="error-page-container public-survey-load-error" role="alert">
            <div className="error-page-text">
              <p className="error-page-logo" aria-hidden="true">FormQuid</p>
              <h1 className="error-page-status">No hemos podido abrir la encuesta</h1>
              <p className="error-page-description">{loadError}</p>
              <button
                className="public-survey-retry"
                type="button"
                onClick={() => setRequestVersion((version) => version + 1)}
              >
                Reintentar
              </button>
            </div>
            <div className="error-page-illustration">
              <img className="error-page-image" src={errorImage} alt="" aria-hidden="true" />
            </div>
          </section>
        )}

        {!isLoading && requiresPassword && !survey && (
          <form className="public-survey-password" onSubmit={handleUnlock} noValidate>
            <h1>
              <MdLockOutline aria-hidden="true" />
              Encuesta protegida
            </h1>
            <p>Escribe la contraseña para acceder a las preguntas.</p>
            <label htmlFor="public-survey-password">Contraseña de la encuesta</label>
            <input
              ref={passwordInputRef}
              id="public-survey-password"
              type="password"
              value={accessPassword}
              autoComplete="current-password"
              aria-describedby={passwordError ? 'public-survey-password-error' : 'public-survey-password-help'}
              aria-invalid={passwordError ? 'true' : undefined}
              onChange={(event) => {
                setAccessPassword(event.target.value);
                if (passwordError) setPasswordError('');
              }}
            />
            <p id="public-survey-password-help">Solicítala a la persona que te compartió la encuesta.</p>
            {passwordError && (
              <p id="public-survey-password-error" className="public-survey-field-error" role="alert">
                {passwordError}
              </p>
            )}
            <button type="submit" disabled={isUnlocking}>
              {isUnlocking ? 'Comprobando...' : 'Acceder a la encuesta'}
            </button>
          </form>
        )}

        {!isLoading && survey && submitted && (
          <section
            ref={statusRef}
            className="public-survey-message public-survey-message--success"
            role="status"
            tabIndex={-1}
          >
            <MdCheckCircle aria-hidden="true" />
            <h1>¡Respuestas enviadas!</h1>
            <img
              className="public-survey-message-image"
              src={happyFormImage}
              alt="Mascota de FormQuid sonriendo"
            />
            <p>Gracias por participar. Tus respuestas se han guardado correctamente.</p>
            <Link to="/">Volver a las encuestas</Link>
          </section>
        )}

        {!isLoading && survey && !submitted && (
          <div ref={surveyContentRef} className="public-survey-content" tabIndex={-1}>
            {alreadyParticipated && (
              <section
                ref={statusRef}
                className="public-survey-participation-notice"
                role="alert"
                tabIndex={-1}
                aria-labelledby="public-survey-participation-notice-title"
              >
                <MdInfoOutline aria-hidden="true" />
                <div>
                  <h2 id="public-survey-participation-notice-title">
                    Ya has participado en esta encuesta
                  </h2>
                  <p>
                    Esta encuesta ya tiene una respuesta tuya. Para cuidar los resultados,
                    cada persona solo puede enviarla una vez.
                  </p>
                </div>
                <button
                  type="button"
                  aria-label="Cerrar aviso de participación anterior"
                  onClick={() => setAlreadyParticipated(false)}
                >
                  <MdClose aria-hidden="true" />
                </button>
              </section>
            )}

            {submitError && (
              <div ref={statusRef} className="public-survey-submit-error" role="alert" tabIndex={-1}>
                {submitError}
              </div>
            )}

            <Survey
              data={survey}
              mode={SURVEY_MODES.RESPOND}
              isSubmitting={isSubmitting}
              onSubmit={handleSubmit}
            />
          </div>
        )}
      </div>
    </main>
  );
}
