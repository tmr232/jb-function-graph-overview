## `jcef_helper.exe`

Those fun little processes tend to leak when debugging the extension (only debugging! It does not happen otherwise!)
if you use the DevTools for the embedded webview.
I [reported the bug](https://youtrack.jetbrains.com/issue/IDEA-364480/Debugging-IntelliJ-plugins-with-JBCefBrowser-devtools-leaks-processes), but I don't know when it'll get fixed.

In the meantime, we can use [this lovely gist](https://gist.github.com/tmr232/a65a96483ecfc2eaf37028e0f0201b8a)
to constantly look for orphaned `jcef_helper.exe` processes and kill them.
If we don't, well, there will be pain.

The easiest way to run the script is `uv run --script <url-to-the-raw-script>`,
or downloading it and running the same locally.
