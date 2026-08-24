import { MdOutlineEmail, MdOutlineBusiness, MdOutlinePerson, MdOutlineEdit, MdOutlineCalendarToday, MdCheck, MdClose } from 'react-icons/md';
import './TarjetaSolicitud.css';

const GENDER_TRANSLATIONS = {
  FEMALE: 'Mujer',
  MALE: 'Hombre',
  OTHER: 'Otro',
};

export function TarjetaSolicitud({ solicitud, onAccept, onDeny }) {
  const { id, name, email, institution, gender, motivation, createdAt } = solicitud;

  const formattedDate = createdAt ? new Date(createdAt).toLocaleString('es-ES', {
        day: '2-digit',
        month: '2-digit',
        year: 'numeric',
        hour: '2-digit',
        minute: '2-digit',
      })
    : '-';

  const genderLabel = GENDER_TRANSLATIONS[gender] || gender || 'No especificado';

  return (
    <article className="solicitud-card" aria-labelledby={`applicant-name-${id}`}>
      <div className="solicitud-col solicitud-col-personal">
        <h3 id={`applicant-name-${id}`} className="solicitud-applicant-name">
          {name}
        </h3>
        <p className="solicitud-section-label">DATOS PERSONALES:</p>
        <ul className="solicitud-data-list">
          <li>
            <MdOutlineEmail className="solicitud-icon" aria-hidden="true" />
            <span>{email}</span>
          </li>
          <li>
            <MdOutlineBusiness className="solicitud-icon" aria-hidden="true" />
            <span>{institution}</span>
          </li>
          <li>
            <MdOutlinePerson className="solicitud-icon" aria-hidden="true" />
            <span>{genderLabel}</span>
          </li>
        </ul>
      </div>

      <div className="solicitud-divider" aria-hidden="true" />

      <div className="solicitud-col solicitud-col-motivation">
        <p className="solicitud-section-label flex-align">
          <MdOutlineEdit className="solicitud-label-icon" aria-hidden="true" />
          <span>MOTIVACIÓN:</span>
        </p>
        <p className="solicitud-motivation-text">{motivation}</p>
      </div>

      <div className="solicitud-divider" aria-hidden="true" />

      <div className="solicitud-col solicitud-col-date">
        <p className="solicitud-section-label flex-align center-text">
          <MdOutlineCalendarToday className="solicitud-label-icon" aria-hidden="true" />
          <span>SOLICITADO EL:</span>
        </p>
        <time className="solicitud-date-value" dateTime={createdAt}>
          {formattedDate}
        </time>
      </div>

      <div className="solicitud-divider" aria-hidden="true" />

      <div className="solicitud-col solicitud-col-actions">
        <button
          type="button"
          className="btn-solicitud-action btn-accept"
          onClick={() => onAccept(solicitud)}
          aria-label={`Aceptar solicitud de ${name}`}
        >
          <span>ACEPTAR</span>
          <MdCheck className="action-btn-icon" aria-hidden="true" />
        </button>

        <button
          type="button"
          className="btn-solicitud-action btn-deny"
          onClick={() => onDeny(solicitud)}
          aria-label={`Denegar solicitud de ${name}`}
        >
          <span>DENEGAR</span>
          <MdClose className="action-btn-icon" aria-hidden="true" />
        </button>
      </div>
    </article>
  );
}