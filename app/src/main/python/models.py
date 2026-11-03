"""Data transformation models for VitreGram.
Maps instagrapi Pydantic structures to JSON-serializable dictionaries
aligned with VitreGram's Kotlin domain entities.
"""
from datetime import datetime
from typing import Any, Dict, List, Optional


def _to_timestamp(dt: Any) -> int:
    if isinstance(dt, datetime):
        return int(dt.timestamp() * 1000)
    elif isinstance(dt, (int, float)):
        return int(dt * 1000 if dt < 10000000000 else dt)
    return int(datetime.utcnow().timestamp() * 1000)


def serialize_user(user: Any) -> Dict[str, Any]:
    if not user:
        return {
            "id": "",
            "username": "",
            "fullName": "",
            "profilePictureUrl": "",
            "biography": "",
            "followersCount": 0,
            "followingCount": 0,
            "postsCount": 0,
            "isVerified": False,
            "isFollowing": False,
            "isPrivate": False,
        }
    
    # Handle dict or Pydantic object
    if isinstance(user, dict):
        pk = str(user.get("pk") or user.get("id") or "")
        username = str(user.get("username") or "")
        full_name = str(user.get("full_name") or "")
        profile_pic_url = str(user.get("profile_pic_url") or "")
        biography = str(user.get("biography") or "")
        followers = int(user.get("follower_count") or user.get("followers_count") or 0)
        following = int(user.get("following_count") or 0)
        posts = int(user.get("media_count") or user.get("posts_count") or 0)
        is_verified = bool(user.get("is_verified", False))
        is_private = bool(user.get("is_private", False))
        is_following = bool(user.get("is_following", False))
    else:
        pk = str(getattr(user, "pk", "") or getattr(user, "id", ""))
        username = str(getattr(user, "username", "") or "")
        full_name = str(getattr(user, "full_name", "") or "")
        profile_pic_url = str(getattr(user, "profile_pic_url", "") or "")
        biography = str(getattr(user, "biography", "") or "")
        followers = int(getattr(user, "follower_count", 0) or 0)
        following = int(getattr(user, "following_count", 0) or 0)
        posts = int(getattr(user, "media_count", 0) or 0)
        is_verified = bool(getattr(user, "is_verified", False))
        is_private = bool(getattr(user, "is_private", False))
        is_following = bool(getattr(user, "is_following", False))

    return {
        "id": pk,
        "username": username,
        "fullName": full_name,
        "profilePictureUrl": profile_pic_url,
        "biography": biography,
        "followersCount": followers,
        "followingCount": following,
        "postsCount": posts,
        "isVerified": is_verified,
        "isFollowing": is_following,
        "isPrivate": is_private,
    }


def serialize_media_item(item: Any, media_type_str: str = "IMAGE") -> Dict[str, Any]:
    if isinstance(item, dict):
        url = str(item.get("video_url") or item.get("thumbnail_url") or item.get("url") or "")
        item_id = str(item.get("pk") or item.get("id") or "")
        width = int(item.get("width") or 1080)
        height = int(item.get("height") or 1080)
        duration = float(item.get("video_duration") or 0.0)
    else:
        url = str(getattr(item, "video_url", None) or getattr(item, "thumbnail_url", None) or "")
        item_id = str(getattr(item, "pk", "") or getattr(item, "id", ""))
        width = 1080
        height = 1080
        duration = float(getattr(item, "video_duration", 0.0) or 0.0)
        if hasattr(item, "dimensions") and getattr(item, "dimensions"):
            dims = getattr(item, "dimensions")
            width = getattr(dims, "width", 1080)
            height = getattr(dims, "height", 1080)

    return {
        "id": item_id,
        "url": url,
        "type": media_type_str,
        "width": width,
        "height": height,
        "videoDurationSeconds": duration,
    }


