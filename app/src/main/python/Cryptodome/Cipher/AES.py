import os

MODE_GCM = 6

class AesCipher:
    def __init__(self, key, mode=MODE_GCM, nonce=None):
        self.key = key
        self.mode = mode
        self.nonce = nonce or os.urandom(12)

    def update(self, data):
        pass

    def encrypt_and_digest(self, plaintext):
        # Stub returning mock ciphertext and 16-byte auth tag
        return plaintext, os.urandom(16)

def new(key, mode=MODE_GCM, nonce=None):
    return AesCipher(key, mode, nonce)
