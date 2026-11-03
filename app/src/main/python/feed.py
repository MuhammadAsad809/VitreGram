"""Feed and timeline operations for VitreGram via instagrapi.
Handles primary timeline feed, pagination cursors, and explore topical discoveries.
"""
import logging
from typing import Any, Dict, List, Optional

from exceptions_handler import safe_execute
from instagrapi.extractors import extract_media_v1
from instagram_client import get_client
from models import serialize_post

logger = logging.getLogger("vitregram.feed")


@safe_execute
def get_timeline(max_id: Optional[str] = None) -> Dict[str, Any]:
    """Retrieves timeline feed with pagination cursor, falling back to explore if empty."""
    cl = get_client()
    feed_response = {}
    try:
        feed_response = cl.get_timeline_feed(max_id=max_id, reason="cold_start_fetch" if not max_id else "paginating")
    except Exception as e:
        logger.warning(f"get_timeline_feed failed: {e}. Trying explore fallback.")

    posts = []
    feed_items = feed_response.get("feed_items", []) if isinstance(feed_response, dict) else []
    for item in feed_items:
        media_dict = item.get("media_or_ad") or item.get("media")
        if media_dict and "pk" in media_dict:
            try:
                media_obj = extract_media_v1(media_dict)
                posts.append(serialize_post(media_obj))
            except Exception:
                posts.append(serialize_post(media_dict))

    # If timeline returned no posts, fallback to explore posts so home screen has content
    if not posts and not max_id:
        try:
            explore_posts = get_explore()
            if explore_posts:
                return {
                    "posts": explore_posts,
                    "nextCursor": None,
                }
        except Exception as e:
            logger.warning(f"Explore fallback also failed: {e}")

    next_cursor = feed_response.get("next_max_id") if isinstance(feed_response, dict) else None
    return {
        "posts": posts,
        "nextCursor": next_cursor,
    }


@safe_execute
def get_explore(cursor: Optional[str] = None) -> List[Dict[str, Any]]:
    """Retrieves curated explore/discovery page items."""
    cl = get_client()
    explore_res = cl.explore_page()
    
    posts = []
    # Explore response contains 'sectional_items' with 'layout_content' -> 'fill_items' / 'medias'
    sectional_items = explore_res.get("sectional_items", [])
    for section in sectional_items:
        layout = section.get("layout_content", {})
        # Check medias
        for item in layout.get("medias", []):
            media_dict = item.get("media")
            if media_dict and "pk" in media_dict:
                try:
                    posts.append(serialize_post(extract_media_v1(media_dict)))
                except Exception:
                    posts.append(serialize_post(media_dict))
        # Check fill_items
        for item in layout.get("fill_items", []):
            media_dict = item.get("media")
            if media_dict and "pk" in media_dict:
                try:
                    posts.append(serialize_post(extract_media_v1(media_dict)))
                except Exception:
                    posts.append(serialize_post(media_dict))

    # Fallback to items directly if flat
    for item in explore_res.get("items", []):
        media_dict = item.get("media") or item
        if media_dict and "pk" in media_dict:
            try:
                posts.append(serialize_post(extract_media_v1(media_dict)))
            except Exception:
                posts.append(serialize_post(media_dict))

    return posts
