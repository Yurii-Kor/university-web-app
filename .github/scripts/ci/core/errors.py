class CiError(Exception):
    """Base class for expected CI script errors."""


class UnknownCommandError(CiError):
    def __init__(self, command: str):
        super().__init__(f"Unknown CI command: {command}")