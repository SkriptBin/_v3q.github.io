import os
import tkinter as tk
from tkinter import filedialog, messagebox


class SKEditorLikeApp:
    def __init__(self, root: tk.Tk) -> None:
        self.root = root
        self.root.title("SKEditor-Like")
        self.root.geometry("1100x760")

        self.current_file: str | None = None
        self._is_dark = True
        self._is_dirty = False

        self._build_ui()
        self._bind_shortcuts()
        self._apply_theme()
        self._on_text_changed()
        self._mark_clean()
        self._update_status()
        self.root.protocol("WM_DELETE_WINDOW", self._on_close)

    def _build_ui(self) -> None:
        self._build_menu()

        frame = tk.Frame(self.root)
        frame.pack(fill=tk.BOTH, expand=True)

        self.line_numbers = tk.Text(
            frame,
            width=5,
            padx=6,
            takefocus=0,
            border=0,
            state=tk.DISABLED,
            wrap=tk.NONE,
        )
        self.line_numbers.pack(side=tk.LEFT, fill=tk.Y)

        text_frame = tk.Frame(frame)
        text_frame.pack(side=tk.LEFT, fill=tk.BOTH, expand=True)

        y_scroll = tk.Scrollbar(text_frame)
        y_scroll.pack(side=tk.RIGHT, fill=tk.Y)
        x_scroll = tk.Scrollbar(text_frame, orient=tk.HORIZONTAL)
        x_scroll.pack(side=tk.BOTTOM, fill=tk.X)

        self.text = tk.Text(
            text_frame,
            undo=True,
            wrap=tk.NONE,
            yscrollcommand=y_scroll.set,
            xscrollcommand=x_scroll.set,
            insertwidth=2,
        )
        self.text.pack(side=tk.LEFT, fill=tk.BOTH, expand=True)

        y_scroll.config(command=self._on_scroll)
        x_scroll.config(command=self.text.xview)

        self.status_var = tk.StringVar(value="Ready")
        self.status = tk.Label(self.root, textvariable=self.status_var, anchor="w", padx=10)
        self.status.pack(fill=tk.X)

        self.text.bind("<KeyRelease>", self._on_text_changed)
        self.text.bind("<ButtonRelease>", self._on_text_changed)
        self.text.bind("<MouseWheel>", self._sync_scroll)
        self.text.bind("<Control-MouseWheel>", lambda _e: "break")

    def _build_menu(self) -> None:
        menu = tk.Menu(self.root)

        file_menu = tk.Menu(menu, tearoff=0)
        file_menu.add_command(label="New", command=self.new_file, accelerator="Ctrl+N")
        file_menu.add_command(label="Open", command=self.open_file, accelerator="Ctrl+O")
        file_menu.add_command(label="Save", command=self.save_file, accelerator="Ctrl+S")
        file_menu.add_command(label="Save As", command=self.save_file_as, accelerator="Ctrl+Shift+S")
        file_menu.add_separator()
        file_menu.add_command(label="Exit", command=self._on_close, accelerator="Ctrl+Q")
        menu.add_cascade(label="File", menu=file_menu)

        edit_menu = tk.Menu(menu, tearoff=0)
        edit_menu.add_command(label="Cut", command=lambda: self.text.event_generate("<<Cut>>"), accelerator="Ctrl+X")
        edit_menu.add_command(label="Copy", command=lambda: self.text.event_generate("<<Copy>>"), accelerator="Ctrl+C")
        edit_menu.add_command(label="Paste", command=lambda: self.text.event_generate("<<Paste>>"), accelerator="Ctrl+V")
        edit_menu.add_separator()
        edit_menu.add_command(label="Find", command=self.find_text, accelerator="Ctrl+F")
        edit_menu.add_command(label="Replace", command=self.replace_text, accelerator="Ctrl+H")
        menu.add_cascade(label="Edit", menu=edit_menu)

        view_menu = tk.Menu(menu, tearoff=0)
        view_menu.add_command(label="Toggle Theme", command=self.toggle_theme)
        menu.add_cascade(label="View", menu=view_menu)

        self.root.config(menu=menu)

    def _bind_shortcuts(self) -> None:
        self.root.bind("<Control-n>", lambda _e: self.new_file())
        self.root.bind("<Control-o>", lambda _e: self.open_file())
        self.root.bind("<Control-s>", lambda _e: self.save_file())
        self.root.bind("<Control-S>", lambda _e: self.save_file_as())
        self.root.bind("<Control-q>", lambda _e: self._on_close())
        self.root.bind("<Control-f>", lambda _e: self.find_text())
        self.root.bind("<Control-h>", lambda _e: self.replace_text())

    def _apply_theme(self) -> None:
        if self._is_dark:
            bg, fg, side, status_bg, status_fg = "#1e1e1e", "#d4d4d4", "#252526", "#007acc", "#ffffff"
        else:
            bg, fg, side, status_bg, status_fg = "#ffffff", "#111111", "#f0f0f0", "#e5e5e5", "#111111"

        self.text.configure(bg=bg, fg=fg, insertbackground=fg)
        self.line_numbers.configure(bg=side, fg="#888")
        self.status.configure(bg=status_bg, fg=status_fg)

    def toggle_theme(self) -> None:
        self._is_dark = not self._is_dark
        self._apply_theme()

    def _on_scroll(self, *args) -> None:
        self.text.yview(*args)
        self.line_numbers.yview(*args)

    def _sync_scroll(self, _event=None) -> None:
        self.line_numbers.yview_moveto(self.text.yview()[0])

    def _mark_dirty(self) -> None:
        self._is_dirty = True

    def _mark_clean(self) -> None:
        self._is_dirty = False

    def _on_text_changed(self, _event=None) -> None:
        self._mark_dirty()
        self._update_line_numbers()
        self._update_status()

    def _update_line_numbers(self) -> None:
        lines = int(self.text.index("end-1c").split(".")[0])
        content = "\n".join(str(i) for i in range(1, lines + 1))

        self.line_numbers.configure(state=tk.NORMAL)
        self.line_numbers.delete("1.0", tk.END)
        self.line_numbers.insert("1.0", content)
        self.line_numbers.configure(state=tk.DISABLED)
        self.line_numbers.yview_moveto(self.text.yview()[0])

    def _update_title(self) -> None:
        filename = os.path.basename(self.current_file) if self.current_file else "Untitled"
        dirty_marker = "*" if self._is_dirty else ""
        self.root.title(f"SKEditor-Like - {filename}{dirty_marker}")

    def _update_status(self) -> None:
        cursor = self.text.index(tk.INSERT)
        line, col = cursor.split(".")
        name = os.path.basename(self.current_file) if self.current_file else "Untitled"
        dirty = " (modified)" if self._is_dirty else ""
        self.status_var.set(f"{name}{dirty} | Ln {line}, Col {int(col) + 1}")
        self._update_title()

    def _prompt_save_if_dirty(self) -> bool:
        if not self._is_dirty:
            return True

        choice = messagebox.askyesnocancel("Unsaved changes", "Save changes before continuing?")
        if choice is None:
            return False
        if choice:
            self.save_file()
            return not self._is_dirty
        return True

    def _on_close(self) -> None:
        if self._prompt_save_if_dirty():
            self.root.destroy()

    def new_file(self) -> None:
        if not self._prompt_save_if_dirty():
            return
        self.text.delete("1.0", tk.END)
        self.current_file = None
        self._mark_clean()
        self._update_line_numbers()
        self._update_status()

    def open_file(self) -> None:
        if not self._prompt_save_if_dirty():
            return

        path = filedialog.askopenfilename(
            filetypes=[
                ("Text files", "*.txt"),
                ("Python", "*.py"),
                ("Markdown", "*.md"),
                ("All files", "*.*"),
            ]
        )
        if not path:
            return

        try:
            with open(path, "r", encoding="utf-8") as f:
                data = f.read()
            self.text.delete("1.0", tk.END)
            self.text.insert("1.0", data)
            self.current_file = path
            self._mark_clean()
            self._update_line_numbers()
            self._update_status()
        except OSError as exc:
            messagebox.showerror("Open error", str(exc))

    def save_file(self) -> None:
        if not self.current_file:
            self.save_file_as()
            return

        try:
            with open(self.current_file, "w", encoding="utf-8") as f:
                f.write(self.text.get("1.0", "end-1c"))
            self._mark_clean()
            self._update_status()
        except OSError as exc:
            messagebox.showerror("Save error", str(exc))

    def save_file_as(self) -> None:
        path = filedialog.asksaveasfilename(
            defaultextension=".txt",
            filetypes=[
                ("Text files", "*.txt"),
                ("Python", "*.py"),
                ("Markdown", "*.md"),
                ("All files", "*.*"),
            ],
        )
        if not path:
            return
        self.current_file = path
        self.save_file()

    def find_text(self) -> None:
        term = self._ask_simple("Find", "Enter text to find:")
        if not term:
            return

        self.text.tag_remove("search", "1.0", tk.END)
        start = "1.0"
        count = 0
        while True:
            pos = self.text.search(term, start, stopindex=tk.END)
            if not pos:
                break
            end = f"{pos}+{len(term)}c"
            self.text.tag_add("search", pos, end)
            start = end
            count += 1

        self.text.tag_config("search", background="#f7d774", foreground="#000")
        self.status_var.set(f"Found {count} match(es)")

    def replace_text(self) -> None:
        find_term = self._ask_simple("Replace", "Find:")
        if find_term is None:
            return
        replace_term = self._ask_simple("Replace", "Replace with:")
        if replace_term is None:
            return

        body = self.text.get("1.0", "end-1c")
        count = body.count(find_term)
        self.text.delete("1.0", tk.END)
        self.text.insert("1.0", body.replace(find_term, replace_term))
        self._mark_dirty()
        self._on_text_changed()
        self.status_var.set(f"Replaced {count} match(es)")

    def _ask_simple(self, title: str, prompt: str):
        dialog = tk.Toplevel(self.root)
        dialog.title(title)
        dialog.geometry("320x120")
        dialog.transient(self.root)
        dialog.grab_set()

        tk.Label(dialog, text=prompt).pack(pady=(12, 6))
        entry = tk.Entry(dialog, width=36)
        entry.pack(pady=4)
        entry.focus_set()

        out = {"value": None}

        def submit() -> None:
            out["value"] = entry.get()
            dialog.destroy()

        def cancel() -> None:
            dialog.destroy()

        btns = tk.Frame(dialog)
        btns.pack(pady=10)
        tk.Button(btns, text="OK", width=10, command=submit).pack(side=tk.LEFT, padx=6)
        tk.Button(btns, text="Cancel", width=10, command=cancel).pack(side=tk.LEFT, padx=6)

        dialog.bind("<Return>", lambda _e: submit())
        dialog.bind("<Escape>", lambda _e: cancel())

        self.root.wait_window(dialog)
        return out["value"]


def main() -> None:
    root = tk.Tk()
    SKEditorLikeApp(root)
    root.mainloop()


if __name__ == "__main__":
    main()
