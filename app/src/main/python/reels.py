"""Reels and vertical video operations for VitreGram via instagrapi.
Handles explore reels, connected reels, and user-specific video clips for Android TV Media3 playback.
"""
import logging
from typing import Any, Dict, List, Optional

from exceptions_handler import safe_execute
from instagram_client import get_client
from models import serialize_reel

logger = logging.getLogger("vitregram.reels")


@safe_execute
def get_reels(amount: int = 15, last_media_pk: Optional[str] = None) -> List[Dict[str, Any]]:
    """Retrieves full-screen Reels for Android TV Leanback vertical video playback."""
    cl = get_client()
    last_pk = int(last_media_pk) if last_media_pk and last_media_pk.isdigit() else 0
    
    # Try explore_reels first (discover)
    reels_list = []
    try:
        medias = cl.explore_reels(amount=amount, last_media_pk=last_pk)
        if medias:
            reels_list = [serialize_reel(m) for m in medias]
    except Exception as e:
        logger.warning(f"explore_reels exception, attempting reels fallback: {e}")

    # Fallback to connected reels if explore_reels is empty
    if not reels_list:
        try:
            medias = cl.reels(amount=amount, last_media_pk=last_pk)
            if medias:
                reels_list = [serialize_reel(m) for m in medias]
        except Exception as e:
            logger.warning(f"connected reels fallback exception: {e}")

    return reels_list


@safe_execute
def get_user_clips(user_id: str, amount: int = 20) -> List[Dict[str, Any]]:
    """Retrieves Reels clips posted by a specific user."""
    cl = get_client()
    uid = str(user_id) if user_id.isdigit() else str(cl.user_id_from_username(user_id))
    clips = cl.user_clips(uid, amount=amount)
    return [serialize_reel(c) for c in clips]


@safe_execute
def mark_reel_seen(media_id: str) -> bool:
    """Marks a reel as seen."""
    cl = get_client()
    try:
        return cl.clip_seen([str(media_id)])
    except Exception as e:
        logger.warning(f"Failed to mark clip seen: {e}")
        return False
