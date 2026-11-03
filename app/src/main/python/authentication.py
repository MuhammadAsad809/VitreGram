"""Authentication operations for VitreGram via instagrapi.
Handles credential login, 2FA submission, session restoration, and logout.
"""
import json
import logging
from typing import Any, Dict, Optional

from exceptions_handler import format_error, format_success, safe_execute
from instagram_client import get_client, get_manager

logger = logging.getLogger("vitregram.auth")


@safe_execute
def login(username: str, password: str, verification_code: str = "") -> Dict[str, Any]:
    """Logs in using username and password, supporting 2FA verification codes."""
    cl = get_client()
    success = cl.login(username=username, password=password, verification_code=verification_code or "")
    
    user_id = str(cl.user_id or "")
    settings_dump = cl.dump_settings()
    
    return {
        "userId": user_id,
        "username": username,
        "sessionToken": cl.cookies.get("sessionid", ""),
        "csrfToken": cl.token or cl.cookies.get("csrftoken", ""),
        "cookies": dict(cl.cookies),
        "settingsJson": json.dumps(settings_dump),
    }


@safe_execute
def login_by_sessionid(sessionid: str) -> Dict[str, Any]:
    """Logs in using an existing valid session ID cookie."""
    cl = get_client()
    cl.login_by_sessionid(sessionid)
    user_id = str(cl.user_id or "")
    
    return {
        "userId": user_id,
        "username": "",
        "sessionToken": sessionid,
        "csrfToken": cl.token or cl.cookies.get("csrftoken", ""),
        "cookies": dict(cl.cookies),
        "settingsJson": json.dumps(cl.dump_settings()),
    }


@safe_execute
def login_with_cookies(cookies_json: str) -> Dict[str, Any]:
    """Restores session from a JSON map of cookies (extracted from WebView or Keystore)."""
    cl = get_client()
    cookies = json.loads(cookies_json) if isinstance(cookies_json, str) else cookies_json
    
    # Configure client cookies
    cl.set_settings({"cookies": cookies})
    cl.init()
    
    user_id = cookies.get("ds_user_id") or ""
    if not user_id and "sessionid" in cookies:
        import re
        match = re.search(r"^\d+", cookies["sessionid"])
        if match:
            user_id = match.group()
            
    if user_id:
        cl.authorization_data = {
            "ds_user_id": str(user_id),
            "sessionid": cookies.get("sessionid", ""),
            "should_use_header_over_cookies": True,
        }
        cl.user_id = str(user_id)
        
    return {
        "userId": str(cl.user_id or user_id),
        "username": "",
        "sessionToken": cookies.get("sessionid", ""),
        "csrfToken": cookies.get("csrftoken", cl.token),
        "cookies": cookies,
        "settingsJson": json.dumps(cl.dump_settings()),
    }


@safe_execute
def restore_session(settings_json: str) -> bool:
    """Restores complete client state from encrypted Keystore settings JSON."""
    return get_manager().load_settings(settings_json)


@safe_execute
def dump_session() -> str:
    """Exports active client settings to JSON string."""
    return get_manager().dump_settings()


@safe_execute
def check_session() -> bool:
    """Verifies whether the current session is valid by making a lightweight request."""
    cl = get_client()
    if not cl.user_id or "sessionid" not in cl.cookies:
        return False
    try:
        # Verify via lightweight user profile or timeline ping
        info = cl.user_info(int(cl.user_id))
        return bool(info and info.pk)
    except Exception as e:
        logger.warning(f"Session check failed: {e}")
        return False


@safe_execute
def logout() -> bool:
    """Terminates session and resets client."""
    manager = get_manager()
    cl = manager.client
    try:
        cl.logout()
    except Exception as e:
        logger.warning(f"Logout exception ignored: {e}")
    finally:
        manager.reset_client()
    return True
