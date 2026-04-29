# Sparkasse Integration & Custom Categories

This document covers all new endpoints and changes introduced for Sparkasse bank import support, user-defined categories, and cross-source merchant search.

---

## Overview of changes

- All transactions now have a `source` field (`TRADE_REPUBLIC` or `SPARKASSE`)
- A new `userCategory` can be assigned to any transaction, from either source
- MCC codes can be mapped to a user category — new TR imports are auto-categorized on import
- Sparkasse CSV files can be imported via a dedicated endpoint with idempotent re-import support
- A new merchant search endpoint queries across both sources simultaneously

---

## `source` field

Every `TransactionResponse` now includes:

```json
"source": "TRADE_REPUBLIC"
```

or

```json
"source": "SPARKASSE"
```

Existing Trade Republic transactions were backfilled to `TRADE_REPUBLIC` via the V5 migration.

---

## Updated `TransactionResponse` schema

```json
{
  "id": 1,
  "transactionId": "uuid",
  "source": "SPARKASSE",
  "datetime": "2024-01-15T12:00:00Z",
  "date": "2024-01-15",
  "accountType": "DEFAULT",
  "category": "CASH",
  "type": "CARD_TRANSACTION",
  "amount": -12.50,
  "fee": null,
  "tax": null,
  "currency": "EUR",
  "description": "Buchungs-Details text",
  "note": "User note from Sparkasse app",
  "assetInfo": null,
  "fxInfo": null,
  "counterpartyInfo": null,
  "merchantName": "McDonalds 47",
  "mccCode": null,
  "userCategory": {
    "id": 2,
    "name": "Restaurants",
    "color": "#FF5722",
    "icon": "restaurant"
  },
  "ownAccountIban": "AT832032032202757965",
  "ownAccountName": "Girokonto",
  "sepaMandateId": null,
  "sepaCreditorId": null,
  "paymentMethod": "Girocard"
}
```

Fields `ownAccountIban`, `ownAccountName`, `sepaMandateId`, `sepaCreditorId`, `paymentMethod`, `note` are only populated for `SPARKASSE` source transactions.

---

## User Categories

### `POST /api/categories`

Create a new user-defined category.

**Request**
```json
{
  "name": "Lebensmittel",
  "color": "#4CAF50",
  "icon": "shopping-cart"
}
```

- `name` — required, case-insensitively unique
- `color` — optional hex color for UI display
- `icon` — optional icon identifier

**Response** `201 Created`
```json
{ "id": 1, "name": "Lebensmittel", "color": "#4CAF50", "icon": "shopping-cart" }
```

---

### `GET /api/categories`

Returns all user categories.

**Response** `200 OK` — array of category objects.

---

### `GET /api/categories/{id}`

Returns a single category by ID. `404` if not found.

---

### `PUT /api/categories/{id}`

Full update of a category (name, color, icon).

**Request** — same shape as POST. `400` if the new name conflicts with another category.

---

### `DELETE /api/categories/{id}`

Deletes the category. All transactions and MCC codes that referenced it will have `userCategory` set to `null` (no data loss). `404` if not found.

---

## MCC → Category mapping

Mapping an MCC code to a user category means **new TR imports** with that MCC code will be automatically tagged with that category on import. Existing transactions are not retroactively updated.

### `PUT /api/mcc/{mcc}/category`

**Path parameter:** `mcc` — 4-digit MCC code, e.g. `5411`

**Request**
```json
{ "categoryId": 1 }
```

Set `categoryId` to `null` to clear the mapping.

**Response** `200 OK` — the MCC code DTO.

---

## Sparkasse JSON Import

### `POST /api/transactions/import/sparkasse-json`

Imports transactions from a Sparkasse JSON export (array of transaction objects). Re-importing the same file is safe — already-existing transactions are skipped.

**Request** — `multipart/form-data` with field `file` (the JSON file, UTF-8).

The file must contain a JSON **array** of transaction objects matching the Sparkasse export format.

**Response** `200 OK`
```json
{ "total": 120, "imported": 115, "skipped": 5 }
```

- `total` — objects in the JSON array
- `imported` — newly saved transactions
- `skipped` — duplicates already in the database + zero-amount rows

