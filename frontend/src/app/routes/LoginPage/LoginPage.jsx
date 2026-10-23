import { AuthPageLayout } from '../../../components/AuthPageLayout/AuthPageLayout.jsx';
import { LoginForm } from '../../../components/LoginForm/LoginForm';

/**
 * Muestra la página pública de inicio de sesión 
 * y contiene el formulario de autenticación.
 */
export function LoginPage() {
  return (
    <AuthPageLayout title="Iniciar sesión" description="Introduce tus credenciales para continuar">
      <LoginForm />
    </AuthPageLayout>
  );
}
