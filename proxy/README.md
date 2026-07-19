# Signing Proxy Setup

Tesla's Fleet API requires vehicle commands to be signed with a NIST P-256
(prime256v1/secp256r1) key. This stack runs Tesla's official `tesla-http-proxy`
for that, plus a
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

3. **Generate the P-256 command-signing keypair** (on the VPS, or generate
   locally and copy up — never let this leave your control). This must be
   NIST P-256 — `tesla-http-proxy` rejects other key types (including
   Ed25519) with `invalid private key: only elliptic curve keys supported`:
   ```bash
   openssl ecparam -name prime256v1 -genkey -noout -out private-key/signing-key.pem
   openssl ec -in private-key/signing-key.pem -pubout -out public-key/com.tesla.3p.public-key.pem
   ```

4. **Generate the internal self-signed TLS cert** for `tesla-http-proxy`
   (this is never exposed publicly — only Caddy talks to it, over the
   Docker-internal network). This must also be an EC key — `-tls-key`
   rejects RSA the same way:
   ```bash
   openssl req -x509 -newkey ec -pkeyopt ec_paramgen_curve:prime256v1 -nodes -days 3650 \
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

9. **Register your domain with the Fleet API** (one-time, per app -- this
   is separate from creating the app in the developer portal, and is easy
   to miss since nothing in the portal UI prompts for it). Skipping this
   step makes the vehicle enrollment page in step 10 fail with "Unable to
   Grant Third-Party Access -- This third party isn't registered with
   Tesla." Run on the VPS, where `TESLA_CLIENT_SECRET` already lives in
   `proxy/.env` -- never type the secret anywhere else:
   ```bash
   cd /opt/flip-vehicle-widget-proxy
   set -a; source .env; set +a
   CLIENT_ID="your-tesla-client-id"
   # Use the Fleet API base URL for your account's region:
   #   NA: https://fleet-api.prd.na.vn.cloud.tesla.com
   #   EU: https://fleet-api.prd.eu.vn.cloud.tesla.com
   FLEET_API_BASE="https://fleet-api.prd.eu.vn.cloud.tesla.com"

   ACCESS_TOKEN=$(curl -s -X POST https://fleet-auth.prd.vn.cloud.tesla.com/oauth2/v3/token \
     -d "grant_type=client_credentials" \
     -d "client_id=${CLIENT_ID}" \
     -d "client_secret=${TESLA_CLIENT_SECRET}" \
     -d "scope=openid vehicle_device_data vehicle_cmds vehicle_charging_cmds" \
     -d "audience=${FLEET_API_BASE}" \
     | python3 -c "import sys,json; print(json.load(sys.stdin)['access_token'])")

   curl -s -X POST "${FLEET_API_BASE}/api/1/partner_accounts" \
     -H "Authorization: Bearer ${ACCESS_TOKEN}" \
     -H "Content-Type: application/json" \
     -d '{"domain": "your-domain"}'
   ```
   A `200` response echoing back your `domain` and a `public_key` matching
   `https://your-domain/.well-known/appspecific/com.tesla.3p.public-key.pem`
   confirms registration succeeded.

10. **Enroll the virtual key on the vehicle** (one-time, per vehicle):
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
force-recreates the stack on every push to `main` that touches `proxy/**`
(force-recreate, not just `up -d`, because a config-file-only change like
a Caddyfile edit doesn't otherwise trigger a restart). It never touches
`proxy/private-key/`, `proxy/public-key/*.pem`, or `proxy/.env` on the
VPS — those only ever exist there, generated once in steps 3–4 and 6
above (the public key is *derived from* the VPS-only private key, so it's
VPS-only too, even though the directory itself is versioned via a
`.gitkeep`).

Required repository secrets (Settings → Secrets and variables → Actions):

| Secret | Value |
|---|---|
| `HETZNER_HOST` | VPS public IP or hostname |
| `HETZNER_USER` | SSH user with Docker access on the VPS (e.g. `deploy`) |
| `HETZNER_SSH_KEY` | Private key (PEM) for that user — generate a dedicated deploy key, don't reuse your personal one |

The first deploy still has to be manual (steps 1–9 above) — the workflow
only syncs config and restarts containers, it doesn't do initial key
generation or DNS setup.
