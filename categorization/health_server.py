import json
import os
import socketserver
import threading
from http.server import BaseHTTPRequestHandler
from typing import Callable, Optional


class _Handler(BaseHTTPRequestHandler):
    liveness_check: Callable[[], bool]
    readiness_check: Callable[[], bool]

    def log_message(self, fmt: str, *args) -> None:
        if os.getenv("HEALTH_LOG_REQUESTS", "").lower() in {"1", "true", "yes"}:
            super().log_message(fmt, *args)

    def _write_json(self, status: int, payload: dict) -> None:
        body = json.dumps(payload).encode("utf-8")
        self.send_response(status)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def do_GET(self) -> None:  # noqa: N802
        if self.path in {"/health", "/healthz", "/health/liveness"}:
            ok = bool(self.liveness_check())
            self._write_json(200 if ok else 503, {"status": "ok" if ok else "failed"})
            return

        if self.path in {"/ready", "/readyz", "/health/readiness"}:
            ok = bool(self.readiness_check())
            self._write_json(200 if ok else 503, {"status": "ready" if ok else "not_ready"})
            return

        self._write_json(404, {"error": "not_found"})


class HealthServer:
    def __init__(
        self,
        host: str = "0.0.0.0",
        port: int = 8080,
        *,
        liveness_check: Optional[Callable[[], bool]] = None,
        readiness_check: Optional[Callable[[], bool]] = None,
    ) -> None:
        self._host = host
        self._port = port
        self._liveness_check = liveness_check or (lambda: True)
        self._readiness_check = readiness_check or (lambda: True)

        self._httpd: Optional[socketserver.TCPServer] = None
        self._thread: Optional[threading.Thread] = None

    def start(self) -> None:
        class _TCPServer(socketserver.ThreadingMixIn, socketserver.TCPServer):
            allow_reuse_address = True
            daemon_threads = True

        handler_cls = type(
            "HealthHandler",
            (_Handler,),
            {
                "liveness_check": staticmethod(self._liveness_check),
                "readiness_check": staticmethod(self._readiness_check),
            },
        )

        self._httpd = _TCPServer((self._host, self._port), handler_cls)
        self._thread = threading.Thread(target=self._httpd.serve_forever, name="health-server")
        self._thread.daemon = True
        self._thread.start()

    def stop(self) -> None:
        if not self._httpd:
            return
        self._httpd.shutdown()
        self._httpd.server_close()
