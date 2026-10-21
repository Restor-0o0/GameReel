import pathlib, sys, shutil, zipfile
ROOT=pathlib.Path(__file__).resolve().parents[1]
outdir=ROOT/'data'/'raw'; outdir.mkdir(parents=True,exist_ok=True)
target=outdir/'Google-Playstore.csv'
try:
    import kagglehub
except ImportError:
    print('kagglehub is not installed.')
    print('Run: python -m pip install kagglehub')
    print('Then rerun this script, or manually download Google-Playstore.csv from:')
    print('https://www.kaggle.com/datasets/gauthamp10/google-playstore-apps')
    sys.exit(2)
path=pathlib.Path(kagglehub.dataset_download('gauthamp10/google-playstore-apps'))
candidates=list(path.rglob('Google-Playstore.csv')) + list(path.rglob('*.csv'))
if not candidates:
    raise SystemExit(f'No CSV found in {path}')
src=next((p for p in candidates if p.name.lower()=='google-playstore.csv'), candidates[0])
shutil.copy2(src,target)
print('Dataset copied to',target)
