from pydantic_settings import BaseSettings, SettingsConfigDict
from pydantic import Field, ValidationError, field_validator

class Settings(BaseSettings):

    # Environment
    environment: str = 'local'

    # GCP
    project_id: str = Field(..., description="GCP Project ID")
    region: str = Field(default='europe-west1', description="GCP Region")
    
    # Cloud Run
    min_instances: int = Field(default=0, ge=0, le=100)
    
    # Feature Flags
    feature_battery_optimization: bool = False  # convert string "true" to boolean True
    
    # Logging
    log_level: str = Field(default='INFO', pattern='^(DEBUG|INFO|WARNING|ERROR)$')
    
    model_config = SettingsConfigDict(
        env_file='config/dev.env',
        env_file_encoding='utf-8',
        case_sensitive=False,  # to have PROJECT_ID = project_id
        extra='ignore'          # to skip indefined vars
    )
    
    @field_validator('project_id')
    def validate_project_id(cls, v):
        if not v.startswith('drone-fleet-'):
            raise ValueError("PROJECT_ID must start with 'drone-fleet-'")
        return v

# Instantiation (validation automatique)
try:
    settings = Settings()
    print(f"Config loaded: {settings.project_id}")
except ValidationError as e:
    print(f"Config error: {e}")
    exit(1)