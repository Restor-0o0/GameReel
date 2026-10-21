$ErrorActionPreference = "Stop"
python .\scripts\build_steam_alltime.py
python .\scripts\download_images.py
Write-Host "Done: data/games_1000.json + CSV + reels + images"
