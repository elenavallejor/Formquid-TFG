import formQuidLogo from '../../../assets/images/formquid-logo.png';
import { LoginForm } from '../../../features/auth/components/LoginForm/LoginForm';

import './LoginPage.css';

export function LoginPage() {
  return (
    <main id="login-page" aria-labelledby="login-title">
      <div className="login-page-container">
        <section className="login-card-container" aria-describedby="login-description">
          <div className="login-logo-backdrop" aria-hidden="true">
            <img src={formQuidLogo} alt="" className="backdrop-logo-img" />
          </div>

          <div className="login-card">
            <div className="login-card-header">
              <h1 className="login-title" id="login-title">
                Iniciar sesión
              </h1>

              <p className="login-subtitle" id="login-description">
                Introduce tus credenciales para continuar
              </p>
            </div>

            <LoginForm />
          </div>
        </section>
      </div>
    </main>
  );
}