$ErrorActionPreference='Stop'
python .\scripts\build_top1000.py
python .\scripts\download_images.py
Write-Host "Done. Dataset: data/games_1000.json, images: images/"
