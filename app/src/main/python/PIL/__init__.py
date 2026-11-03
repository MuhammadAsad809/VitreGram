"""Lightweight PIL stub for Android viewing-only mode.
Ensures modules requiring `from PIL import Image` import successfully.
"""
from PIL import Image

__version__ = "10.0.0"
__all__ = ["Image"]
