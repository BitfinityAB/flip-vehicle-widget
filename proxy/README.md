# Signing Proxy Setup

Tesla's Fleet API requires vehicle commands to be Ed25519-signed. This
stack runs Tesla's official `tesla-http-proxy` for that, plus a
Caddy reverse proxy in front that gets a free Let's Encrypt certificate
automatically and routes both the proxy API and the `.well-known` files
your domain must serve, all on one HTTPS domain.

## Architecture

```
Internet --443--> Caddy --routes-->  /.well-known/*      -> static files (public key, assetlinks.json)
                                      /oauth/callback     -> a plain "close this tab" page
                                      everything else     -> tesla-http-proxy (internal, self-signed TLS)
```

`tesla-http-proxy` always terminates its own TLS, so it gets a self-signed
cert for the internal Docker-network hop; Caddy is what the internet
actually talks to, with a real Let's Encrypt cert for your domain.

## One-time setup

1. **Register a Tesla Developer app** at https://developer.tesla.com,
   get a Client ID. Put it in `local.properties` as `tesla.clientId`.
   For "Allowed Origin URL" use `https://your-domain`, for "Allowed
   Redirect URI" use `https://your-domain/oauth/callback` — these don't
   need to be live yet to register the app, just owned by you.

2. **Point DNS at your server**: an `A` record for your domain (e.g.
   `flip-widget.example.com`) to the VPS's public IP. No TLS cert needed
   up front — Caddy obtains one automatically on first request, as long
   as ports 80 and 443 are reachable from the internet.

3. **Generate the Ed25519 signing keypair** (on the VPS, or generate
   locally and copy up — never let this leave your control):
   ```bash
   openssl genpkey -algorithm ed25519 -out private-key/signing-key.pem
   openssl pkey -in private-key/signing-key.pem -pubout -out public-key/com.tesla.3p.public-key.pem
   ```

4. **Generate the internal self-signed TLS cert** for `tesla-http-proxy`
   (this is never exposed publicly — only Caddy talks to it, over the
   Docker-internal network):
   ```bash
   openssl req -x509 -newkey rsa:2048 -nodes -days 3650 \
     -keyout private-key/internal-tls-key.pem \
     -out private-key/internal-tls-cert.pem \
     -subj "/CN=tesla-http-proxy-internal"
   ```

5. **Get your Android app's signing certificate SHA-256 fingerprint**
   (from the keystore you'll release-sign the app with):
   ```bash
   keytool -list -v -keystore your-release.keystore -alias your-key-alias
   ```
   Paste it into `well-known/assetlinks.json`, replacing the placeholder.

6. **Create `proxy/.env`** on the VPS (gitignored, not committed):
   ```properties
   PROXY_DOMAIN=your-domain
   ```

7. **Start the stack**:
   ```bash
   cd proxy
   docker compose up -d
   ```
   Once Caddy has obtained its certificate (usually seconds), the domain
   serves:
   - `https://your-domain/.well-known/appspecific/com.tesla.3p.public-key.pem`
   - `https://your-domain/.well-known/assetlinks.json`
   - `https://your-domain/api/1/...` (proxied to `tesla-http-proxy`)

8. **Set app config** in `local.properties`:
   ```properties
   proxy.baseUrl=https://your-domain/
   oauth.redirectUri=https://your-domain/oauth/callback
   oauth.redirectHost=your-domain
   vehicle.btName=Your Car's Bluetooth Display Name
   ```

9. **Enroll the virtual key on the vehicle** (one-time, per vehicle):
   open `https://tesla.com/_ak/your-domain` on a phone with the Tesla
   app installed and the vehicle nearby, and approve the pairing
   request from the vehicle's touchscreen.

## Verifying the proxy after deploy

```bash
curl -s https://your-domain/.well-known/assetlinks.json
curl -s https://your-domain/api/1/vehicles \
  -H "Authorization: Bearer <a valid access token>"
```

A `200` with a vehicle list (or a Tesla API error body, not a connection
error) means the proxy is reachable and forwarding correctly.

## Deploying via GitHub Actions

`.github/workflows/deploy-proxy.yml` rsyncs `proxy/` to the VPS and
restarts the stack on every push to `main` that touches `proxy/**`. It
never touches `proxy/private-key/` or `proxy/.env` on the VPS — those
only ever exist there, generated once in steps 3–4 and 6 above.

Required repository secrets (Settings → Secrets and variables → Actions):

| Secret | Value |
|---|---|
| `HETZNER_HOST` | VPS public IP or hostname |
| `HETZNER_USER` | SSH user with Docker access on the VPS (e.g. `deploy`) |
| `HETZNER_SSH_KEY` | Private key (PEM) for that user — generate a dedicated deploy key, don't reuse your personal one |

The first deploy still has to be manual (steps 1–9 above) — the workflow
only syncs config and restarts containers, it doesn't do initial key
generation or DNS setup.
