import './AuthPageLayout.css';

export function AuthPageLayout({ title, description, children }) {
  return (
    <main id="login-page" aria-labelledby="login-title">
      <div className="login-page-container">
        <section className="login-card-container" aria-describedby="login-description">
          <div className="login-logo-backdrop" aria-hidden="true">
            <span className="backdrop-logo-img" />
          </div>
          <div className="login-card">
            <div className="login-card-header">
              <h1 className="login-title" id="login-title">{title}</h1>
              <p className="login-subtitle" id="login-description">{description}</p>
            </div>
            {children}
          </div>
        </section>
      </div>
    </main>
  );
}
