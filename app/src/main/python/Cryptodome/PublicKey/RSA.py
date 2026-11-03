class RsaKey:
    def __init__(self, key=None):
        self.key = key

def import_key(extern_key):
    return RsaKey(extern_key)
