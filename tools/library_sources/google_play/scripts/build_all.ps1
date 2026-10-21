$ErrorActionPreference = "Stop"
if (-Not (Test-Path .\data\raw\Google-Playstore.csv)) {
    python -m pip install --disable-pip-version-check kagglehub
    python .\scripts\fetch_dataset.py
}
python .\scripts\build_google_play_top1000.py
python -m pip install --disable-pip-version-check google-play-scraper
# Important: this second pass fetches Store descriptions/genres, caches them and adds many more mechanics/tags.
python .\scripts\enrich_existing.py --online
python .\scripts\download_images.py
Write-Host "Done: enriched data/games_1000.json + CSV + reels + coverage report + images"
