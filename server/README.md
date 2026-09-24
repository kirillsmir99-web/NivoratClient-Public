# Nivorat presence security rollout

## Behaviour

- Both JAR editions prove the Minecraft account through Mojang's session service before sending a heartbeat. The presence API stores verified name and UUID in memory for at most 45 seconds after the last heartbeat.
- The Dev edition has a login form in Settings. Each developer has a separate server-side login and password tied to one Minecraft UUID. The password is never in the JAR or ordinary config. The Dev session lasts up to 12 hours in memory and is revoked on logout, password rotation, or account removal.
- Dev peer responses contain only names on the server last reported by that authenticated Dev account. Both public and Dev players appear. No UUID or server address is returned to the client.
- The Minecraft account is verified; an arbitrary external Minecraft server is still a self-reported location. Strong proof of location would require cooperation from that game server or its proxy.

## Local preparation

1. Build `NivoratClient.jar` with `gradlew build` and `NivoratClient-Dev.jar` with `gradlew buildDev` in separate invocations. The build no longer rewrites tracked Java source or bundles a Dev key.
2. Inspect `nivorat-edition.txt` inside each JAR: `public` and `dev`. Do not distribute any old JAR or an old sources JAR containing a former key.
3. Collect each Dev user's Minecraft UUID and choose a distinct login name. Avoid sending passwords in chat.

## VDS activation plan

These are production changes. Back up the existing Python service and systemd unit before applying. Keep the backup outside the publicly served directory, preserve permissions, and record SHA-256 hashes. Stage `presence_server.py`, `manage_dev_users.py`, and `nivorat-presence.service` at versioned paths; validate their syntax and unit file before switching the active service. The API stays bound to `127.0.0.1:18790` behind the existing HTTPS Nginx location.

Create a dedicated system account `nivorat-presence` and `/var/lib/nivorat-presence` owned by that account with mode `0700`. The service reads `dev-users.json` there. Use `manage_dev_users.py add USERNAME MINECRAFT_UUID` under that account to generate one password per person. Deliver each password privately and only once. `revoke USERNAME` removes access; `add` with the same username rotates the password. Never commit `dev-users.json`.

After replacing the active service, run `systemd-analyze verify`, `nginx -t`, and check service status and fresh journal entries. Verify `/health`, rejection of unauthenticated `/heartbeat` and `/dev/peers`, a real Minecraft login, ordinary and Dev presence, server-specific markers, logout, revocation, and rate limiting. Check from a separate Minecraft client before calling the migration complete. Restore the backed-up service and unit if the new flow fails.

**Compatibility:** The new API intentionally rejects legacy unsigned heartbeats. Players running old public or Dev JARs will not appear until they update to 3.0.10. Publish and distribute the new JARs in the same rollout window as server activation.

**Former secret:** The old Dev key was present in source, previous builds, and potentially repository history. Removing it from the new source does not erase copies. The new server does not accept it, and old Dev builds must be retired.
