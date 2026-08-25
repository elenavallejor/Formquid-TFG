import { FiUserPlus } from 'react-icons/fi';
import { MdOutlineBarChart, MdOutlineLightbulb } from 'react-icons/md';

import { RegisterForm } from '../../../components/RegisterForm/RegisterForm';

import './RegisterPage.css';

/**
 * Pagina de registro (Solicita cuenta de editor)
 * 
 * Muestra la página pública de solicitud de registro y presenta la 
 * información sobre el acceso como editor.
 */
export function RegisterPage() {
  return (
    <main id="register-page" aria-labelledby="register-title">
      <section className="register-card">
        <div className="register-info">
          <div className="register-info-header">
            <div className="register-info-icon" aria-hidden="true">
              <FiUserPlus />
            </div>

            <h1 id="register-title">Solicita tu registro como editor</h1>

            <p className="register-info-description">
              Para garantizar la calidad y seguridad de la plataforma, el acceso
              de nuevos editores debe ser revisado por un administrador.
            </p>
          </div>

          <div className="register-info-item">
            <div className="info-item-icon" aria-hidden="true">
              <MdOutlineBarChart />
            </div>
            <div>
              <strong>Colabora y crea</strong>
              <span>
                Podrás crear encuestas, colaborar con otros editores y analizar
                resultados.
              </span>
            </div>
          </div>

          <div className="register-info-item">
            <div className="info-item-icon" aria-hidden="true">
              <MdOutlineLightbulb />
            </div>
            <div>
              <strong>Aporta valor</strong>
              <span>
                Únete a nuestra comunidad y forma parte de investigaciones de
                calidad.
              </span>
            </div>
          </div>
        </div>

        <div className="register-form-wrapper">
          <h2>Formulario de solicitud</h2>
          <p className="register-form-subtitle">
            Completa los datos para solicitar tu registro como editor.
          </p>

          <RegisterForm />
        </div>
      </section>
    </main>
  );
}