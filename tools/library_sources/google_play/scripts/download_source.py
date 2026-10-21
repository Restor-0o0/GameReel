"""Fetch the public historical CSV used by the user's Google Play generator."""
import pathlib
import shutil
import urllib.request
import zipfile

ROOT = pathlib.Path(__file__).resolve().parents[1]
RAW = ROOT / "data" / "raw"
URL = "https://www.kaggle.com/api/v1/datasets/download/gauthamp10/google-playstore-apps"

def main():
    RAW.mkdir(parents=True, exist_ok=True)
    target = RAW / "Google-Playstore.csv"
    if target.exists():
        print("CSV already exists", flush=True)
        return
    archive = RAW / "source.zip"
    if not archive.exists():
        partial = RAW / "source.zip.part"
        request = urllib.request.Request(URL, headers={"User-Agent": "GameReel-Dataset/1.0"})
        with urllib.request.urlopen(request, timeout=60) as response, partial.open("wb") as output:
            total = 0
            threshold = 10 * 1024 * 1024
            while chunk := response.read(1024 * 1024):
                output.write(chunk)
                total += len(chunk)
                if total >= threshold:
                    print(f"Downloaded {total // 1024 // 1024} MiB", flush=True)
                    threshold += 10 * 1024 * 1024
        partial.replace(archive)
    with zipfile.ZipFile(archive) as source:
        matches = [info for info in source.infolist() if pathlib.PurePosixPath(info.filename).name.lower() == "google-playstore.csv"]
        if len(matches) != 1:
            raise RuntimeError("Expected exactly one Google-Playstore.csv in the source archive")
        # Copy only the CSV bytes to a fixed workspace path, never extract arbitrary paths.
        partial_csv = RAW / "Google-Playstore.csv.part"
        with source.open(matches[0]) as incoming, partial_csv.open("wb") as output:
            shutil.copyfileobj(incoming, output)
        partial_csv.replace(target)
    print("Source CSV ready", flush=True)

if __name__ == "__main__":
    main()
