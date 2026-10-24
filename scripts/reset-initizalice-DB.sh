#!/usr/bin/env bash
set -euo pipefail

usage() {
  cat <<'USAGE'
Uso: scripts/reset-initizalice-DB.sh --confirm-reset

Reinicia los datos de desarrollo de FormQuid conservando el superadministrador
y las categorías iniciales. Requiere psql o Docker y la configuración DB_*
de .env o de las variables de entorno.

Revisa scripts/reset-initizalice-DB.sql antes de ejecutarlo.
USAGE
}

if [[ $# -eq 1 && ( "$1" == '--help' || "$1" == '-h' ) ]]; then
  usage
  exit 0
fi

if [[ $# -ne 1 || "${1:-}" != '--confirm-reset' ]]; then
  usage >&2
  exit 2
fi

script_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
env_file="$script_dir/../.env"
sql_file="$script_dir/reset-initizalice-DB.sql"

if [[ ! -f "$sql_file" || ! -r "$sql_file" ]]; then
  printf 'No se puede leer %s.\n' "$sql_file" >&2
  exit 2
fi

if [[ -f "$env_file" ]]; then
  while IFS= read -r line || [[ -n "$line" ]]; do
    line="${line%$'\r'}"
    line="${line#"${line%%[![:space:]]*}"}"
    [[ -z "$line" || "$line" == \#* ]] && continue
    [[ "$line" == export\ * ]] && line="${line#export }"
    [[ "$line" == *=* ]] || continue

    key="${line%%=*}"
    key="${key%"${key##*[![:space:]]}"}"
    case "$key" in
      DB_HOST|DB_PORT|DB_NAME|DB_USERNAME|DB_PASSWORD|DB_SSLMODE) ;;
      *) continue ;;
    esac
    [[ -n "${!key:-}" ]] && continue

    value="${line#*=}"
    value="${value#"${value%%[![:space:]]*}"}"
    value="${value%"${value##*[![:space:]]}"}"
    if [[ ${#value} -ge 2 ]]; then
      if [[ "${value:0:1}" == '"' && "${value: -1}" == '"' ]] ||
         [[ "${value:0:1}" == "'" && "${value: -1}" == "'" ]]; then
        value="${value:1:${#value}-2}"
      fi
    fi
    printf -v "$key" '%s' "$value"
    export "$key"
  done < "$env_file"
fi

for variable in DB_HOST DB_PORT DB_NAME DB_USERNAME DB_PASSWORD; do
  if [[ -z "${!variable:-}" ]]; then
    printf 'Falta %s en las variables de entorno o en %s.\n' "$variable" "$env_file" >&2
    exit 2
  fi
done

export PGHOST="$DB_HOST"
export PGPORT="$DB_PORT"
export PGDATABASE="$DB_NAME"
export PGUSER="$DB_USERNAME"
export PGPASSWORD="$DB_PASSWORD"
export PGSSLMODE="${DB_SSLMODE:-require}"

unset PGHOSTADDR PGSERVICE

send_sql() {
  printf "SET formquid.allow_dev_seed = 'yes';\n"
  cat -- "$sql_file"
}

run_sql() {
  if command -v psql >/dev/null 2>&1; then
    send_sql | psql -X -w -v ON_ERROR_STOP=1
  elif command -v docker >/dev/null 2>&1; then
    if ! docker info >/dev/null 2>&1; then
      printf 'Docker está instalado, pero no se puede acceder a su servicio. Comprueba «docker info».\n' >&2
      return 2
    fi
    printf 'psql no está instalado; se usará el cliente PostgreSQL de Docker.\n' >&2
    send_sql | docker run --rm -i --network host \
      -e PGHOST -e PGPORT -e PGDATABASE -e PGUSER -e PGPASSWORD -e PGSSLMODE \
      postgres:16-alpine psql -X -w -v ON_ERROR_STOP=1
  else
    printf 'No se encontró psql ni Docker. Instala postgresql-client o Docker para ejecutar la carga.\n' >&2
    return 2
  fi
}

printf 'Iniciando el reinicio de %s en %s:%s...\n' "$DB_NAME" "$DB_HOST" "$DB_PORT"
if run_sql; then
  printf 'Reinicio de %s finalizado correctamente.\n' "$DB_NAME"
else
  status=$?
  printf 'El reinicio de %s ha fallado; revisa el error anterior.\n' "$DB_NAME" >&2
  exit "$status"
fi
