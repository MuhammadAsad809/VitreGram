"""User-initiated interactions for VitreGram via instagrapi.
Handles likes, unlikes, saves, follows, unfollows, and comment threads.
Strictly adheres to user-initiated actions; no automated mass-actions.
"""
import logging
from typing import Any, Dict, List

from exceptions_handler import safe_execute
from instagram_client import get_client
from models import serialize_comment

logger = logging.getLogger("vitregram.interactions")


@safe_execute
def like_media(media_id: str) -> bool:
    """Likes a post, reel, or video."""
    cl = get_client()
    return cl.media_like(str(media_id))


@safe_execute
def unlike_media(media_id: str) -> bool:
    """Removes a previously given like."""
    cl = get_client()
    return cl.media_unlike(str(media_id))


@safe_execute
def save_media(media_id: str) -> bool:
    """Saves/bookmarks a post to the user's saved collection."""
    cl = get_client()
    return cl.media_save(str(media_id))


@safe_execute
def unsave_media(media_id: str) -> bool:
    """Removes a post from bookmarks."""
    cl = get_client()
    return cl.media_unsave(str(media_id))


@safe_execute
def follow_user(user_id: str) -> bool:
    """Follows a user profile."""
    cl = get_client()
    uid = str(user_id) if user_id.isdigit() else str(cl.user_id_from_username(user_id))
    return cl.user_follow(uid)


@safe_execute
def unfollow_user(user_id: str) -> bool:
    """Unfollows a user profile."""
    cl = get_client()
    uid = str(user_id) if user_id.isdigit() else str(cl.user_id_from_username(user_id))
    return cl.user_unfollow(uid)


@safe_execute
def get_comments(media_id: str, amount: int = 25) -> List[Dict[str, Any]]:
    """Retrieves comments for a specific post."""
    cl = get_client()
    comments = cl.media_comments(str(media_id), amount=amount)
    return [serialize_comment(c, str(media_id)) for c in comments]


@safe_execute
def add_comment(media_id: str, text: str) -> Dict[str, Any]:
    """Posts a new comment to a media item."""
    cl = get_client()
    c = cl.media_comment(str(media_id), text=text)
    return serialize_comment(c, str(media_id))


@safe_execute
def like_comment(comment_id: str) -> bool:
    """Likes a comment."""
    cl = get_client()
    cid = int(comment_id) if comment_id.isdigit() else comment_id
    return cl.comment_like(cid)


@safe_execute
def unlike_comment(comment_id: str) -> bool:
    """Unlikes a comment."""
    cl = get_client()
    cid = int(comment_id) if comment_id.isdigit() else comment_id
    return cl.comment_unlike(cid)
