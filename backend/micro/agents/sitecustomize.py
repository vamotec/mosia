# sitecustomize.py — put this in project root or venv site-packages

try:
    import grpc
    if not hasattr(grpc, "__version__"):
        # try stdlib importlib.metadata (py3.8+)
        try:
            from importlib.metadata import version, PackageNotFoundError
        except Exception:
            # fallback for older Python / environments
            try:
                from importlib_metadata import version, PackageNotFoundError  # type: ignore
            except Exception:
                version = None
                PackageNotFoundError = Exception

        v = None
        if version is not None:
            try:
                v = version("grpcio")
            except PackageNotFoundError:
                v = None
            except Exception:
                v = None

        if v is None:
            # try pkg_resources if available
            try:
                import pkg_resources
                v = pkg_resources.get_distribution("grpcio").version
            except Exception:
                v = "unknown"

        try:
            grpc.__version__ = v
        except Exception:
            # best effort; do not break startup
            pass
except Exception:
    # ensure any error here does not block app start
    pass