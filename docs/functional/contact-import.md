# Contact Import

## Overview

Allows users to create multiple `Acquaintance` records in one flow by selecting from their Android contacts. Each imported person is automatically linked to the source contact.

## Entry Point

Overflow menu (⋮) on the main screen → **Import from contacts**.

## Permission

Requires `READ_CONTACTS`. If not yet granted, the screen shows an explanation and a "Grant permission" button. Granting proceeds directly to the contact list.

## Flow

1. **Contact list**: All device contacts are listed alphabetically.
   - Contacts already linked to an existing person show a ✓ icon and "Already linked" subtitle; they are not selectable.
   - All other contacts have a checkbox. A birthday icon appears if the contact has a birthday set.
2. **Category assignment** (optional): If any categories exist, an expandable card at the top lets the user pick one or more categories. Selected categories are applied to all imported people.
3. **Select all / Deselect all**: Available via top-bar text buttons.
4. **Import**: The "Import N" button at the bottom is enabled when at least one contact is selected. Tapping it creates `Acquaintance` records and navigates back.

## Data mapping

| Contact field | Acquaintance field |
|---|---|
| `DISPLAY_NAME_PRIMARY` | `name` |
| `CommonDataKinds.Event` (TYPE_BIRTHDAY) | `birthday` (ISO-8601 string) |
| `CommonDataKinds.Organization` title + company | `bio` (first line) |
| `CommonDataKinds.Note` | `bio` (appended, newline-separated) |
| `LOOKUP_KEY` | `androidContactLookupKey` |

## Birthday display

Birthday is shown in `AcquaintanceDetailScreen` alongside a cake icon. The format depends on whether the year is stored:
- With year: "January 15, 1990"
- Without year (stored as "--MM-DD"): "January 15"

## Out of scope

- Phone/email import (no structured field in the model)
- Ongoing sync
- Exporting back to contacts
