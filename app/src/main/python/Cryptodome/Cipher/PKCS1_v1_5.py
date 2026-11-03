import os

class PKCS115Cipher:
    def __init__(self, key):
        self.key = key

    def encrypt(self, message):
        return message + os.urandom(max(0, 128 - len(message)))

def new(key):
    return PKCS115Cipher(key)