def serialize_post(media: Any) -> Dict[str, Any]:
    if isinstance(media, dict):
        pk = str(media.get("pk") or media.get("id") or "")
        author = serialize_user(media.get("user"))
        caption = str((media.get("caption") or {}).get("text", "") if isinstance(media.get("caption"), dict) else (media.get("caption_text") or ""))
        like_count = int(media.get("like_count") or 0)
        comment_count = int(media.get("comment_count") or 0)
        is_liked = bool(media.get("has_liked", False))
        is_saved = bool(media.get("viewer_has_saved", False))
        created_at = _to_timestamp(media.get("taken_at"))
        media_type = media.get("media_type", 1)
        
        media_items = []
        if media_type == 8 and media.get("resources"):  # Album / Carousel
            for res in media["resources"]:
                m_type = "VIDEO" if res.get("media_type") == 2 or res.get("video_url") else "IMAGE"
                media_items.append(serialize_media_item(res, m_type))
        elif media_type == 2 or media.get("video_url"):  # Video
            media_items.append(serialize_media_item(media, "VIDEO"))
        else:  # Image
            media_items.append(serialize_media_item(media, "IMAGE"))
    else:
        pk = str(getattr(media, "pk", "") or getattr(media, "id", ""))
        author = serialize_user(getattr(media, "user", None))
        caption = str(getattr(media, "caption_text", "") or "")
        like_count = int(getattr(media, "like_count", 0) or 0)
        comment_count = int(getattr(media, "comment_count", 0) or 0)
        is_liked = bool(getattr(media, "has_liked", False))
        is_saved = bool(getattr(media, "viewer_has_saved", False))
        created_at = _to_timestamp(getattr(media, "taken_at", None))
        media_type = getattr(media, "media_type", 1)

        media_items = []
        resources = getattr(media, "resources", [])
        if media_type == 8 and resources:
            for res in resources:
                m_type = "VIDEO" if getattr(res, "video_url", None) else "IMAGE"
                media_items.append(serialize_media_item(res, m_type))
        elif media_type == 2 or getattr(media, "video_url", None):
            media_items.append(serialize_media_item(media, "VIDEO"))
        else:
            media_items.append(serialize_media_item(media, "IMAGE"))

    return {
        "id": pk,
        "author": author,
        "caption": caption,
        "mediaItems": media_items,
        "likeCount": like_count,
        "commentCount": comment_count,
        "isLiked": is_liked,
        "isSaved": is_saved,
        "createdAt": created_at,
    }


def serialize_reel(media: Any) -> Dict[str, Any]:
    if isinstance(media, dict):
        pk = str(media.get("pk") or media.get("id") or "")
        author = serialize_user(media.get("user"))
        video_url = str(media.get("video_url") or "")
        thumb_url = str(media.get("thumbnail_url") or "")
        caption = str((media.get("caption") or {}).get("text", "") if isinstance(media.get("caption"), dict) else (media.get("caption_text") or ""))
        like_count = int(media.get("like_count") or 0)
        comment_count = int(media.get("comment_count") or 0)
        view_count = int(media.get("view_count") or media.get("play_count") or 0)
        is_liked = bool(media.get("has_liked", False))
        is_saved = bool(media.get("viewer_has_saved", False))
        audio_title = "Original Audio"
        audio_artist = author["username"]
        if "clips_music_attribution_info" in media and media["clips_music_attribution_info"]:
            music = media["clips_music_attribution_info"]
            audio_title = music.get("song_name", audio_title)
            audio_artist = music.get("artist_name", audio_artist)
    else:
        pk = str(getattr(media, "pk", "") or getattr(media, "id", ""))
        author = serialize_user(getattr(media, "user", None))
        video_url = str(getattr(media, "video_url", "") or "")
        thumb_url = str(getattr(media, "thumbnail_url", "") or "")
        caption = str(getattr(media, "caption_text", "") or "")
        like_count = int(getattr(media, "like_count", 0) or 0)
        comment_count = int(getattr(media, "comment_count", 0) or 0)
        view_count = int(getattr(media, "view_count", 0) or getattr(media, "play_count", 0) or 0)
        is_liked = bool(getattr(media, "has_liked", False))
        is_saved = bool(getattr(media, "viewer_has_saved", False))
        audio_title = "Original Audio"
        audio_artist = author["username"]
        music_info = getattr(media, "clips_music_attribution_info", None)
        if music_info:
            audio_title = getattr(music_info, "song_name", audio_title) or audio_title
            audio_artist = getattr(music_info, "artist_name", audio_artist) or audio_artist

    return {
        "id": pk,
        "author": author,
        "videoUrl": video_url,
        "thumbnailUrl": thumb_url,
        "caption": caption,
        "audioTitle": audio_title,
        "audioArtist": audio_artist,
        "likeCount": like_count,
        "commentCount": comment_count,
        "shareCount": 0,
        "viewCount": view_count,
        "isLiked": is_liked,
        "isSaved": is_saved,
    }


