# PettiBox Play Store Listing

This listing was prepared from the current app implementation and Google Play metadata guidance. It avoids keyword stuffing, avoids unrelated special characters, and stays within the main Google Play field limits: app title up to 30 characters, short description up to 80 characters, and full description up to 4000 characters.

## ASO Focus

Primary keyword theme: bookmark manager and link saver

Secondary keywords:
- read it later
- Pocket alternative (full description only — never in title or short description)
- save for later
- screenshot search
- OCR search
- offline bookmark manager
- PDF search
- link saver
- save from share menu
- collections and tags
- private bookmarks

Recommended category: Productivity

Recommended tags to consider in Play Console:
- Productivity
- Tools
- Notes
- File management
- Personal organization

## App Name

PettiBox: Bookmark Manager

Character count: 26 / 30

## Short Description

Save links, screenshots and PDFs to read later. Private offline OCR search.

Character count: 76 / 80

## Full Description

Save links, screenshots, PDFs and notes in one private bookmark manager. PettiBox makes anything you save easy to organize and find later—even offline.

Use the Android share menu to send content to PettiBox from the apps you already use. Keep articles, videos, recipes, study material, shopping ideas and documents out of scattered chats, browser bookmarks, downloads and gallery screenshots.

SAVE FOR LATER FROM ANY APP

Share a link, image, screenshot, PDF or file from any Android app that supports sharing. You can also paste a link, write a quick note or select files directly in PettiBox.

FIND SAVED CONTENT FAST

Search titles, notes, links, sources, collections and tags. On-device OCR also makes text inside screenshots, images and PDFs searchable. Search works offline, and OCR scans can be refreshed from Settings.

ORGANIZE BOOKMARKS YOUR WAY

Group saved content into collections for recipes, study, work, shopping, travel or anything else. Add tags, pin important items, mark favorites and archive what you no longer need.

MORE THAN A LINK SAVER

Keep links, notes, text, screenshots, images, PDFs, documents and other files together. Link previews make bookmarks easy to recognize, and multiple attachments can stay together in a single saved item.

A READ IT LATER APP THAT CANNOT LOSE YOUR LIBRARY

Looking for a Pocket alternative? PettiBox keeps your read-later library on your own device, so no service shutdown, account change or sync outage can take it away. Save articles and videos now, read or watch them later, and keep everything searchable offline.

REMINDERS FOR SAVED ITEMS

Add a reminder when you save something or schedule one later. Remember to read an article, watch a video, review a document or revisit a shopping idea.

PRIVATE AND OFFLINE-FIRST

Your library stays on your device. No account or cloud sync is required for core features. PettiBox only uses internet access for optional link previews and remote thumbnails.

BACKUP AND RESTORE

Export a local backup containing saved items, collections, tags, notes, favorites, archived items and attachments. Restore it when you need it.

Great for saving:

- Recipes and meal ideas
- Reddit posts and articles
- Study materials and class notes
- Shopping ideas and wish lists
- Travel plans and places
- Work links and documents
- Screenshots with important text
- PDFs, files and reference material
- Personal memories and inspiration

PettiBox is a private bookmark manager, read it later app, link saver and screenshot organizer built to help you save now and find later.

Character count: 2571 / 4000

## ASO Diagnosis And Launch Plan

The live listing currently has 50+ downloads, no visible rating, and unrelated apps in
the Similar apps section. That suggests Google Play has weak behavioral data and an
unclear category signal for PettiBox. The old title, "Save & Find," does not contain a
recognized category term.

After publishing the revised metadata:

1. Keep the title stable for at least four weeks so Google can learn the new positioning.
2. In Play Console, open Grow users > Store performance > Conversion analysis. Split
   Google Play search from Explore and review results by country and language.
3. Check the Search terms report weekly. Build the next wording test from actual queries,
   not a generic keyword list.
4. Run a Store listing experiment on the short description or first screenshot, one
   variable at a time. Do not test the title, short description and artwork together.
5. Make screenshot 1 communicate the complete promise: "Save anything. Find it offline."
   Screenshot 2 should prove sharing; screenshot 3 should prove OCR search.
6. Localize the listing and screenshot text for each market the app actively supports.
7. Drive a small amount of qualified external traffic to the Play URL with UTM-tagged
   links. ASO improves ranking and conversion, but a new app still needs real installs,
   retention and ratings to develop ranking signals.

## Feature Bullets

- Save links, notes, images, screenshots, PDFs, documents and files.
- Save from the Android share menu or add items inside the app.
- Search titles, notes, links, sources, tags and OCR text.
- On-device OCR for images and PDFs, with adjustable PDF scan limits.
- Organize saves with collections, colors, tags, favorites and pins.
- Add reminders to saved items.
- Archive items before permanent deletion.
- Export and restore local backups with attachments.
- Link previews and YouTube thumbnail previews.
- Offline-first design with no login required.

## Screenshot Captions

1. Save links, screenshots, notes and files from any app.
2. Pick a collection and add tags before saving.
3. Search titles, notes, tags and text inside screenshots.
4. Browse colorful collections for work, study, recipes and travel.
5. Pin favorites and revisit important saves faster.
6. Add reminders for things you want to come back to.
7. Export a private backup and restore your shelf anytime.
8. Keep your saves on your device, with no account required.

## Promo Text Options

Save anything from any app and find it later with offline OCR search.

One private shelf for links, screenshots, notes, PDFs and files.

## What To Avoid In The Store Listing

- Do not say the app never uses internet. The app declares INTERNET for link metadata and remote preview images.
- Do not imply official integrations with Instagram, Reddit, YouTube, WhatsApp, TikTok, X or other brands. PettiBox accepts shared content from apps that support Android sharing.
- Pocket may be named in the full description only, and only as a comparison ("Pocket alternative"). Never put a competitor brand in the app title, short description or screenshots — that violates Play metadata policy.
- Do not promise perfect OCR for every language or every PDF. The current in-app wording says English works best and other languages may be partial.
- Do not mention subscriptions or ads unless the release build and store setup confirm the final monetization state.

## Store Audit Notes

Implementation-supported claims:
- Android share target for text, images, PDFs, generic files, multiple images and selected text.
- Local Room database with FTS search.
- On-device ML Kit OCR for images and rasterized PDF pages.
- Collections, colors, tags, favorites, pins, archive state and reminders.
- Link metadata fetching with YouTube thumbnail fallback.
- Manual backup export and restore, including attachments.
- Automatic local safety-copy flow in Settings.

Permissions to explain if needed:
- Internet: optional link previews and remote thumbnails.
- Notifications: reminder notifications.
- Exact alarms: scheduled reminders, with runtime fallback.
- Boot completed: re-arms pending reminders after reboot.

Data safety positioning:
- No account required for core features.
- Saved content is stored locally on device.
- Link preview fetching may contact the linked website when saving URLs.
- Backup files are user-controlled exports.
