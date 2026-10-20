"""Comprueba la configuración de producción sin imprimir secretos."""

import json
import subprocess
import sys
from pathlib import Path


ROOT = Path(__file__).resolve().parent
ENV_FILE = ROOT / ".env"
REQUIRED = (
    "SUPERADMIN_EMAIL",
    "SUPERADMIN_PASSWORD",
    "SUPERADMIN_NAME",
    "SUPERADMIN_INSTITUTION",
)


def main() -> int:
    if not ENV_FILE.is_file():
        print("Falta el archivo .env", file=sys.stderr)
        return 1

    try:
        result = subprocess.run(
            [
                "docker", "compose", "--env-file", str(ENV_FILE),
                "-f", str(ROOT / "docker-compose.yml"),
                "-f", str(ROOT / "docker-compose.prod.yml"),
                "config", "--format", "json",
            ],
            cwd=ROOT,
            capture_output=True,
            text=True,
            check=False,
        )
    except FileNotFoundError:
        print("Docker Compose no está disponible", file=sys.stderr)
        return 1

    if result.returncode != 0:
        print("La configuración de producción no se puede resolver; revisa .env", file=sys.stderr)
        return 1

    try:
        environment = json.loads(result.stdout)["services"]["backend"]["environment"]
    except (ValueError, KeyError, TypeError):
        print("No se pudo leer el entorno del backend desde Docker Compose", file=sys.stderr)
        return 1

    missing = [key for key in REQUIRED if not str(environment.get(key) or "").strip()]
    if missing:
        print("Variables obligatorias vacías: " + ", ".join(missing), file=sys.stderr)
        return 1
    if environment.get("SPRING_PROFILES_ACTIVE") != "prod":
        print("El backend debe usar el perfil prod", file=sys.stderr)
        return 1

    print("Configuración de producción verificada sin mostrar sus valores")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
