# Accounts, Aliases, Rules & Transaction Edit (V6)

This document is self-contained. It covers everything added in V6 and supersedes any conflicting information in older docs.

---

## Overview

- **Accounts** — normalized per-bank accounts auto-created on import. Each transaction now carries an `account` summary. Dashboard endpoints accept `?accountId=` to filter by account.
- **Merchant aliases** — regex rules that rewrite raw merchant names to canonical form (e.g. `"McDonalds 47"` → `"McDonald's"`). Applied at import time and retroactively.
- **Sparkasse rules** — regex rules that auto-assign a `userCategory` to Sparkasse transactions based on partner name or Verwendungszweck. Applied at import time and retroactively.
- **Retroactive re-categorization** — changing an MCC→category mapping, creating/updating a Sparkasse rule, or creating/updating a merchant alias immediately updates all matching existing transactions where `userCategory` is currently `null`.
- **Transaction edit/delete** — `PATCH /api/transactions/{uuid}` for user-editable fields; `DELETE /api/transactions/{uuid}` for hard delete.

---

## Updated TransactionResponse schema

Every transaction response now includes a nested `account` object:

```json
{
  "id": 123,
  "transactionId": "550e8400-e29b-41d4-a716-446655440000",
  "source": "SPARKASSE",
  "datetime": "2024-11-15T14:23:00+01:00",
  "date": "2024-11-15",
  "accountType": "DEFAULT",
  "category": "CASH",
  "type": "CARD_TRANSACTION",
  "amount": -12.50,
  "fee": null,
  "tax": null,
  "currency": "EUR",
  "description": "Ref 12345",
  "note": null,
  "assetInfo": null,
  "fxInfo": null,
  "counterpartyInfo": null,
  "merchantName": "McDonald's",
  "mccCode": null,
  "userCategory": {
    "id": 1,
    "name": "Lebensmittel",
    "color": "#22c55e",
    "icon": "shopping-cart"
  },
  "account": {
    "id": 2,
    "name": "Sparkasse 4321",
    "color": null,
    "icon": null,
    "source": "SPARKASSE",
    "ownAccountIban": "AT12345678904321"
  },
  "ownAccountIban": "AT12345678904321",
  "ownAccountName": "Max Mustermann",
  "sepaMandateId": null,
  "sepaCreditorId": null,
  "paymentMethod": null
}
```

---

## Accounts API

Accounts are auto-created on first import for each unique `(source, ownAccountIban)` combination. Trade Republic always has one account with `ownAccountIban: null`.

---

### GET /api/accounts

**Response** `200 OK`

```json
[
  {
    "id": 1,
    "name": "Trade Republic",
    "color": null,
    "icon": null,
    "source": "TRADE_REPUBLIC",
    "ownAccountIban": null,
    "createdAt": "2024-01-01T00:00:00Z",
    "updatedAt": "2024-01-01T00:00:00Z"
  },
  {
    "id": 2,
    "name": "Sparkasse 4321",
    "color": "#3b82f6",
    "icon": "bank",
    "source": "SPARKASSE",
    "ownAccountIban": "AT12345678904321",
    "createdAt": "2024-01-01T00:00:00Z",
    "updatedAt": "2024-01-01T00:00:00Z"
  }
]
```

---

### GET /api/accounts/{id}

**Path parameter** `id` — account ID (Long).

**Response** `200 OK` — single account object as above.  
**Response** `404 Not Found` if account does not exist.

---

### PATCH /api/accounts/{id}

Update display metadata. At least one field required.

**Path parameter** `id` — account ID.

**Request**

```json
{
  "name": "Hauptkonto",
  "color": "#3b82f6",
  "icon": "bank"
}
```

All fields optional (omit to leave unchanged). `color` and `icon` can be set to empty string to clear.

**Response** `200 OK` — updated account object.  
**Response** `400 Bad Request` if body has no fields.  
**Response** `404 Not Found` if account does not exist.

> **Note:** `source` and `ownAccountIban` are read-only — they cannot be changed via PATCH.

---

## Merchant Aliases API

Base path: `/api/merchant-aliases`

Aliases rewrite `transactions.merchantName` at import and retroactively. Pattern is a Java regex (`java.util.regex.Pattern`). Use `(?i)` for case-insensitive matching.

**Side-effect:** Creating or updating an alias immediately rewrites `merchantName` on all existing transactions whose current merchant name matches the pattern. The raw original value is lost. Deleting an alias does not revert.

---

### GET /api/merchant-aliases

**Response** `200 OK` — list of alias objects.

```json
[
  {
    "id": 1,
    "pattern": "(?i)^mcdonalds",
    "canonicalName": "McDonald's",
    "createdAt": "2024-11-01T10:00:00Z",
    "updatedAt": "2024-11-01T10:00:00Z"
  }
]
```

---

### GET /api/merchant-aliases/{id}

**Response** `200 OK` or `404 Not Found`.

---

### POST /api/merchant-aliases

**Request**

```json
{
  "pattern": "(?i)^mcdonalds",
  "canonicalName": "McDonald's"
}
```

Both fields required and non-blank. Invalid regex → `400 Bad Request`.

**Response** `201 Created` — created alias object.

---

### PUT /api/merchant-aliases/{id}

Full replacement of pattern and canonicalName.

**Response** `200 OK` or `404 Not Found`.

---

### DELETE /api/merchant-aliases/{id}

