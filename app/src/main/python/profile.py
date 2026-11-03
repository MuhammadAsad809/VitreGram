"""Profile operations for VitreGram via instagrapi.
Fetches user details, biography, avatar, statistics, followers, following, and profile media.
"""
import logging
from typing import Any, Dict, List

from exceptions_handler import safe_execute
from instagram_client import get_client
from models import serialize_post, serialize_user

logger = logging.getLogger("vitregram.profile")


@safe_execute
def get_user_info(identifier: str) -> Dict[str, Any]:
    """Retrieves full user profile by numeric user_id or username string."""
    cl = get_client()
    if identifier.isdigit():
        user = cl.user_info(int(identifier))
    else:
        user = cl.user_info_by_username(identifier)
    return serialize_user(user)


@safe_execute
def get_user_posts(user_id: str, amount: int = 20) -> List[Dict[str, Any]]:
    """Retrieves recent posts for a specific user ID."""
    cl = get_client()
    uid = int(user_id) if user_id.isdigit() else int(cl.user_id_from_username(user_id))
    medias = cl.user_medias(uid, amount=amount)
    return [serialize_post(m) for m in medias]


@safe_execute
def get_followers(user_id: str, amount: int = 20) -> List[Dict[str, Any]]:
    """Retrieves followers for a specific user ID."""
    cl = get_client()
    uid = int(user_id) if user_id.isdigit() else int(cl.user_id_from_username(user_id))
    followers_dict = cl.user_followers(uid, amount=amount)
    return [serialize_user(u) for u in followers_dict.values()]


@safe_execute
def get_following(user_id: str, amount: int = 20) -> List[Dict[str, Any]]:
    """Retrieves users that a specific user ID is following."""
    cl = get_client()
    uid = int(user_id) if user_id.isdigit() else int(cl.user_id_from_username(user_id))
    following_dict = cl.user_following(uid, amount=amount)
    return [serialize_user(u) for u in following_dict.values()]
