class EccKey:
    def __init__(self, curve='p256'):
        self.curve = curve

def generate(curve='p256'):
    return EccKey(curve)
