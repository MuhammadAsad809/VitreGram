"""Pure Python implementation of Pydantic primitives required by instagrapi.
Bypasses pydantic-core Rust binary extension, enabling 100% Android ARM64
and Chaquopy mobile execution without maturin/Rust compilers.
"""
from typing import Any, Callable, Dict, List, Optional, Union


class ValidationError(Exception):
    pass


class HttpUrl(str):
    pass


class FilePath(str):
    pass


def ConfigDict(**kwargs) -> Dict[str, Any]:
    return kwargs


def Field(default: Any = None, *, default_factory: Optional[Callable] = None, **kwargs) -> Any:
    if default_factory is not None:
        try:
            return default_factory()
        except Exception:
            return []
    return default


def field_validator(*fields: str, mode: str = "after") -> Callable:
    def decorator(fn: Callable) -> Callable:
        return fn
    return decorator


def validator(*fields: str, pre: bool = False, **kwargs) -> Callable:
    def decorator(fn: Callable) -> Callable:
        return fn
    return decorator


class BaseModel:
    model_config: Dict[str, Any] = {}

    def __init__(self, **data: Any):
        cls = self.__class__
        # Resolve class-level defaults
        for base in reversed(cls.__mro__):
            annotations = getattr(base, "__annotations__", {})
            for key in annotations:
                if hasattr(base, key):
                    val = getattr(base, key)
                    if callable(val) and key not in ("dict", "model_dump", "model_copy"):
                        try:
                            val = val()
                        except Exception:
                            pass
                    setattr(self, key, val)
                elif not hasattr(self, key):
                    setattr(self, key, None)
            for key, val in getattr(base, "__dict__", {}).items():
                if not key.startswith("_") and key not in ("model_config", "dict", "model_dump", "model_copy"):
                    if callable(val) and not isinstance(val, type):
                        continue
                    setattr(self, key, val)
        # Apply provided data
        for key, val in data.items():
            setattr(self, key, val)

    def dict(self, *args: Any, **kwargs: Any) -> Dict[str, Any]:
        return self._dump()

    def model_dump(self, *args: Any, **kwargs: Any) -> Dict[str, Any]:
        return self._dump()

    def model_copy(self, *, update: Optional[Dict[str, Any]] = None, deep: bool = False) -> "BaseModel":
        dumped = self._dump()
        if update:
            dumped.update(update)
        return self.__class__(**dumped)

    def _dump(self) -> Dict[str, Any]:
        result = {}
        for k, v in self.__dict__.items():
            if k.startswith("_"):
                continue
            if isinstance(v, BaseModel):
                result[k] = v._dump()
            elif isinstance(v, list):
                result[k] = [item._dump() if isinstance(item, BaseModel) else item for item in v]
            elif isinstance(v, dict):
                result[k] = {dk: dv._dump() if isinstance(dv, BaseModel) else dv for dk, dv in v.items()}
            else:
                result[k] = v
        return result

    def __repr__(self) -> str:
        return f"{self.__class__.__name__}({self.__dict__})"


__all__ = [
    "BaseModel",
    "ConfigDict",
    "Field",
    "FilePath",
    "HttpUrl",
    "ValidationError",
    "field_validator",
    "validator",
]
