#!/usr/bin/env python3
"""Create or revoke individually scoped Dev logins on the VDS."""

import hashlib
import json
import os
import re
import secrets
import sys
import tempfile
from pathlib import Path


data_dir = Path(os.environ.get("NIVORAT_PRESENCE_DATA_DIR", "/var/lib/nivorat-presence"))
users_file = data_dir / "dev-users.json"


def read_users():
    try:
        value = json.loads(users_file.read_text(encoding="utf-8"))
        return value if isinstance(value, dict) else {}
    except (FileNotFoundError, ValueError):
        return {}


def save_users(users):
    data_dir.mkdir(mode=0o700, parents=True, exist_ok=True)
    fd, temp_name = tempfile.mkstemp(prefix=".dev-users-", dir=data_dir)
    try:
        os.fchmod(fd, 0o600)
        with os.fdopen(fd, "w", encoding="utf-8") as output:
            json.dump(users, output, separators=(",", ":"), sort_keys=True)
            output.flush()
            os.fsync(output.fileno())
        os.replace(temp_name, users_file)
    finally:
        if os.path.exists(temp_name):
            os.unlink(temp_name)


def main(args):
    if len(args) == 1 and args[0] == "list":
        for name, user in sorted(read_users().items()):
            print(name, user.get("uuid", ""))
        return 0
    if len(args) == 2 and args[0] == "revoke":
        users = read_users()
        users.pop(args[1], None)
        save_users(users)
        print("Revoked:", args[1])
        return 0
    if len(args) == 3 and args[0] == "add":
        username, uuid = args[1], args[2].lower()
        raw_uuid = uuid.replace("-", "")
        if not re.fullmatch(r"[A-Za-z0-9_-]{3,32}", username) or not re.fullmatch(r"[0-9a-f]{32}", raw_uuid):
            print("Invalid username or Minecraft UUID", file=sys.stderr)
            return 2
        uuid = f"{raw_uuid[:8]}-{raw_uuid[8:12]}-{raw_uuid[12:16]}-{raw_uuid[16:20]}-{raw_uuid[20:]}"
        password = secrets.token_urlsafe(24)
        salt = secrets.token_bytes(16)
        digest = hashlib.scrypt(password.encode("utf-8"), salt=salt, n=16384, r=8, p=1, dklen=32)
        users = read_users()
        users[username] = {"uuid": uuid, "salt": salt.hex(), "hash": digest.hex()}
        save_users(users)
        print("Username:", username)
        print("One-time password:", password)
        return 0
    print("Usage: manage_dev_users.py add USERNAME MINECRAFT_UUID | revoke USERNAME | list", file=sys.stderr)
    return 2


if __name__ == "__main__":
    raise SystemExit(main(sys.argv[1:]))
