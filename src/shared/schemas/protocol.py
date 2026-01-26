from enum import Enum

"""
Define authorised terms to define things and states
"""


class DroneStatus(str, Enum):
    IDLE = "IDLE"  # Waiting at base
    MOVING = "MOVING"  # Flying towards a target
    DELIVERING = "DELIVERING"  # Dropping/loading
    CHARGING = "CHARGING"  # Charging
    MAINTENANCE = "MAINTENANCE"  # Out of service


class UrgencyLevel(str, Enum):
    STANDARD = "STANDARD"
    HIGH = "HIGH"  # Blood, Organs -> Absolute priority
    CRITICAL = "CRITICAL"  # Vital prognosis (Takes precedence over everything)


class ActionType(str, Enum):
    FLY_TO = "FLY_TO"  # Move
    PICKUP = "PICKUP"  # Load a package
    DROPOFF = "DROPOFF"  # Deliver a package
    CHARGE = "CHARGE"  # Recharge
