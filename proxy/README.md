# Signing Proxy Setup

Tesla's Fleet API requires vehicle commands to be Ed25519-signed. This
stack runs Tesla's official `tesla-http-proxy` to handle that, plus a
static file host for the two things your domain must serve.

## One-time setup

1. **Register a Tesla Developer app** at https://developer.tesla.com,
   get a Client ID. Put it in `local.properties` as `tesla.clientId`.

2. **Point a domain at this server** (e.g. `flipwidget.example.com`),
   with a TLS certificate for it.

3. **Generate the Ed25519 signing keypair**:
   ```bash
   openssl genpkey -algorithm ed25519 -out private-key/signing-key.pem
   openssl pkey -in private-key/signing-key.pem -pubout -out public-key/com.tesla.3p.public-key.pem
   ```

4. **Get your Android app's signing certificate SHA-256 fingerprint**
   (from the keystore you'll release-sign the app with):
   ```bash
   keytool -list -v -keystore your-release.keystore -alias your-key-alias
   ```
   Paste it into `well-known/assetlinks.json`, replacing the placeholder.

5. **Copy your TLS cert/key** into `private-key/tls-cert.pem` and
   `private-key/tls-key.pem` (not committed — see `.gitignore`).

6. **Start the stack**:
   ```bash
   docker compose -f proxy/docker-compose.yml up -d
   ```
   `tesla-http-proxy` listens on 4443 (map it behind your real HTTPS
   ingress on 443); `public-key-host` serves:
   - `https://your-domain/.well-known/appspecific/com.tesla.3p.public-key.pem`
   - `https://your-domain/.well-known/assetlinks.json`

7. **Set app config** in `local.properties`:
   ```properties
   proxy.baseUrl=https://your-domain:4443/
   oauth.redirectUri=https://your-domain/oauth/callback
   oauth.redirectHost=your-domain
   vehicle.btName=Your Car's Bluetooth Display Name
   ```

8. **Enroll the virtual key on the vehicle** (one-time, per vehicle):
   open `https://tesla.com/_ak/your-domain` on a phone with the Tesla
   app installed and the vehicle nearby, and approve the pairing
   request from the vehicle's touchscreen.

## Verifying the proxy after deploy

```bash
curl -sk https://your-domain:4443/api/1/vehicles \
  -H "Authorization: Bearer <a valid access token>"
```

A `200` with a vehicle list (or a Tesla API error body, not a
connection error) means the proxy is reachable and forwarding
correctly.
