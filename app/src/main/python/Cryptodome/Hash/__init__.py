import hashlib

class SHA256:
    @staticmethod
    def new(data=b""):
        return hashlib.sha256(data)

__all__ = ["SHA256"]
