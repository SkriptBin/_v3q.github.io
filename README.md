# _v3q.github.io

## SKEditor-like Windows `.exe` app

This repo now includes a small desktop editor app in `skeditor_app/` that is designed to feel like a lightweight SKEditor-style text/code editor.

### Features
- Dark/light theme toggle
- Line numbers
- New/Open/Save/Save As
- Find and replace
- Keyboard shortcuts (`Ctrl+N`, `Ctrl+O`, `Ctrl+S`, `Ctrl+F`, etc.)

### Run locally (Python)
```bash
cd skeditor_app
python main.py
```

### Build `.exe` on Windows
Use Command Prompt in `skeditor_app`:
```bat
build_exe.bat
```

Output:
- `dist/SKEditorLike.exe`
