import os

def get_random_bytes(n: int) -> bytes:
    return os.urandom(n)

__all__ = ["get_random_bytes"]
