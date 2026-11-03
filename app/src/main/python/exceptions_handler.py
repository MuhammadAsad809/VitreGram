"""Unified exception handler for VitreGram Instagram Python modules.
Intercepts instagrapi and network errors, returning structured JSON dictionaries
for seamless consumption across the Chaquopy Kotlin bridge.
"""
import functools
import json
import logging
import traceback
from typing import Any, Callable, Dict

from instagrapi.exceptions import (
    BadPassword,
    ChallengeRequired,
    ClientConnectionError,
    ClientError,
    ClientForbiddenError,
    ClientJSONDecodeError,
    ClientNotFoundError,
    ClientThrottledError,
    ClientUnauthorizedError,
    FeedbackRequired,
    LoginRequired,
    MediaUnavailable,
    PleaseWaitFewMinutes,
    PrivateAccount,
    RateLimitError,
    TwoFactorRequired,
    UserNotFound,
)

logger = logging.getLogger("vitregram.exceptions")


def format_success(data: Any = None) -> str:
    return json.dumps({
        "success": True,
        "data": data,
        "error_type": None,
        "message": None,
        "extra": {}
    })


def format_error(error_type: str, message: str, extra: Dict[str, Any] = None) -> str:
    return json.dumps({
        "success": False,
        "data": None,
        "error_type": error_type,
        "message": message,
        "extra": extra or {}
    })


def safe_execute(func: Callable) -> Callable:
    """Decorator catching instagrapi and standard exceptions and formatting structured JSON."""
    @functools.wraps(func)
    def wrapper(*args, **kwargs) -> str:
        try:
            result = func(*args, **kwargs)
            return format_success(result)
        except TwoFactorRequired as e:
            logger.warning(f"2FA Required in {func.__name__}: {e}")
            extra = {}
            if hasattr(e, "two_factor_info"):
                extra = getattr(e, "two_factor_info") or {}
            elif hasattr(e, "last_json"):
                extra = getattr(e, "last_json") or {}
            return format_error("TwoFactorRequired", str(e) or "Two-factor authentication required.", extra)
        except ChallengeRequired as e:
            logger.warning(f"Challenge Required in {func.__name__}: {e}")
            extra = {}
            if hasattr(e, "last_json"):
                extra = getattr(e, "last_json") or {}
            return format_error("ChallengeRequired", str(e) or "Instagram checkpoint challenge required.", extra)
        except BadPassword as e:
            logger.warning(f"BadPassword in {func.__name__}: {e}")
            return format_error("BadPassword", str(e) or "Invalid username or password.")
        except LoginRequired as e:
            logger.warning(f"LoginRequired in {func.__name__}: {e}")
            return format_error("LoginRequired", str(e) or "Session expired or login required.")
        except RateLimitError as e:
            logger.warning(f"RateLimitError in {func.__name__}: {e}")
            return format_error("RateLimitError", str(e) or "Instagram rate limit reached. Please wait.")
        except PleaseWaitFewMinutes as e:
            logger.warning(f"PleaseWaitFewMinutes in {func.__name__}: {e}")
            return format_error("PleaseWaitFewMinutes", str(e) or "Action throttled by Instagram.")
        except FeedbackRequired as e:
            logger.warning(f"FeedbackRequired in {func.__name__}: {e}")
            return format_error("FeedbackRequired", str(e) or "Action blocked by Instagram anti-bot.")
        except UserNotFound as e:
            logger.info(f"UserNotFound in {func.__name__}: {e}")
            return format_error("UserNotFound", str(e) or "User profile not found.")
        except (ClientNotFoundError, MediaUnavailable) as e:
            logger.info(f"MediaUnavailable in {func.__name__}: {e}")
            return format_error("MediaUnavailable", str(e) or "Media not found or unavailable.")
        except PrivateAccount as e:
            logger.info(f"PrivateAccount in {func.__name__}: {e}")
            return format_error("PrivateAccount", str(e) or "Account is private.")
        except ClientConnectionError as e:
            logger.error(f"ConnectionError in {func.__name__}: {e}")
            return format_error("ClientConnectionError", f"Network connectivity error: {e}")
        except ClientError as e:
            logger.error(f"ClientError in {func.__name__}: {e}")
            return format_error(type(e).__name__, str(e) or "Instagram client error.")
        except Exception as e:
            logger.error(f"Unexpected exception in {func.__name__}: {traceback.format_exc()}")
            return format_error("UnexpectedError", f"Internal error: {str(e)}")

    return wrapper
