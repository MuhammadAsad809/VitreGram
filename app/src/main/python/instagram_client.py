"""Instagram Client Singleton Manager for VitreGram.
Maintains a single lifecycle-controlled instagrapi Client with thread safety,
configured specifically with private_transport='requests' for Android ARM64 compatibility.
"""
import json
import logging
import threading
from typing import Any, Dict, Optional

from instagrapi import Client

logger = logging.getLogger("vitregram.client")


class InstagramClientManager:
    _instance: Optional["InstagramClientManager"] = None
    _lock = threading.RLock()

    def __init__(self):
        self._client: Optional[Client] = None
        self._active_username: Optional[str] = None
        self._init_client()

    @classmethod
    def get_instance(cls) -> "InstagramClientManager":
        with cls._lock:
            if cls._instance is None:
                cls._instance = InstagramClientManager()
            return cls._instance

    def _init_client(self, settings: Optional[Dict[str, Any]] = None):
        """Initializes a new instagrapi Client with Android-compatible HTTP/requests transport."""
        logger.info("Initializing instagrapi Client with private_transport='requests'")
        self._client = Client(
            settings=settings,
            private_transport="requests",
            request_timeout=15,
        )
        # Avoid unexpected file system prompts
        self._client.with_challenge_flow = False

    @property
    def client(self) -> Client:
        with self._lock:
            if self._client is None:
                self._init_client()
            return self._client

    def reset_client(self):
        """Closes existing connections and constructs a fresh client instance."""
        with self._lock:
            if self._client is not None:
                try:
                    self._client.private.close()
                    self._client.public.close()
                except Exception as e:
                    logger.warning(f"Error closing previous client sessions: {e}")
            self._client = None
            self._active_username = None
            self._init_client()

    def dump_settings(self) -> str:
        """Serializes client device ID, cookies, and tokens into a JSON string."""
        with self._lock:
            if not self._client:
                return "{}"
            settings_dict = self._client.dump_settings()
            return json.dumps(settings_dict)

    def load_settings(self, settings_json_str: str) -> bool:
        """Restores client session, cookies, and device identifiers from JSON string."""
        with self._lock:
            if not settings_json_str or settings_json_str == "{}":
                return False
            try:
                settings_dict = json.loads(settings_json_str)
                self._client.set_settings(settings_dict)
                self._client.init()
                logger.info("Instagrapi session settings loaded successfully.")
                return True
            except Exception as e:
                logger.error(f"Failed to load instagrapi settings: {e}")
                return False

    def is_logged_in(self) -> bool:
        """Checks if client has user_id and authorization cookies present."""
        with self._lock:
            if not self._client:
                return False
            return bool(self._client.user_id and "sessionid" in self._client.cookies)


# Convenience accessor
def get_client() -> Client:
    return InstagramClientManager.get_instance().client


def get_manager() -> InstagramClientManager:
    return InstagramClientManager.get_instance()
