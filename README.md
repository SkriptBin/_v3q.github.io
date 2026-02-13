# _v3q.github.io

## SKEditor-like Windows `.exe` app

A lightweight desktop text/code editor is included in `skeditor_app/`.

### What it supports
- Dark/light theme toggle
- Line numbers + status bar
- New / Open / Save / Save As
- Find + replace
- Unsaved-change prompt on open/new/exit
- Shortcuts (`Ctrl+N`, `Ctrl+O`, `Ctrl+S`, `Ctrl+Shift+S`, `Ctrl+F`, `Ctrl+H`)

### Run in Python
```bash
cd skeditor_app
python main.py
```

### Build `.exe` on Windows (from extracted ZIP folder)
Open **Command Prompt** in `skeditor_app` and run:
```bat
build_exe.bat
```

Output executable:
- `skeditor_app\dist\SKEditorLike.exe`
