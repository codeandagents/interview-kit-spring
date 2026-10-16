"""A stub OIDC issuer for local checks only: serves discovery + a dummy JWK (nothing is ever signed) on :9000."""
import json
from http.server import BaseHTTPRequestHandler, HTTPServer

ISS = "http://localhost:9000"
DOC = {"issuer": ISS, "jwks_uri": ISS + "/jwks", "authorization_endpoint": ISS + "/authorize",
       "token_endpoint": ISS + "/token", "response_types_supported": ["code"],
       "subject_types_supported": ["public"], "id_token_signing_alg_values_supported": ["RS256"]}


class H(BaseHTTPRequestHandler):
    def do_GET(self):
        body = DOC if "openid-configuration" in self.path or "oauth-authorization-server" in self.path else {"keys": [{"kty": "RSA", "alg": "RS256", "use": "sig", "kid": "stub", "e": "AQAB", "n": "l6-BwMXdfeIyqFg4yN75YzBYwn8czCa9g4j4zPcrFua94zLJdCT4KF1AnqdWPCs1mUBlES7Z9pa7C7_-saZKBldKN2omjBlTuHBs9DRCpp3VlE6340JUEhYSFyIFts8uVFcshvsvBFTt0FC4WT1ATuvOJ0-YMqY6iBpygmIA-cSLDJ76HCVWH_6mZlTNRTAuKxlgSUjewedOOhfMDa0Gsr35sMPYZg8h4xLwPyHyuN8X3RNMO7Rh__8ra6sYjV6OIkXnY4hFoKsNlIMnGiiVDazG0cg6oslkD57rl2XdeaP360cGMC6RjuUk88LxYHu4xsBVcBxIi2x7rnXESv0CBw"}]}
        data = json.dumps(body).encode()
        self.send_response(200); self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(data))); self.end_headers(); self.wfile.write(data)

    def log_message(self, *a):
        pass


HTTPServer(("127.0.0.1", 9000), H).serve_forever()
