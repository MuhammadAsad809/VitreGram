"""Lightweight Image stub."""

class Resampling:
    NEAREST = 0
    BOX = 4
    BILINEAR = 2
    HAMMING = 5
    BICUBIC = 3
    LANCZOS = 1

class Image:
    Resampling = Resampling
    
    def __init__(self, size=(1080, 1080), mode="RGB"):
        self.size = size
        self.mode = mode
        self.width = size[0]
        self.height = size[1]

    def resize(self, size, resample=None):
        return Image(size=size, mode=self.mode)

    def crop(self, box):
        return Image(size=(box[2]-box[0], box[3]-box[1]), mode=self.mode)

    def save(self, fp, format=None, **params):
        pass

    def convert(self, mode):
        self.mode = mode
        return self

def open(fp, mode="r"):
    return Image()

def new(mode, size, color=0):
    return Image(size=size, mode=mode)

def fromarray(obj, mode=None):
    return Image()
