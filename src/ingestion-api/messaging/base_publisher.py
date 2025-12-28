from abc import ABC, abstractmethod
from typing import Any, Dict

class MessagePublisher(ABC):
    """
    Abstract base class for message publishers.
    Defines the interface for sending messages to a broker.
    """

    @abstractmethod
    def publish(self, topic: str, message: Dict[str, Any], **kwargs) -> bool:
        """
        Publishes a message to a specific topic.

        Args:
            topic (str): The destination topic or channel.
            message (Dict[str, Any]): The message payload (usually JSON serializable).
            **kwargs: Additional backend-specific arguments (e.g., ordering_key).

        Returns:
            bool: True if publication was successful (or accepted), False otherwise.
        """
        pass

    @abstractmethod
    def close(self):
        """
        Closes the connection to the message broker.
        """
        pass
