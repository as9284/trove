# Trove brand

> A private, offline screenshot organizer with text search. Editorial-minimal identity.

## Logo
Concept: **crop brackets + gem**, the screenshot crop corners (capture) framing a gem (trove/treasure).

| File | Use |
|---|---|
| `trove-icon.svg` | Full composed icon (paper squircle + mark): previews, web, listings |
| `trove-icon-foreground.svg` | Android adaptive icon **foreground** layer (transparent) |
| `trove-icon-monochrome.svg` | Android 13+ **themed** (monochrome) icon layer |
| `trove-wordmark.svg` | Horizontal mark + wordmark + tagline lockup |
| `trove-feature-graphic.svg` | 1024×500 feature / banner graphic |

Adaptive icon **background** layer is a flat color, no file needed: `#F2ECDF`.

## Palette
| Token | Hex | Use |
|---|---|---|
| Ink | `#1B1A17` | Primary marks, text, brackets |
| Paper | `#F2ECDF` | Backgrounds, icon background layer |
| Brass | `#C0892E` | Accent **only**: the gem, search-match highlights, primary action. Use sparingly. |

In-app: derive light/dark surfaces from ink + paper. Brass is a spice, never a fill.

## Type
- **Wordmark / editorial headings:** Fraunces (SIL OFL, open license). Weight 500. Outline the wordmark to paths for final assets so it renders without the font installed.
- **UI / body:** system default (Roboto / Material 3). Keep two weights: regular + medium.

## Do / Don't
- Do keep generous whitespace and let the mark breathe (it lives in the central safe zone).
- Do reduce to one color cleanly (the monochrome layer proves it).
- Don't add gradients, shadows, or a second accent.
- Don't recolor the gem to anything but brass.

## Tagline
"your screenshots, found"
