from enum import Enum

"""
Define authorised terms to define things and states
"""


class DroneStatus(str, Enum):
    IDLE = "IDLE"  # Waiting at base
    RESERVED = "RESERVED"  # Locked for optimization
    MOVING = "MOVING"  # Flying towards a target
    DELIVERING = "DELIVERING"  # Dropping/loading
    CHARGING = "CHARGING"  # Charging
    MAINTENANCE = "MAINTENANCE"  # Out of service


class OrderStatus(str, Enum):
    PENDING = "PENDING"  # Received, not yet processed
    SOLVING = "SOLVING"  # Currently being optimized
    ASSIGNED = "ASSIGNED"  # Mission assigned
    IN_DELIVERY = "IN_DELIVERY"  # Onboard a drone
    DELIVERED = "DELIVERED"  # Success
    CANCELLED = "CANCELLED"  # Aborted


class UrgencyLevel(str, Enum):
    STANDARD = "STANDARD"
    HIGH = "HIGH"  # Blood, Organs -> Absolute priority
    CRITICAL = "CRITICAL"  # Vital prognosis (Takes precedence over everything)


class ActionType(str, Enum):
    FLY_TO = "FLY_TO"  # Move
    PICKUP = "PICKUP"  # Load a package
    DROPOFF = "DROPOFF"  # Deliver a package
    CHARGE = "CHARGE"  # Recharge


class WaypointType(str, Enum):
    DEPOT_START = "DEPOT_START"
    WAREHOUSE_PICKUP = "WAREHOUSE_PICKUP"
    HOSPITAL_DELIVERY = "HOSPITAL_DELIVERY"
    DEPOT_RETURN = "DEPOT_RETURN"
