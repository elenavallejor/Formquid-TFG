"""Registra en GitHub únicamente despliegues confirmados por Vercel o Render."""

import json
import os
import re
import sys
import time
import urllib.error
import urllib.parse
import urllib.request


GITHUB_API = "https://api.github.com"
RENDER_API = "https://api.render.com/v1"
FRONTEND_URL = "https://formquid.vercel.app"
BACKEND_URL = "https://formquid-backend.onrender.com"
POLL_SECONDS = 15


def api_request(url, token, data=None):
    headers = {
        "Accept": "application/vnd.github+json" if url.startswith(GITHUB_API) else "application/json",
        "Authorization": f"Bearer {token}",
        "User-Agent": "formquid-deployment-recorder",
    }
    if url.startswith(GITHUB_API):
        headers["X-GitHub-Api-Version"] = "2022-11-28"
    if data is not None:
        headers["Content-Type"] = "application/json"
    request = urllib.request.Request(
        url,
        data=json.dumps(data).encode("utf-8") if data is not None else None,
        headers=headers,
        method="POST" if data is not None else "GET",
    )
    try:
        with urllib.request.urlopen(request, timeout=30) as response:
            return json.load(response)
    except urllib.error.HTTPError as error:
        raise RuntimeError(f"La API respondió con HTTP {error.code}: {url}") from error


def wait_for_vercel(repo, sha, token):
    url = f"{GITHUB_API}/repos/{repo}/commits/{sha}/status"
    for _ in range(48):
        result = api_request(url, token)
        status = next(
            (item for item in result.get("statuses", []) if item.get("context") == "Vercel"),
            None,
        )
        if status and status["state"] in {"success", "failure", "error"}:
            return {
                "state": "success" if status["state"] == "success" else "failure",
                "source_id": status.get("target_url") or f"vercel:{sha}",
                "log_url": status.get("target_url") or FRONTEND_URL,
                "environment": "frontend-production",
                "environment_url": FRONTEND_URL,
            }
        time.sleep(POLL_SECONDS)
    raise TimeoutError(f"Vercel no publicó un estado final para {sha} en 12 minutos")


def wait_for_render(sha, token, service_id):
    encoded_id = urllib.parse.quote(service_id, safe="")
    url = f"{RENDER_API}/services/{encoded_id}/deploys?limit=100"
    failed_states = {"build_failed", "update_failed", "pre_deploy_failed", "canceled"}
    for _ in range(72):
        result = api_request(url, token)
        matching = [
            item["deploy"]
            for item in result
            if (item.get("deploy", {}).get("commit") or {}).get("id") == sha
        ]
        if matching:
            deploy = max(matching, key=lambda item: item.get("createdAt", ""))
            state = deploy["status"]
            if state in {"live", "deactivated"} | failed_states:
                deploy_id = deploy["id"]
                return {
                    "state": "failure" if state in failed_states else "success",
                    "source_id": deploy_id,
                    "log_url": f"https://dashboard.render.com/web/{service_id}/deploys/{deploy_id}",
                    "environment": "backend-production",
                    "environment_url": BACKEND_URL,
                }
        time.sleep(POLL_SECONDS)
    raise TimeoutError(f"Render no informó de un despliegue final para {sha} en 18 minutos")


def record_github_deployment(repo, sha, token, provider, deployment):
    environment = deployment["environment"]
    query = urllib.parse.urlencode({"sha": sha, "environment": environment, "per_page": 100})
    base_url = f"{GITHUB_API}/repos/{repo}/deployments"
    previous = api_request(f"{base_url}?{query}", token)
    source_id = deployment["source_id"]
    existing = next(
        (
            item for item in previous
            if isinstance(item.get("payload"), dict)
            and item["payload"].get("provider") == provider
            and item["payload"].get("source_id") == source_id
        ),
        None,
    )
    if existing:
        deployment_id = existing["id"]
        statuses = api_request(f"{base_url}/{deployment_id}/statuses?per_page=1", token)
        if statuses and statuses[0]["state"] == deployment["state"]:
            print(f"El despliegue {provider} de {sha} ya figura en GitHub: {deployment_id}")
            return
    else:
        created = api_request(base_url, token, {
            "ref": sha,
            "task": "deploy",
            "auto_merge": False,
            "required_contexts": [],
            "environment": environment,
            "description": f"Despliegue de {provider} verificado en su plataforma",
            "production_environment": True,
            "payload": {"provider": provider, "source_id": source_id},
        })
        deployment_id = created["id"]

    api_request(f"{base_url}/{deployment_id}/statuses", token, {
        "state": deployment["state"],
        "description": f"Estado comunicado por {provider}",
        "environment_url": deployment["environment_url"],
        "log_url": deployment["log_url"],
    })
    print(f"Registrado {provider} {sha}: {deployment['state']} (GitHub {deployment_id})")


def main():
    if len(sys.argv) != 2 or sys.argv[1] not in {"frontend", "backend"}:
        raise SystemExit("Uso: record_deployment.py frontend|backend")
    provider = sys.argv[1]
    repo = os.environ["GITHUB_REPOSITORY"]
    sha = os.environ["TARGET_SHA"].lower()
    if not re.fullmatch(r"[0-9a-f]{40}", sha):
        raise SystemExit("TARGET_SHA debe ser un SHA completo de 40 caracteres")
    token = os.environ["GITHUB_TOKEN"]
    if provider == "frontend":
        deployment = wait_for_vercel(repo, sha, token)
    else:
        deployment = wait_for_render(
            sha,
            os.environ["RENDER_API_KEY"],
            os.environ["RENDER_SERVICE_ID"],
        )
    record_github_deployment(repo, sha, token, provider, deployment)
    if deployment["state"] == "failure":
        raise SystemExit(f"El despliegue de {provider} falló en su plataforma")


if __name__ == "__main__":
    main()
