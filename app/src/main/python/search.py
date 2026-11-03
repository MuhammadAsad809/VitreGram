"""Search operations for VitreGram via instagrapi.
Handles user discovery, hashtag search, and media matching.
"""
import logging
from typing import Any, Dict, List

from exceptions_handler import safe_execute
from instagram_client import get_client
from models import serialize_post, serialize_reel, serialize_user

logger = logging.getLogger("vitregram.search")


@safe_execute
def search_users(query: str, count: int = 25) -> List[Dict[str, Any]]:
    """Searches Instagram for user profiles matching query."""
    cl = get_client()
    users = cl.search_users(query=query)
    if count and len(users) > count:
        users = users[:count]
    return [serialize_user(u) for u in users]


@safe_execute
def search_hashtags(query: str) -> List[Dict[str, Any]]:
    """Searches hashtags matching query."""
    cl = get_client()
    tag_clean = query.lstrip("#")
    tags = cl.search_hashtags(query=tag_clean)
    result = []
    for t in tags:
        result.append({
            "id": str(getattr(t, "id", "") or getattr(t, "name", "")),
            "name": getattr(t, "name", ""),
            "mediaCount": getattr(t, "media_count", 0) or 0,
        })
    return result


@safe_execute
def search_media(query: str, count: int = 20) -> List[Dict[str, Any]]:
    """Searches top media/reels for a query (treats as hashtag or explore discovery)."""
    cl = get_client()
    tag_clean = query.lstrip("#").replace(" ", "")
    try:
        medias = cl.hashtag_medias_top(tag_clean, amount=count)
        return [serialize_post(m) for m in medias]
    except Exception as e:
        logger.warning(f"hashtag_medias_top search failed for {query}: {e}")
        return []


@safe_execute
def search_all(query: str) -> Dict[str, Any]:
    """Combined search returning both users and media matching query for VitreGram search screen."""
    cl = get_client()
    users = []
    try:
        raw_users = cl.search_users(query=query)
        users = [serialize_user(u) for u in (raw_users[:20] if raw_users else [])]
    except Exception as e:
        logger.warning(f"Search users exception: {e}")

    media_items = []
    tag_clean = query.lstrip("#").replace(" ", "")
    if tag_clean:
        try:
            raw_media = cl.hashtag_medias_top(tag_clean, amount=12)
            media_items = [serialize_reel(m) for m in raw_media]
        except Exception as e:
            logger.warning(f"Search media exception: {e}")

    return {
        "users": users,
        "media": media_items,
    }
