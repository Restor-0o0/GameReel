$ErrorActionPreference = "Stop"
python -m pip install --disable-pip-version-check kagglehub
python .\scripts\fetch_dataset.py
