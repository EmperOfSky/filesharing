from apps.base.models import KeyValue
from apps.base.utils import ip_limit
from core.settings import DEFAULT_CONFIG, settings


async def ensure_settings_row() -> None:
    await KeyValue.get_or_create(key="settings", defaults={"value": DEFAULT_CONFIG})


def _sync_ip_limits() -> None:
    ip_limit["error"].minutes = settings.errorMinute
    ip_limit["error"].count = settings.errorCount
    ip_limit["upload"].minutes = settings.uploadMinute
    ip_limit["upload"].count = settings.uploadCount


async def refresh_settings() -> None:
    """从数据库读取最新配置并应用到运行时。"""
    config_record = await KeyValue.filter(key="settings").first()
    settings.user_config = config_record.value if config_record and config_record.value else {}
    _sync_ip_limits()
