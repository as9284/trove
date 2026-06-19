# Privacy

Trove is built to keep everything on your phone. The short version: your
screenshots and anything Trove reads from them never leave your device, because
Trove has no way to send them anywhere.

## No network, by design

Trove does not request the `INTERNET` permission. It is not a setting you can
toggle; the app simply has no ability to make a network connection. There are no
servers, no accounts, no sign-in, no analytics, no ads, and no third-party SDKs
that phone home.

## What stays on your device

- **Your screenshots.** Trove reads them from your phone's storage to show and
  organize them. It never uploads or copies them off the device.
- **The text inside them.** Reading the text (OCR) happens entirely on your
  phone using a bundled, offline engine. The recognized text is saved to a
  private database on the device so you can search it. It is never sent anywhere.
- **Everything Trove derives.** Categories, detected links and codes, duplicate
  groupings, pins, and your recent searches are all stored locally.

## Permissions Trove asks for

- **Access to your photos/screenshots.** This is what lets Trove find and index
  your screenshots. Trove works best with access to all of them, so search can
  find any screenshot. If you grant access to only a few, Trove will ask for full
  access, because it cannot search what it cannot see.

That is the only sensitive permission. The remaining entries in the manifest are
there so background indexing can run reliably; none of them involve the network.

## Deleting your data

- Cleaning up duplicates moves screenshots to your system Trash, where Android
  lets you restore them for a while before they are removed for good.
- Uninstalling Trove removes its local database, including all recognized text
  and everything it derived.

## Questions

Trove is open source. If something here is unclear, read the code or open the
project and see for yourself.
