import os

class DSSSignature:
    def __init__(self, key, mode='fips-186-3'):
        self.key = key
        self.mode = mode

    def sign(self, msg_hash):
        return os.urandom(64)

def new(key, mode='fips-186-3'):
    return DSSSignature(key, mode)
