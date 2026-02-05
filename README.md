# InternalTool

InternalTool is a small desktop app for comparing Excel files with flexible header mapping.

## Using the app

- Pick the feature from the dropdown (currently `excel-compare`).
- Provide the left/right Excel files and optional sheet name.
- Set the comparison mode (union, left-only, right-only, changes).
- Use the header mapping panel: click Scan Headers, select left/right columns, then Add.
- Click the `?` button to see a full description of the feature and its configuration options.

## Install (one-line)

These commands download the app from GitHub Releases and install it. Copy the whole line and paste it into Terminal.

### macOS

```bash
curl -L "https://github.com/ThomasBarth04/InternalTool/releases/download/v1.0.0/InternalTool-1.0.0.dmg" -o /tmp/InternalTool.dmg && hdiutil attach /tmp/InternalTool.dmg -nobrowse && cp -R /Volumes/InternalTool/InternalTool.app /Applications && hdiutil detach /Volumes/InternalTool
```

What this does:
- Downloads the app
- Opens it
- Copies it into your Applications folder
- Closes the installer

After it finishes, open **Applications** and click **InternalTool**.

### Linux

```bash
curl -L "https://github.com/ThomasBarth04/InternalTool/releases/download/v1.0.0/InternalTool-1.0.0-linux.tar.gz" -o /tmp/InternalTool.tar.gz && mkdir -p ~/InternalTool && tar -xzf /tmp/InternalTool.tar.gz -C ~/InternalTool && ~/InternalTool/InternalTool/bin/InternalTool
```

What this does:
- Downloads the app
- Unpacks it into `~/InternalTool`
- Starts the app

Next time, you can start it with:

```bash
~/InternalTool/InternalTool/bin/InternalTool
```

## Need help?

If a command fails, copy the error message and send it to the maintainer.
