"""Direct Messaging operations for VitreGram via instagrapi.
Handles conversation lists, message histories, and sending messages.
"""
import logging
from typing import Any, Dict, List

from exceptions_handler import safe_execute
from instagram_client import get_client
from models import serialize_message, serialize_user

logger = logging.getLogger("vitregram.messages")


@safe_execute
def get_threads(amount: int = 20) -> List[Dict[str, Any]]:
    """Retrieves direct message conversation threads with participant details and latest message."""
    cl = get_client()
    threads = cl.direct_threads(amount=amount)
    
    result = []
    for th in threads:
        thread_id = str(th.id or th.pk)
        latest_text = ""
        latest_time = 0
        latest_msg_id = ""
        sender_user = None
        
        if th.messages:
            last = th.messages[0]
            latest_text = str(last.text or "")
            latest_msg_id = str(last.id or "")
            latest_time = int(last.timestamp.timestamp() * 1000) if last.timestamp else 0
            # Identify sender
            for u in th.users:
                if str(u.pk) == str(last.user_id):
                    sender_user = serialize_user(u)
                    break
                    
        if not sender_user and th.users:
            sender_user = serialize_user(th.users[0])
        elif not sender_user:
            sender_user = serialize_user(None)

        result.append({
            "id": latest_msg_id or thread_id,
            "conversationId": thread_id,
            "title": th.thread_title or (th.users[0].username if th.users else "Chat"),
            "sender": sender_user,
            "text": latest_text,
            "timestamp": latest_time,
            "isRead": not th.read_state if hasattr(th, "read_state") and th.read_state else True,
            "participants": [serialize_user(u) for u in th.users],
        })
        
    return result


@safe_execute
def get_thread_messages(thread_id: str, amount: int = 20) -> List[Dict[str, Any]]:
    """Retrieves message history for a specific conversation thread."""
    cl = get_client()
    tid = int(thread_id) if thread_id.isdigit() else thread_id
    messages = cl.direct_messages(tid, amount=amount)
    return [serialize_message(m, str(thread_id)) for m in messages]


@safe_execute
def send_message(thread_id: str, text: str) -> Dict[str, Any]:
    """Sends a text message to a conversation thread."""
    cl = get_client()
    tid = int(thread_id) if thread_id.isdigit() else thread_id
    msg = cl.direct_send(text=text, thread_ids=[tid])
    return serialize_message(msg, str(thread_id))


@safe_execute
def mark_thread_seen(thread_id: str) -> bool:
    """Marks a direct message thread as seen."""
    cl = get_client()
    tid = int(thread_id) if thread_id.isdigit() else thread_id
    try:
        return cl.direct_send_seen(tid)
    except Exception as e:
        logger.warning(f"Failed to mark direct thread seen: {e}")
        return False
