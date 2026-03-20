# @Time    : 2023/8/14 11:48
# @Author  : Lan
# @File    : response.py
# @Software: PyCharm
from typing import Generic, Optional, TypeVar

from pydantic import BaseModel

T = TypeVar("T")


class APIResponse(BaseModel, Generic[T]):
    code: int = 200
    message: str = "ok"
    detail: Optional[T] = None