**Response** `204 No Content` or `404 Not Found`.

---

## Sparkasse Rules API

Base path: `/api/sparkasse-rules`

Rules match against Sparkasse transactions and assign a `userCategory`. Rules are evaluated in `priority ASC, id ASC` order; first match wins.

**`targetField` values:**
- `PARTNER_NAME` — matches against `merchantName` (card transactions) or `counterparty.name` (transfer transactions)
- `REFERENCE` — matches against `description` (= Verwendungszweck / Buchungsreferenz)
- `BOTH` — matches if either field matches

**Side-effect:** Creating or updating a rule immediately assigns the rule's category to all Sparkasse transactions where `userCategory IS NULL` and the pattern matches. Manual assignments (via `PATCH /api/transactions/{uuid}`) are never overwritten. Deleting a rule does not un-tag previously tagged transactions.

---

### GET /api/sparkasse-rules

**Response** `200 OK` — list ordered by `priority ASC, id ASC`.

```json
[
  {
    "id": 1,
    "pattern": "(?i)billa",
    "targetField": "BOTH",
    "userCategory": {
      "id": 1,
      "name": "Lebensmittel",
      "color": "#22c55e",
      "icon": "shopping-cart"
    },
    "priority": 10,
    "createdAt": "2024-11-01T10:00:00Z",
    "updatedAt": "2024-11-01T10:00:00Z"
  }
]
```

---

### GET /api/sparkasse-rules/{id}

**Response** `200 OK` or `404 Not Found`.

---

### POST /api/sparkasse-rules

**Request**

```json
{
  "pattern": "(?i)billa",
  "targetField": "BOTH",
  "userCategoryId": 1,
  "priority": 10
}
```

`priority` defaults to 100 if omitted. Invalid regex → `400 Bad Request`. Unknown `userCategoryId` → `404 Not Found`.

**Response** `201 Created` — created rule with expanded `userCategory`.

---

### PUT /api/sparkasse-rules/{id}

Full replacement.

**Response** `200 OK` or `404 Not Found`.

---

### DELETE /api/sparkasse-rules/{id}

**Response** `204 No Content` or `404 Not Found`.

---

## Retroactive Re-Categorization (general)

Auto-categorization changes — new MCC→category mapping, new/updated Sparkasse rule, new/updated merchant alias — update only transactions where `userCategory` is currently `null`. Transactions manually tagged via `PATCH /api/transactions/{uuid}` are never overwritten.

**Import-time pipeline order:**
1. Merchant alias — rewrites raw `merchantName` to canonical form.
2. MCC→category mapping (Trade Republic card transactions only).
3. Sparkasse rule — evaluated against canonicalized merchant name (cards) or raw partner name / Verwendungszweck (transfers). Only fires when `userCategory` is still null after step 2.

---

## Transaction Edit

### PATCH /api/transactions/{transactionId}

**Path parameter** `transactionId` — UUID.

Partial update of user-editable fields. Send `null` to clear a field. Bank-sourced fields are read-only.

**Editable fields:**

| Field | Type | Semantics |
|---|---|---|
| `note` | `string \| null` | User note. `null` clears. |
| `description` | `string \| null` | Free-text description override. Omit to leave unchanged; `null` clears only if sent explicitly. |
| `merchantName` | `string \| null` | Override merchant name. Omit to leave unchanged. |
| `mccCode` | `string \| null` | MCC code string (e.g. `"5411"`). Sets linked MCC. |
| `userCategoryId` | `number \| null` | Category ID. `null` clears the category. |
| `category` | enum | Transaction category enum (`CASH`, `SAVINGS`, …). |

**Read-only fields** (ignored if sent): `source`, `datetime`, `date`, `accountType`, `type`, `amount`, `fee`, `tax`, `currency`, `ownAccountIban`, `ownAccountName`, `sepaMandateId`, `sepaCreditorId`, `paymentMethod`, `transactionId`, `id`.

**Request example** (set note, clear userCategory):

```json
{
  "note": "Dinner with client",
  "userCategoryId": null
}
```

**Response** `200 OK` — full updated `TransactionResponse`.  
**Response** `404 Not Found` if UUID not found.  
**Response** `404 Not Found` if `mccCode` or `userCategoryId` reference non-existent records.

---

## Transaction Delete

### DELETE /api/transactions/{transactionId}

Hard delete. Cannot be undone.

**Path parameter** `transactionId` — UUID.

**Response** `204 No Content`.  
**Response** `404 Not Found` if UUID not found.

---

## Dashboard `?accountId=` filter

All four dashboard endpoints accept an optional `accountId` query parameter:

```
GET /api/dashboard/overview?accountId=2
GET /api/dashboard/portfolio?accountId=1
GET /api/dashboard/spending?accountId=2
GET /api/dashboard/income?accountId=2
```

- Omit `accountId` to get combined totals across all accounts.
- Pass a valid account ID to filter to that account only.
- Unknown `accountId` → `404 Not Found`.
- Portfolio with a non-Trade Republic `accountId` returns empty `positions[]` (assets are only held in Trade Republic).

---

## HTTP test files

| File | Coverage |
|---|---|
| `http/accounts.http` | GET all, GET by ID, PATCH |
| `http/aliases.http` | Full CRUD for merchant aliases |
| `http/sparkasse-rules.http` | Full CRUD for Sparkasse rules |
| `http/transactions.http` | Existing — add PATCH and DELETE manually |
