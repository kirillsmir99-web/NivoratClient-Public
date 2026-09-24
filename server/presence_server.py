#!/usr/bin/env python3
"""Nivorat presence API. Bind to loopback and expose only behind HTTPS proxy."""

import hashlib
import hmac
import json
import os
import re
import secrets
import threading
import time
import urllib.error
import urllib.parse
import urllib.request
from collections import defaultdict, deque
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path


HOST = "127.0.0.1"
PORT = 18790
DATA_DIR = Path(os.environ.get("NIVORAT_PRESENCE_DATA_DIR", "/var/lib/nivorat-presence"))
USERS_FILE = DATA_DIR / "dev-users.json"
PEER_TTL = 45
AUTH_TTL = 3600
DEV_TTL = 43200
MAX_PEERS = 5000
NAME_RE = re.compile(r"[A-Za-z0-9_]{3,16}\Z")
SERVER_RE = re.compile(r"[A-Za-z0-9._:-]{1,255}\Z")
HEX_UUID_RE = re.compile(r"[0-9a-f]{32}\Z")
TOKEN_RE = re.compile(r"[A-Za-z0-9_-]{32,256}\Z")
state_lock = threading.RLock()
password_slots = threading.BoundedSemaphore(4)
pending = {}
auth_tokens = {}
dev_tokens = {}
peers = {}
rate_events = defaultdict(deque)
last_cleanup = 0.0


def token_hash(token):
    return hashlib.sha256(token.encode("ascii")).digest()


def normalize_server(value):
    if not isinstance(value, str) or not SERVER_RE.fullmatch(value):
        return None
    server = value.lower().rstrip(".")
    return server[:-6] if server.endswith(":25565") else server


def uuid_text(value):
    if not isinstance(value, str):
        return None
    raw = value.lower().replace("-", "")
    if not HEX_UUID_RE.fullmatch(raw):
        return None
    return f"{raw[:8]}-{raw[8:12]}-{raw[12:16]}-{raw[16:20]}-{raw[20:]}"


def cleanup(now):
    global last_cleanup
    if now - last_cleanup < 10:
        return
    last_cleanup = now
    for table in (pending, auth_tokens, dev_tokens):
        for key, item in list(table.items()):
            if item["expires"] <= now:
                del table[key]
    for key, item in list(peers.items()):
        if item["seen"] + PEER_TTL <= now:
            del peers[key]
    for key, events in list(rate_events.items()):
        while events and events[0] < now - 60:
            events.popleft()
        if not events:
            del rate_events[key]


def allowed(ip, category, limit):
    now = time.monotonic()
    with state_lock:
        cleanup(now)
        events = rate_events[(ip, category)]
        while events and events[0] < now - 60:
            events.popleft()
        if len(events) >= limit:
            return False
        events.append(now)
        return True


def read_users():
    try:
        if USERS_FILE.stat().st_size > 65536:
            return {}
        data = json.loads(USERS_FILE.read_text(encoding="utf-8"))
        return data if isinstance(data, dict) else {}
    except (OSError, ValueError):
        return {}


def verify_password(password, user):
    try:
        salt = bytes.fromhex(user["salt"])
        expected = bytes.fromhex(user["hash"])
        candidate = hashlib.scrypt(password.encode("utf-8"), salt=salt, n=16384, r=8, p=1, dklen=32)
        return hmac.compare_digest(candidate, expected)
    except (ValueError, KeyError, TypeError):
        return False


def mojang_has_joined(name, challenge):
    query = urllib.parse.urlencode({"username": name, "serverId": challenge})
    url = "https://sessionserver.mojang.com/session/minecraft/hasJoined?" + query
    request = urllib.request.Request(url, headers={"User-Agent": "NivoratPresence/1.0"})
    try:
        with urllib.request.urlopen(request, timeout=4) as response:
            if response.status == 204:
                return None
            if response.status != 200:
                return None
            body = response.read(1025)
            if len(body) > 1024:
                return None
            data = json.loads(body)
    except urllib.error.HTTPError as error:
        if error.code in (204, 404):
            return None
        raise
    return data


class BoundedHTTPServer(ThreadingHTTPServer):
    daemon_threads = True
    request_queue_size = 64

    def __init__(self, address, handler):
        super().__init__(address, handler)
        self.slots = threading.BoundedSemaphore(32)

    def process_request(self, request, client_address):
        if not self.slots.acquire(blocking=False):
            self.shutdown_request(request)
            return
        try:
            super().process_request(request, client_address)
        except Exception:
            self.slots.release()
            raise

    def process_request_thread(self, request, client_address):
        try:
            super().process_request_thread(request, client_address)
        finally:
            self.slots.release()


