@echo off
setlocal

REM Build a Windows .exe using PyInstaller.
python -m pip install --upgrade pip
python -m pip install pyinstaller

pyinstaller --noconfirm --onefile --windowed --name SKEditorLike main.py

echo.
echo Build complete. EXE is in the dist folder.
endlocal
