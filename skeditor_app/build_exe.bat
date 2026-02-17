@echo off
setlocal

REM Build SKEditorLike.exe on Windows using a local virtual environment.
if not exist ".venv\Scripts\python.exe" (
  py -3 -m venv .venv
)

call .venv\Scripts\activate
python -m pip install --upgrade pip
python -m pip install -r requirements.txt

pyinstaller --noconfirm --clean --onefile --windowed --name SKEditorLike main.py

echo.
echo Build complete: dist\SKEditorLike.exe
endlocal