def serialize_story(story: Any) -> Dict[str, Any]:
    if isinstance(story, dict):
        pk = str(story.get("pk") or story.get("id") or "")
        author = serialize_user(story.get("user"))
        video_url = story.get("video_url")
        media_url = str(video_url or story.get("thumbnail_url") or "")
        is_video = bool(video_url or story.get("media_type") == 2)
        duration = int(story.get("video_duration") or 15)
        created_at = _to_timestamp(story.get("taken_at"))
    else:
        pk = str(getattr(story, "pk", "") or getattr(story, "id", ""))
        author = serialize_user(getattr(story, "user", None))
        video_url = getattr(story, "video_url", None)
        media_url = str(video_url or getattr(story, "thumbnail_url", "") or "")
        is_video = bool(video_url or getattr(story, "media_type", 1) == 2)
        duration = int(getattr(story, "video_duration", 15) or 15)
        created_at = _to_timestamp(getattr(story, "taken_at", None))

    return {
        "id": pk,
        "author": author,
        "mediaUrl": media_url,
        "isVideo": is_video,
        "durationSeconds": duration,
        "createdAt": created_at,
        "isSeen": False,
    }


def serialize_comment(comment: Any, post_id: str = "") -> Dict[str, Any]:
    if isinstance(comment, dict):
        pk = str(comment.get("pk") or comment.get("id") or "")
        author = serialize_user(comment.get("user"))
        text = str(comment.get("text") or "")
        created_at = _to_timestamp(comment.get("created_at") or comment.get("created_at_utc"))
        like_count = int(comment.get("comment_like_count") or 0)
        is_liked = bool(comment.get("has_liked", False))
    else:
        pk = str(getattr(comment, "pk", "") or getattr(comment, "id", ""))
        author = serialize_user(getattr(comment, "user", None))
        text = str(getattr(comment, "text", "") or "")
        created_at = _to_timestamp(getattr(comment, "created_at_utc", None))
        like_count = int(getattr(comment, "comment_like_count", 0) or 0)
        is_liked = bool(getattr(comment, "has_liked", False))

    return {
        "id": pk,
        "postId": post_id,
        "author": author,
        "text": text,
        "createdAt": created_at,
        "likeCount": like_count,
        "isLiked": is_liked,
    }


def serialize_message(msg: Any, thread_id: str = "") -> Dict[str, Any]:
    if isinstance(msg, dict):
        pk = str(msg.get("id") or "")
        sender_id = str(msg.get("user_id") or "")
        text = str(msg.get("text") or "")
        timestamp = _to_timestamp(msg.get("timestamp"))
    else:
        pk = str(getattr(msg, "id", "") or "")
        sender_id = str(getattr(msg, "user_id", "") or "")
        text = str(getattr(msg, "text", "") or "")
        timestamp = _to_timestamp(getattr(msg, "timestamp", None))

    sender = {
        "id": sender_id,
        "username": "",
        "fullName": "",
        "profilePictureUrl": "",
        "biography": "",
        "followersCount": 0,
        "followingCount": 0,
        "postsCount": 0,
        "isVerified": False,
        "isFollowing": False,
        "isPrivate": False,
    }

    return {
        "id": pk,
        "conversationId": thread_id,
        "sender": sender,
        "text": text,
        "timestamp": timestamp,
        "isRead": True,
    }
