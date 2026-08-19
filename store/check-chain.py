"""Check each link in the Play publishing chain and say which are broken."""
import json, os, subprocess, urllib.request
from google.oauth2 import service_account
import google.auth.transport.requests as tr

KEY = os.path.expanduser("~/.kamsiob-secrets/play-service-account.json")
PROJECT = "kamsiob-503213"
DEV = "8331942106234132525"
REPO = "/var/home/Kamsiob/Kamiob Apps/-- Android/Meedwell"

def line(n, name, ok, detail=""):
    print(f"[{'PASS' if ok else 'FAIL'}] {n}. {name}" + (f"\n        {detail}" if detail else ""))

# 2. key exists, outside repo, gitignored
info = json.load(open(KEY))
outside = not os.path.abspath(KEY).startswith(os.path.abspath(REPO))
gi = os.path.join(REPO, ".gitignore")
patterns = open(gi).read() if os.path.exists(gi) else ""
ignored = any(p in patterns for p in ("*.json", "play-service-account", ".kamsiob-secrets"))
line(2, "Service account + local key",
     info["client_email"].startswith("kamsiob@") and outside,
     f"{info['client_email']}\n        key at {KEY} (outside repo: {outside})\n        "
     f"repo .gitignore covers key pattern: {ignored} (key lives outside the repo either way)")

creds = service_account.Credentials.from_service_account_file(
    KEY, scopes=["https://www.googleapis.com/auth/androidpublisher"])
creds.refresh(tr.Request())
TOK = {"Authorization": "Bearer " + creds.token}

def get(url):
    try:
        return True, json.load(urllib.request.urlopen(urllib.request.Request(url, headers=TOK)))
    except Exception as e:
        body = e.read().decode() if hasattr(e, "read") else str(e)
        try:
            return False, json.loads(body)["error"]
        except Exception:
            return False, {"message": body[:200]}

# 1. androidpublisher API enabled in the GCP project
ok, res = get(f"https://androidpublisher.googleapis.com/androidpublisher/v3/applications/com.kamsiob.healthtrail/edits/nonexistent")
api_disabled = (not ok) and "has not been used in project" in json.dumps(res)
line(1, f"Google Play Android Developer API enabled in {PROJECT}",
     not api_disabled,
     "calls are being served, not blocked by service activation" if not api_disabled else json.dumps(res)[:200])

# 3 + 4. linked to Play Console, and permissions
ok, res = get(f"https://androidpublisher.googleapis.com/androidpublisher/v3/developers/{DEV}/users?pageSize=-1")
linked = ok
detail = ""
perms_ok = False
if ok:
    me = info["client_email"].lower()
    for u in res.get("users", []):
        if u.get("email", "").lower() == me:
            roles = u.get("developerAccountPermissions") or []
            grants = u.get("grants") or []
            perms_ok = "CAN_MANAGE_PUBLIC_LISTING_GLOBAL" in roles
            detail = (f"account-level roles include CAN_MANAGE_PUBLIC_LISTING_GLOBAL: "
                      f"{'yes' if perms_ok else 'no'}; "
                      f"CAN_MANAGE_DRAFT_APPS_GLOBAL: {'CAN_MANAGE_DRAFT_APPS_GLOBAL' in roles}; "
                      f"CAN_MANAGE_PUBLIC_APKS_GLOBAL (release to production): "
                      f"{'CAN_MANAGE_PUBLIC_APKS_GLOBAL' in roles}; per-app grants: {len(grants)}")
else:
    detail = json.dumps(res)[:200]
line(3, "GCP project linked to Play Console (Setup > API access)", linked,
     f"developer {DEV} answered a privileged call, so the link exists" if linked else detail)
line(4, "Service account invited with sufficient permissions", perms_ok, detail)

# The actual target.
#
# Probed with edits.insert, not with a GET on an edit. A GET validates the edit
# ID format first and answers "Invalid edit ID" for a package that does not
# exist at all, which reads as success and is not one.
from googleapiclient.discovery import build as _build
_svc = _build("androidpublisher", "v3", credentials=creds, cache_discovery=False)
try:
    _eid = _svc.edits().insert(packageName="com.kamsiob.meedwell", body={}).execute()["id"]
    _svc.edits().delete(packageName="com.kamsiob.meedwell", editId=_eid).execute()
    line("*", "com.kamsiob.meedwell resolves as a package", True, "an edit opened and was discarded")
except Exception as _e:
    line("*", "com.kamsiob.meedwell resolves as a package", False,
         "no package by that name is bound to any app entry yet"
         if "not found" in str(_e) else str(_e)[:160])
