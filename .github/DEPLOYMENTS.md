# Registro de despliegues en GitHub

El workflow `Registrar despliegues` registra en la sección **Deployments** de GitHub
los despliegues terminados de Vercel y Render. No vuelve a desplegar la aplicación.

## Configuración de Render

1. En Render, crea una API key en **Account Settings → API Keys**. No la subas al repositorio.
2. En GitHub, abre **Settings → Secrets and variables → Actions → Secrets** y crea
   el secreto `RENDER_API_KEY` con esa clave.
3. En la pestaña **Variables**, crea `RENDER_SERVICE_ID` con el identificador que
   aparece en la página de `formquid-backend` en Render (`srv-...`).

Sin estos dos valores, el workflow registra el frontend y omite el backend. La
API key de Render permite acceder a recursos de la cuenta: consérvala únicamente
en GitHub Secrets y revócala en Render si deja de ser necesaria.

## Funcionamiento

- Un push a `main` que cambia `frontend/` espera al estado **Vercel** del mismo
  commit y registra su resultado en `frontend-production`.
- Un push a `main` que cambia `backend/` espera al despliegue del mismo commit en
  Render y registra su resultado en `backend-production`.
- Para registrar un despliegue anterior, abre **Actions → Registrar despliegues →
  Run workflow** e introduce el SHA completo del commit y el servicio.

El estado se obtiene del proveedor antes de crear el registro. Si el proveedor
no publica un resultado final, el workflow falla sin marcar el despliegue como
correcto en GitHub.
