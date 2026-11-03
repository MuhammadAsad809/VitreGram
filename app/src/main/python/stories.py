"""Stories operations for VitreGram via instagrapi.
Handles following reels tray (active story circles), individual user stories, seen markers, and reactions.
"""
import logging
from typing import Any, Dict, List

from exceptions_handler import safe_execute
from instagrapi.extractors import extract_story_v1
from instagram_client import get_client
from models import serialize_story

logger = logging.getLogger("vitregram.stories")


@safe_execute
def get_stories_tray() -> List[Dict[str, Any]]:
    """Retrieves active stories tray from followed users on the home feed."""
    cl = get_client()
    try:
        tray_response = cl.get_reels_tray_feed()
    except Exception as e:
        logger.warning(f"get_reels_tray_feed failed: {e}")
        return []
    
    stories = []
    tray_items = tray_response.get("tray", []) if isinstance(tray_response, dict) else []
    for user_tray in tray_items:
        items = user_tray.get("items", [])
        for item in items:
            if "pk" in item:
                try:
                    story_obj = extract_story_v1(item)
                    stories.append(serialize_story(story_obj))
                except Exception:
                    stories.append(serialize_story(item))
                    
    return stories


@safe_execute
def get_user_stories(user_id: str) -> List[Dict[str, Any]]:
    """Retrieves active stories for a specific user ID."""
    cl = get_client()
    uid = str(user_id) if user_id.isdigit() else str(cl.user_id_from_username(user_id))
    stories = cl.user_stories(uid)
    return [serialize_story(s) for s in stories]


@safe_execute
def mark_story_seen(story_pk: str) -> bool:
    """Marks a story as seen/viewed on Instagram servers."""
    cl = get_client()
    pk = int(story_pk) if story_pk.isdigit() else story_pk
    cl.story_seen([pk])
    return True


@safe_execute
def like_story(story_id: str) -> bool:
    """Likes an active story."""
    cl = get_client()
    return cl.story_like(str(story_id))


@safe_execute
def unlike_story(story_id: str) -> bool:
    """Unlikes a previously liked story."""
    cl = get_client()
    return cl.story_unlike(str(story_id))