### Skipping zero-amount rows

Entries where `amount.value == 0` are skipped. These are informational closing/summary entries added by the bank (e.g. *Abschlussbuchung per 31.12.2023*) that carry no real cash movement. Duplicate detection (same transaction imported twice) also counts toward `skipped`.

### JSON field mapping

| JSON field | DB field / notes |
|---|---|
| `booking` | `datetime` + `date` (UTC); falls back to `valuation` if absent |
| `partnerName` | `counterparty.name` (transfers) or `merchantName` (cards) |
| `partnerAccount.iban` | `counterparty.iban` |
| `partnerAccount.bic` | not stored |
| `amount.value` + `amount.precision` | `amount` = `value / 10^precision` (e.g. -2606 / 100 = -26.06) |
| `amount.currency` | `currency` |
| `reference` | `description` |
| `referenceNumber` | `paymentReference` (also used as primary UUID key) |
| `note` | `note` |
| `sepaMandateId` | `sepaMandateId` |
| `sepaCreditorId` | `sepaCreditorId` |
| `ownerAccountNumber` | `ownAccountIban` |
| `ownerAccountTitle` | `ownAccountName` |
| `merchantName` | `merchantName` (preferred over `partnerName` for card transactions) |
| `paymentMethod` | `paymentMethod` |
| `virtualCardNumber`, `cardNumber`, `cardBrand`, `cardType` | used for type inference; not stored separately |
| `transactionId` | always null in exports — UUID is generated deterministically |
| `source` | always set to `SPARKASSE` internally |
| `accountType` / `category` | always `DEFAULT` / `CASH` |

### Idempotency / deterministic UUID

Since `transactionId` is null in virtually all Sparkasse exports, a stable UUID is generated per transaction:

- **If `referenceNumber` is present** (bank-issued unique reference): `UUID.nameUUIDFromBytes("SPARKASSE|REF|" + ownerIban + "|" + referenceNumber)`
- **Fallback** (hash of all available stable fields): booking timestamp, amount, partnerIban, partnerName, reference text. The `note` field is excluded because users can edit it in the Sparkasse app between exports.

### Type inference logic

| Condition | `type` assigned |
|---|---|
| `amount < 0` AND any card field set (`virtualCardNumber`, `cardNumber`, `cardBrand`, `cardType`, `merchantName`) | `CARD_TRANSACTION` |
| `amount < 0` (all other debits — SEPA direct debit, bank transfer, cash withdrawal, fees) | `TRANSFER_INSTANT_OUTBOUND` |
| `amount > 0` | `TRANSFER_INBOUND` |
| `amount = 0` or null | **skipped** |

For card transactions, `merchantName` (JSON) is preferred as the stored merchant name; if null, `partnerName` is used. For transfers, `partnerName` becomes the counterparty name linked by `partnerAccount.iban`.

---

## Merchant Search

### `GET /api/transactions/by-merchant?q={query}`

Searches transactions by merchant name across **both sources**. Matches case-insensitively on `merchantName` (Trade Republic card transactions, Sparkasse card transactions) and `counterparty.name` (SEPA transfers from both sources).

**Query parameters**

| Parameter | Default | Description |
|---|---|---|
| `q` | required | Search string, e.g. `mcdonalds` or `billa` |
| `page` | `0` | Page index |
| `size` | `50` | Page size |
| `sort` | — | e.g. `date,desc` |

**Response** `200 OK` — paginated `TransactionResponse`.

> **Note:** Matching is strict substring (ILIKE `%q%`). "McDonalds 47" and "McDonalds 126" are both returned by `?q=mcdonalds`. Merchant deduplication / aliasing is not yet supported.

---

## Assign category to a transaction

### `PATCH /api/transactions/{transactionId}/category`

Manually assigns or removes a user category on any transaction (works for both TR and Sparkasse).

**Request**
```json
{ "categoryId": 2 }
```

Set `categoryId` to `null` to remove the category.

**Response** `200 OK` — full `TransactionResponse` with updated `userCategory`.

---

## HTTP test files

- `http/categories.http` — UserCategory CRUD, MCC mappings, transaction assignment
- `http/transactions.http` — extended with Sparkasse import and merchant search
