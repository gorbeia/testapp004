# Android Contact Linking

## Overview

Each acquaintance can be optionally linked to a contact in the device's Android
Contacts app. When linked, the acquaintance's detail screen shows the contact's
display name and primary phone number.

## User Flows

### Linking a contact

1. Open an acquaintance's detail screen.
2. In the **Linked Contact** row, tap **Link Contact**.
   (When no contact is linked this row is compact; when linked it expands to a full card.)
3. If the app does not yet have `READ_CONTACTS` permission, the OS permission
   dialog is shown. On deny, nothing happens; the acquaintance remains unlinked.
4. On grant (or if already granted), the system contact picker opens.
5. Select a contact. The detail screen updates to show the contact's name and
   primary phone number.

### Opening the linked contact

- Tap **Open Contact** in the Linked Contact card to view the contact in the
  device's Contacts app.

### Calling the linked contact

- Tap **Call** in the Linked Contact card to open the phone dialler with the
  contact's primary phone number pre-filled. Only shown when a primary phone
  number is available.

### Sending a WhatsApp message to the linked contact

- Tap **WhatsApp** in the Linked Contact card. If WhatsApp is installed it opens
  a new chat directly; otherwise the browser opens `https://wa.me/<number>`.
  Only shown when a primary phone number is available. The number is normalised
  to digits only before building the URL.

### Changing a linked contact

- Tap **Change** in the Linked Contact card to re-open the picker and select a
  different contact.

### Unlinking a contact

- Tap **Unlink** in the Linked Contact card. The acquaintance reverts to
  "No contact linked".

### Contact deleted from device

If the linked contact is later removed from the device's Contacts app, the card
shows "Contact unavailable" with an Unlink button.

## Data Model

`Acquaintance.androidContactLookupKey: String?` — the `ContactsContract.Contacts.LOOKUP_KEY`
of the linked contact. `null` means no contact is linked.

## Permissions

`android.permission.READ_CONTACTS` — declared in `AndroidManifest.xml`; requested
at runtime the first time the user taps **Link Contact**.
