import './Footer.css';

/**
 * Pie de página público de FormQuid.
 *
 * Muestra la información básica del proyecto y enlaces secundarios de ayuda,
 * documentación y contacto.
 */
export function Footer() {
  return (
    <footer id="main-footer">
      <div className="footer-content">
        <p className="footer-copy">
          2026, FormQuid - Trabajo de Fin de Grado
        </p>

        <nav className="footer-navigation" aria-label="Enlaces del pie de página">
          <a className="footer-link" href="/terms">
            Términos de uso
          </a>

          |

          <a className="footer-link" href="/manual">
            Manual de usuario
          </a>

          |

          <a className="footer-link" href="/contact">
            Contacto
          </a>
        </nav>
      </div>
    </footer>
  );
}