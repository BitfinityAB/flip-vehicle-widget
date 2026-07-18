#!/usr/bin/env python3
"""Minimal OAuth token-exchange relay.

Tesla's Fleet API token endpoint requires a client_secret on every code
exchange and refresh -- a secret that must never be embedded in the
Android app (it would be extractable from the APK). This relay sits
between the app and Tesla: the app POSTs the normal token/refresh
request body (grant_type, code or refresh_token, redirect_uri,
client_id, code_verifier) with no secret attached; this relay injects
TESLA_CLIENT_SECRET server-side and forwards to Tesla's real endpoint,
then relays the response back unchanged.

Stdlib only, no dependencies, so no image build step is needed -- this
runs directly via the official python:3-alpine image with this file
bind-mounted in.
"""
import os
import sys
from http.server import BaseHTTPRequestHandler, HTTPServer
from urllib.error import HTTPError
from urllib.parse import parse_qsl, urlencode
from urllib.request import Request, urlopen

TESLA_TOKEN_URL = "https://fleet-auth.prd.vn.cloud.tesla.com/oauth2/v3/token"
CLIENT_SECRET = os.environ["TESLA_CLIENT_SECRET"]
PORT = int(os.environ.get("PORT", "8090"))


class Handler(BaseHTTPRequestHandler):
    def do_POST(self):
        length = int(self.headers.get("Content-Length", 0))
        body = self.rfile.read(length)
        fields = dict(parse_qsl(body.decode("utf-8")))
        fields["client_secret"] = CLIENT_SECRET
        upstream_body = urlencode(fields).encode("utf-8")

        upstream_req = Request(
            TESLA_TOKEN_URL,
            data=upstream_body,
            headers={"Content-Type": "application/x-www-form-urlencoded"},
            method="POST",
        )
        try:
            with urlopen(upstream_req, timeout=15) as resp:
                self._relay(resp.status, resp.read(), resp.headers.get("Content-Type"))
        except HTTPError as e:
            self._relay(e.code, e.read(), e.headers.get("Content-Type") if e.headers else None)
        except Exception:
            self._relay(502, b'{"error":"upstream_unreachable"}', "application/json")

    def _relay(self, status, body, content_type):
        self.send_response(status)
        self.send_header("Content-Type", content_type or "application/json")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def log_message(self, fmt, *args):
        # Deliberately log only method/path/status, never request/response
        # bodies -- those can contain authorization codes or refresh tokens.
        sys.stderr.write("%s - %s\n" % (self.address_string(), fmt % args))


if __name__ == "__main__":
    HTTPServer(("0.0.0.0", PORT), Handler).serve_forever()
