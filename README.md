# InternalTool

InternalTool idea for support, so we can do reduce repetitive tasks (mac and linux)
    
## Using the app
There is not a great UI yet and maybe only i understand how to use it now since i made it ;). But can show and explain + update to make it easier to understand.

- Pick the feature from the dropdown (currently `excel-compare`).
- Provide the left/right Excel files and optional sheet name.
- Set the comparison mode (union, left-only, right-only, changes). If using changes the idea is that the left Excel file is the old contact list and the right is the new contact list
- Use the header mapping panel: click Scan Headers, select left/right columns, then Add. This makes it so we can specify which fields to compare and it makes it so inconsistencies in the Excel headers dosnt matter.
- So if one has a field called E-Mail and the other has epost, we can just map E-mail -> epost and it will understand that it should compare those fields.
- Click the `?` button to see a full description of the feature and its configuration options. (needs a lot of improvement for the description)

## Install (one-line)

These commands download the app from GitHub Releases and install it. Copy the whole line and paste it into Terminal.
You have to be logged in as admin to be able to install from a link, but when you have installed it from admin you should be able to open from standard user
### macOS

```bash
TAG=$(curl -fsSL https://api.github.com/repos/ThomasBarth04/InternalTool/releases/latest | grep -m1 '"tag_name":' | sed -E 's/.*"([^"]+)".*/\1/') && VER=${TAG#v} && curl -L "https://github.com/ThomasBarth04/InternalTool/releases/download/${TAG}/InternalTool-${VER}.dmg" -o /tmp/InternalTool.dmg && hdiutil attach /tmp/InternalTool.dmg -nobrowse && cp -R /Volumes/InternalTool/InternalTool.app /Applications && hdiutil detach /Volumes/InternalTool
```

What this does:
- Downloads the app
- Opens it
- Copies it into your Applications folder
- Closes the installer

After it finishes, open **Applications** and click **InternalTool**.

### Linux

```bash
TAG=$(curl -fsSL https://api.github.com/repos/ThomasBarth04/InternalTool/releases/latest | grep -m1 '"tag_name":' | sed -E 's/.*"([^"]+)".*/\1/') && VER=${TAG#v} && curl -L "https://github.com/ThomasBarth04/InternalTool/releases/download/${TAG}/InternalTool-${VER}-linux.tar.gz" -o /tmp/InternalTool.tar.gz && mkdir -p ~/InternalTool && tar -xzf /tmp/InternalTool.tar.gz -C ~/InternalTool && ~/InternalTool/InternalTool/bin/InternalTool
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

If a command fails, copy the error message and send it to Thomas.