class PresenceHandler(BaseHTTPRequestHandler):
    protocol_version = "HTTP/1.1"

    def log_message(self, format, *args):
        pass

    def client_ip(self):
        if self.client_address[0] in ("127.0.0.1", "::1"):
            forwarded = self.headers.get("X-Real-IP", "")
            if re.fullmatch(r"[0-9a-fA-F:.]{3,45}", forwarded):
                return forwarded
        return self.client_address[0]

    def send_json(self, code, data):
        body = json.dumps(data, separators=(",", ":")).encode("utf-8")
        self.close_connection = True
        self.send_response(code)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Cache-Control", "no-store")
        self.send_header("Connection", "close")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        try:
            self.wfile.write(body)
        except (BrokenPipeError, ConnectionResetError):
            pass

    def read_json(self):
        try:
            length = int(self.headers.get("Content-Length", "0"))
            if length < 1 or length > 2048:
                return None
            body = json.loads(self.rfile.read(length))
            return body if isinstance(body, dict) else None
        except (ValueError, UnicodeDecodeError, TimeoutError):
            return None

    def bearer(self):
        header = self.headers.get("Authorization", "")
        token = header[7:] if header.startswith("Bearer ") else ""
        return token if TOKEN_RE.fullmatch(token) else ""

    def presence(self, token):
        if not token or not TOKEN_RE.fullmatch(token):
            return None
        now = time.monotonic()
        with state_lock:
            cleanup(now)
            item = auth_tokens.get(token_hash(token))
            return dict(item) if item and item["expires"] > now else None

    def do_GET(self):
        path = urllib.parse.urlsplit(self.path).path.rstrip("/")
        if path == "/health":
            self.send_json(200, {"status": "healthy"})
            return
        if path != "/dev/peers":
            self.send_json(404, {"error": "not_found"})
            return
        if not allowed(self.client_ip(), "peers", 120):
            self.send_json(429, {"error": "rate_limited"})
            return
        presence = self.presence(self.headers.get("X-Presence-Token", ""))
        if not presence or not presence.get("server"):
            self.send_json(401, {"error": "unauthorized"})
            return
        now = time.monotonic()
        with state_lock:
            dev = dev_tokens.get(token_hash(self.bearer())) if self.bearer() else None
            if not dev or dev["expires"] <= now or dev["uuid"] != presence["uuid"]:
                self.send_json(403, {"error": "forbidden"})
                return
            viewer = peers.get(presence["uuid"])
            if not viewer or viewer["server"] != presence["server"] or viewer["seen"] + PEER_TTL <= now:
                self.send_json(403, {"error": "viewer_offline"})
                return
            names = [p["name"] for p in peers.values()
                     if p["server"] == presence["server"] and p["seen"] + PEER_TTL > now]
        current_user = read_users().get(dev["username"])
        if not isinstance(current_user, dict) or current_user.get("uuid") != dev["uuid"] \
                or current_user.get("hash") != dev["user_hash"]:
            self.send_json(403, {"error": "forbidden"})
            return
        if len(names) > 512:
            self.send_json(503, {"error": "too_many_peers"})
            return
        self.send_json(200, {"peers": [{"name": name} for name in names]})

    def do_POST(self):
        path = urllib.parse.urlsplit(self.path).path.rstrip("/")
        limits = {"/auth/challenge": 12, "/auth/verify": 12, "/heartbeat": 60,
                  "/dev/login": 5, "/dev/logout": 30}
        if path not in limits:
            self.send_json(404, {"error": "not_found"})
            return
        if not allowed(self.client_ip(), path, limits[path]):
            self.send_json(429, {"error": "rate_limited"})
            return
        data = self.read_json()
        if data is None:
            self.send_json(400, {"error": "bad_request"})
            return
        if path == "/auth/challenge":
            self.challenge(data)
        elif path == "/auth/verify":
            self.verify(data)
        elif path == "/heartbeat":
            self.heartbeat(data)
        elif path == "/dev/login":
            self.dev_login(data)
        else:
            self.dev_logout()

    def challenge(self, data):
        name = data.get("name")
        if not isinstance(name, str) or not NAME_RE.fullmatch(name):
            self.send_json(400, {"error": "invalid_name"})
            return
        challenge = secrets.token_hex(32)
        now = time.monotonic()
        with state_lock:
            if len(pending) >= 2000:
                self.send_json(503, {"error": "busy"})
                return
            pending[challenge] = {"name": name, "ip": self.client_ip(), "expires": now + 60}
        self.send_json(200, {"challenge": challenge})

    def verify(self, data):
        name = data.get("name")
        challenge = data.get("challenge")
        if not isinstance(name, str) or not NAME_RE.fullmatch(name) or not isinstance(challenge, str) \
                or not re.fullmatch(r"[a-f0-9]{64}", challenge):
            self.send_json(400, {"error": "bad_request"})
            return
        now = time.monotonic()
        with state_lock:
            item = pending.pop(challenge, None)
        if not item or item["expires"] <= now or item["ip"] != self.client_ip() \
                or item["name"].lower() != name.lower():
            self.send_json(401, {"error": "unauthorized"})
            return
        try:
            verified = mojang_has_joined(name, challenge)
        except (OSError, ValueError):
            self.send_json(503, {"error": "identity_service_unavailable"})
            return
        if not isinstance(verified, dict) or str(verified.get("name", "")).lower() != name.lower():
            self.send_json(401, {"error": "unauthorized"})
            return
        uuid = uuid_text(verified.get("id"))
        if not uuid:
            self.send_json(401, {"error": "unauthorized"})
            return
        token = secrets.token_urlsafe(32)
        with state_lock:
            if len(auth_tokens) >= 10000:
                self.send_json(503, {"error": "busy"})
                return
            auth_tokens[token_hash(token)] = {"name": verified["name"], "uuid": uuid,
                                               "server": "", "expires": time.monotonic() + AUTH_TTL}
        self.send_json(200, {"uuid": uuid, "token": token, "expires_in": AUTH_TTL})

    def heartbeat(self, data):
        token = self.bearer()
        presence = self.presence(token)
        server = normalize_server(data.get("server"))
        version = data.get("v", "")
        if not presence:
            self.send_json(401, {"error": "unauthorized"})
            return
        if not server or not isinstance(version, str) or len(version) > 32:
            self.send_json(400, {"error": "bad_request"})
            return
        now = time.monotonic()
        with state_lock:
            if len(peers) >= MAX_PEERS and presence["uuid"] not in peers:
                self.send_json(503, {"error": "busy"})
                return
            auth_tokens[token_hash(token)]["server"] = server
            peers[presence["uuid"]] = {"name": presence["name"], "server": server,
                                       "seen": now, "version": version}
        self.send_json(200, {"status": "ok"})

    def dev_login(self, data):
        presence = self.presence(self.headers.get("X-Presence-Token", ""))
        username = data.get("username")
        password = data.get("password")
        if not presence or not presence.get("server"):
            self.send_json(401, {"error": "unauthorized"})
            return
        now = time.monotonic()
        with state_lock:
            viewer = peers.get(presence["uuid"])
            if not viewer or viewer["server"] != presence["server"] or viewer["seen"] + PEER_TTL <= now:
                self.send_json(403, {"error": "viewer_offline"})
                return
        if not isinstance(username, str) or not re.fullmatch(r"[A-Za-z0-9_-]{3,32}", username) \
                or not isinstance(password, str) or not 12 <= len(password) <= 128:
            self.send_json(400, {"error": "bad_request"})
            return
        if not allowed(username.lower(), "dev-user", 10):
            self.send_json(429, {"error": "rate_limited"})
            return
        user = read_users().get(username)
        if not isinstance(user, dict) or user.get("uuid") != presence["uuid"]:
            self.send_json(403, {"error": "forbidden"})
            return
        if not password_slots.acquire(blocking=False):
            self.send_json(503, {"error": "busy"})
            return
        try:
            password_ok = verify_password(password, user)
        finally:
            password_slots.release()
        if not password_ok:
            self.send_json(403, {"error": "forbidden"})
            return
        token = secrets.token_urlsafe(32)
        with state_lock:
            if len(dev_tokens) >= 1000:
                self.send_json(503, {"error": "busy"})
                return
            dev_tokens[token_hash(token)] = {"username": username, "uuid": presence["uuid"],
                                             "user_hash": user["hash"],
                                             "expires": time.monotonic() + DEV_TTL}
        self.send_json(200, {"token": token, "expires_in": DEV_TTL})

    def dev_logout(self):
        token = self.bearer()
        if not token:
            self.send_json(401, {"error": "unauthorized"})
            return
        with state_lock:
            dev_tokens.pop(token_hash(token), None)
        self.send_json(200, {"status": "ok"})


if __name__ == "__main__":
    BoundedHTTPServer((HOST, PORT), PresenceHandler).serve_forever()
