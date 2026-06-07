# PettiBox Play Store Listing

This listing was prepared from the current app implementation and Google Play metadata guidance. It avoids keyword stuffing, avoids unrelated special characters, and stays within the main Google Play field limits: app title up to 30 characters, short description up to 80 characters, and full description up to 4000 characters.

## ASO Focus

Primary keyword theme: save and find saved content

Secondary keywords:
- bookmark manager
- screenshot search
- OCR search
- offline notes
- PDF search
- link saver
- file organizer
- save from share menu
- collections and tags
- backup and restore

Recommended category: Productivity

Recommended tags to consider in Play Console:
- Productivity
- Tools
- Notes
- File management
- Personal organization

## App Name

PettiBox: Save & Find

Character count: 21 / 30

## Short Description

Save links, screenshots, PDFs and notes offline. Search text inside images.

Character count: 75 / 80

## Full Description

PettiBox is a private, offline-first shelf for everything you want to save now and find later.

Use the Android share menu to save links, screenshots, notes, images, PDFs, documents and files from the apps you already use. PettiBox keeps them organized in one calm place, so useful things do not get lost in bookmarks, chats, downloads or gallery screenshots.

Save from any app

Share into PettiBox from Chrome, YouTube, Instagram, Reddit, WhatsApp, TikTok, X, Gallery, Files and other apps that support Android sharing. You can also add a quick note, paste a link, pick images or save a document directly from PettiBox.

Find saved content fast

Search titles, notes, links, tags, sources and text found inside images or PDFs. PettiBox uses on-device text recognition to index screenshots and documents for offline search. OCR works best with clear English text and can be rescanned from Settings when needed.

Organize your digital life

Create collections for recipes, study notes, work links, shopping ideas, travel plans, inspiration, documents and personal memories. Add tags, mark favorites, pin important saves, archive old items and move saves between collections without clutter.

Built for everyday saving

PettiBox supports links, notes, text, images, screenshots, PDFs and general files. Link previews and YouTube thumbnails make saved links easier to recognize. Multiple image or file attachments can stay together as one save, so related material remains easy to browse.

Reminders when you need them

Add a reminder while saving or later from an item. PettiBox can nudge you about a recipe to cook, a document to review, a video to watch, a shopping idea to revisit or anything else you saved for later.

Private by design

Your saves stay on your device. PettiBox does not require an account, subscription or cloud sync for its core features. Internet access is only used for optional link previews and remote thumbnail loading; saving, organizing, OCR search and browsing are built around local storage.

Backup and restore

Export a PettiBox backup whenever you want. Backups can include your saved items, collections, tags, notes, favorites, archived items and attachments, so you stay in control of your own library.

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

PettiBox helps you collect the useful pieces of your digital life, keep them private, and find them again with fast offline search.

Save now. Find later. Stay organized.

Character count: 2709 / 4000

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
