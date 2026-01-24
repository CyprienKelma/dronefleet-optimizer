import logging
import sys

import structlog

from .global_config import settings


def setup_logging():
    """
    Configures structured logging using structlog.
    Outputs JSON in production/dev and pretty-printed logs in local environment.
    """
    shared_processors = [
        structlog.contextvars.merge_contextvars,
        structlog.processors.add_log_level,
        structlog.stdlib.add_logger_name,
        structlog.processors.TimeStamper(fmt="iso"),
        structlog.processors.StackInfoRenderer(),
        structlog.processors.format_exc_info,
        structlog.processors.UnicodeDecoder(),
    ]

    if settings.is_local:
        processors = shared_processors + [structlog.dev.ConsoleRenderer()]
    else:
        processors = shared_processors + [
            structlog.stdlib.ProcessorFormatter.wrap_for_formatter,
        ]

    structlog.configure(
        processors=processors,
        context_class=dict,
        logger_factory=structlog.stdlib.LoggerFactory(),
        wrapper_class=structlog.stdlib.BoundLogger,
        cache_logger_on_first_use=True,
    )

    # Standard logging configuration to capture logs from other libraries
    formatter = structlog.stdlib.ProcessorFormatter(
        processor=structlog.processors.JSONRenderer()
        if not settings.is_local
        else structlog.dev.ConsoleRenderer(),
        foreign_pre_chain=shared_processors,
    )

    handler = logging.StreamHandler(sys.stdout)
    handler.setFormatter(formatter)

    # Clear existing handlers to avoid double logging and ensure deterministic behavior
    root_logger = logging.getLogger()
    root_logger.handlers.clear()
    root_logger.addHandler(handler)
    root_logger.setLevel(settings.log_level)
