# Rules Update (V7) — Counterparty Matching, TR Card Rules, Asset Rules & MCC Mapping

This document is self-contained and covers everything added in V7. It supersedes any conflicting information in older docs.

---

## Overview of changes

- **`COUNTERPARTY_NAME` target field** added to Sparkasse rules — match against the counterparty's name on SEPA transfers (e.g. gym subscriptions, recurring payments to named accounts).
- **Sparkasse rules now apply to Trade Republic card payments** — the same rule engine previously limited to Sparkasse transactions now also fires for TR card purchases (`CARD_TRANSACTION` / `CARD_TRANSACTION_INTERNATIONAL`). Rules override MCC-derived categories at import time.
- **MCC → category mapping improved** — `GET /api/mcc` and `PUT /api/mcc/{mcc}/category` now include the mapped `userCategory` in their responses. New `GET /api/mcc/{mcc}` endpoint for single-code lookup. New `?mapped=true/false` filter on the list endpoint.
- **New Asset Rules system** — a dedicated rule engine for investment transactions (BUY, SELL, DIVIDEND, etc.). Rules are scoped per asset class and match on the asset's symbol or name. Investment transactions are excluded from Sparkasse rules.

---

## 1. Sparkasse Rule: new `COUNTERPARTY_NAME` target field

The `targetField` enum on Sparkasse rules now has four values:

| Value | Matches against |
|---|---|
| `PARTNER_NAME` | `merchantName` for card transactions; `partnerName` (raw CSV partner) for non-card transactions |
| `COUNTERPARTY_NAME` | `counterparty.name` specifically — only set when a transaction has a linked counterparty (i.e. a SEPA transfer with a known IBAN) |
| `REFERENCE` | `description` (Verwendungszweck / booking reference) |
| `BOTH` | All three fields above — matches if any one of them matches |

**Use case:** A gym subscription is a SEPA direct debit. The counterparty's name (e.g. `"McFit GmbH"`) is stored in `counterparty.name`. Create a rule with `targetField: "COUNTERPARTY_NAME"` and `pattern: "(?i)mcfit"` to auto-categorize it.

**No API changes** — the POST/PUT request and response shapes are identical. Just use the new enum value in `targetField`.

```json
{
  "pattern": "(?i)mcfit",
  "targetField": "COUNTERPARTY_NAME",
  "userCategoryId": 3,
  "priority": 20
}
```

---

## 2. Sparkasse rules now apply to Trade Republic card payments

### What changed

Sparkasse rules now fire for `source = TRADE_REPUBLIC` transactions of type `CARD_TRANSACTION` or `CARD_TRANSACTION_INTERNATIONAL`, in addition to all Sparkasse transactions.

### Matching at import time

For TR card payments the rule is evaluated against:
- `PARTNER_NAME` / `COUNTERPARTY_NAME` / `BOTH` — matched against `merchantName` (same field as Sparkasse card transactions).
- `REFERENCE` / `BOTH` — matched against `description`.

**Priority vs MCC:** if a Sparkasse rule matches a TR card transaction, the rule's `userCategory` is used even if the transaction's MCC code has its own category mapping. The rule wins.

If no rule matches, the MCC-derived category is used as before.

### Retroactive sweep

The retroactive pass that runs when you create or update a Sparkasse rule now also covers TR card transactions where `userCategory IS NULL`. It does **not** overwrite `userCategory` values that were already set (whether by MCC or by a prior rule) — only null slots are filled.

### No API changes

No new endpoints or request/response fields. Rules at `/api/sparkasse-rules` work exactly as before; they now just match a broader set of transactions.

---

## 3. New: Asset Rules

Base path: `/api/asset-rules`

Asset rules auto-assign a `userCategory` to investment transactions based on the asset's symbol or name. Each rule is scoped to a single `assetClass`.

### How it works

- Rules are evaluated when a TR CSV is imported. For any non-card transaction that has a linked asset (`BUY`, `SELL`, `DIVIDEND`, `STOCKPERK`, `MIGRATION`), the asset rules for that asset's class are checked in `priority ASC, id ASC` order; first match wins.
- Investment transactions are **excluded** from Sparkasse rules entirely, regardless of pattern.
- Creating or updating an asset rule triggers a retroactive sweep: all existing TR investment transactions of that asset class where `userCategory IS NULL` are checked and updated if the pattern matches.

### `AssetClass` values (unchanged)

| Value | Meaning |
|---|---|
| `STOCK` | Individual stocks |
| `FUND` | Funds and ETFs (equivalent in this system) |
| `DERIVATIVE` | Options, certificates, etc. |
| `CRYPTO` | Cryptocurrencies |

### `targetField` values

| Value | Matches against |
|---|---|
| `SYMBOL` | `asset.symbol` — e.g. `"AAPL"`, `"LU1681048804"` |
| `NAME` | `asset.name` — e.g. `"Apple Inc."`, `"S&P 500 EUR (Acc)"` |
| `BOTH` | Matches if either symbol or name matches |

---

### `GET /api/asset-rules`

Returns all asset rules, optionally filtered by asset class.

**Query parameters**

| Parameter | Required | Description |
|---|---|---|
| `assetClass` | no | Filter to one class: `STOCK`, `FUND`, `DERIVATIVE`, `CRYPTO` |

**Response** `200 OK` — list ordered by `priority ASC, id ASC`.

```json
[
  {
    "id": 1,
    "pattern": "(?i)^AAPL$",
    "targetField": "SYMBOL",
    "assetClass": "STOCK",
    "userCategory": {
      "id": 5,
      "name": "Tech Stocks",
      "color": "#6366f1",
      "icon": "chart-bar"
    },
    "priority": 10,
    "createdAt": "2025-04-28T10:00:00Z",
    "updatedAt": "2025-04-28T10:00:00Z"
  }
]
```

---

### `GET /api/asset-rules/{id}`

**Response** `200 OK` or `404 Not Found`.

---

### `POST /api/asset-rules`

Create a new asset rule.

**Request**

```json
{
  "pattern": "(?i)^AAPL$",
  "targetField": "SYMBOL",
  "assetClass": "STOCK",
  "userCategoryId": 5,
  "priority": 10
}
```

| Field | Required | Notes |
|---|---|---|
| `pattern` | yes | Java regex. Use `(?i)` for case-insensitive. Invalid regex → `400 Bad Request`. |
| `targetField` | yes | `SYMBOL`, `NAME`, or `BOTH` |
| `assetClass` | yes | `STOCK`, `FUND`, `DERIVATIVE`, or `CRYPTO` |
| `userCategoryId` | yes | Must reference an existing user category. Unknown ID → `404 Not Found`. |
| `priority` | no | Lower number = higher priority. Defaults to `100` if omitted. |

**Response** `201 Created`

```json
{
  "id": 1,
  "pattern": "(?i)^AAPL$",
  "targetField": "SYMBOL",
  "assetClass": "STOCK",
  "userCategory": {
    "id": 5,
    "name": "Tech Stocks",
    "color": "#6366f1",
    "icon": "chart-bar"
  },
  "priority": 10,
  "createdAt": "2025-04-28T10:00:00Z",
  "updatedAt": "2025-04-28T10:00:00Z"
}
```

---

### `PUT /api/asset-rules/{id}`

Full replacement of all fields.

**Request** — same shape as POST.

**Response** `200 OK` with updated rule.  
**Response** `404 Not Found` if rule or `userCategoryId` does not exist.

---

### `DELETE /api/asset-rules/{id}`

**Response** `204 No Content`.  
**Response** `404 Not Found` if rule does not exist.

> Deleting a rule does **not** remove categories previously assigned by it.

---

## Updated import-time categorization pipeline

The full pipeline that runs when any transaction is imported:

### Trade Republic CSV import

| Transaction type | Step 1 | Step 2 | Step 3 |
|---|---|---|---|
| Card (`CARD_TRANSACTION`, `CARD_TRANSACTION_INTERNATIONAL`) | Apply merchant alias (rewrites `merchantName`) | **Sparkasse rules** — match on merchant name / description. Wins over MCC if matched. | Fall back to MCC → category if no rule matched. |
| Investment (`BUY`, `SELL`, `DIVIDEND`, etc. — has `assetClass`) | — | **Asset rules** — match on asset symbol / name within the asset's class. | Leave `userCategory = null` if no rule matched. |
| Other (transfers, interest, etc.) | — | MCC → category if MCC present. | — |

### Sparkasse JSON import (unchanged)

1. Merchant alias (rewrites `merchantName`).
2. Sparkasse rules — match on partner name / counterparty name / reference.

---

## Retroactive re-categorization summary

| Trigger | Transactions updated |
|---|---|
| Create / update MCC → category mapping | TR transactions with that MCC and `userCategory IS NULL` |
| Create / update Sparkasse rule | All Sparkasse transactions + TR card transactions where `userCategory IS NULL` and pattern matches |
| Create / update merchant alias | All transactions where `merchantName` matches (regardless of `userCategory`) |
| Create / update Asset rule | TR investment transactions of that `assetClass` where `userCategory IS NULL` and pattern matches |

All retroactive passes only fill `null` slots — manually assigned categories (via `PATCH /api/transactions/{uuid}`) are never overwritten.

---

## Common patterns / examples

```jsonc
// Categorize a gym SEPA direct debit by counterparty name
{
  "pattern": "(?i)mcfit",
  "targetField": "COUNTERPARTY_NAME",
  "userCategoryId": 7,       // "Fitness"
  "priority": 10
}

// Categorize all Spotify charges on the TR card
{
  "pattern": "(?i)spotify",
  "targetField": "PARTNER_NAME",
  "userCategoryId": 8,       // "Streaming"
  "priority": 20
}

// Asset rule: tag all Apple stock buys
{
  "pattern": "^AAPL$",
  "targetField": "SYMBOL",
  "assetClass": "STOCK",
  "userCategoryId": 5,       // "Tech Stocks"
  "priority": 10
}

// Asset rule: tag the S&P 500 ETF by partial name match
{
  "pattern": "(?i)s&p 500",
  "targetField": "NAME",
  "assetClass": "FUND",
  "userCategoryId": 9,       // "Index ETFs"
  "priority": 10
}

// Asset rule: catch-all for all crypto (lowest priority, broad match)
{
  "pattern": ".*",
  "targetField": "SYMBOL",
  "assetClass": "CRYPTO",
  "userCategoryId": 10,      // "Crypto"
  "priority": 999
}
```

---

## 4. MCC → Category mapping improvements

### What changed

The `MccCodeDto` shape returned by all MCC endpoints now includes a `userCategory` field. Previously it was omitted, making it impossible to build a UI that shows which codes are already mapped.

Three endpoint changes:

---

### `GET /api/mcc`

**New optional query parameter**

| Parameter | Values | Description |
|---|---|---|
| `mapped` | `true` / `false` | Filter to only codes that have (`true`) or don't have (`false`) a category assigned. Omit for all codes. |

**Updated response shape** — each item now includes `userCategory`:

```json
[
  {
    "mcc": "5411",
    "description": "Grocery Stores, Supermarkets",
    "userCategory": {
      "id": 1,
      "name": "Lebensmittel",
      "color": "#22c55e",
      "icon": "shopping-cart"
    }
  },
  {
    "mcc": "5812",
    "description": "Eating Places, Restaurants",
    "userCategory": null
  }
]
```

`userCategory` is `null` when no mapping has been set for that code.

---

### `GET /api/mcc/{mcc}` *(new)*

Look up a single MCC code by its 4-digit code.

**Path parameter:** `mcc` — e.g. `5411`

**Response** `200 OK`

```json
{
  "mcc": "5411",
  "description": "Grocery Stores, Supermarkets",
  "userCategory": {
    "id": 1,
    "name": "Lebensmittel",
    "color": "#22c55e",
    "icon": "shopping-cart"
  }
}
```

**Response** `404 Not Found` if the code is not in the database.

---

### `PUT /api/mcc/{mcc}/category`

**No change to request shape.** The response now includes `userCategory` in the same format as the list above.

```json
{
  "mcc": "5411",
  "description": "Grocery Stores, Supermarkets",
  "userCategory": {
    "id": 1,
    "name": "Lebensmittel",
    "color": "#22c55e",
    "icon": "shopping-cart"
  }
}
```

Set `categoryId: null` in the request body to clear a mapping; `userCategory` will be `null` in the response.

---

### `mccCode` in `TransactionResponse`

The `mccCode` object embedded in a `TransactionResponse` also gains `userCategory`:

```jsonc
{
  // ...
  "mccCode": {
    "mcc": "5411",
    "description": "Grocery Stores, Supermarkets",
    "userCategory": {
      "id": 1,
      "name": "Lebensmittel",
      "color": "#22c55e",
      "icon": "shopping-cart"
    }
  },
  "userCategory": {
    "id": 1,
    "name": "Lebensmittel",
    "color": "#22c55e",
    "icon": "shopping-cart"
  }
  // ...
}
```

Note: `mccCode.userCategory` is the MCC's *default* mapped category. `userCategory` at the top level is the category *actually applied* to this transaction — it may differ if a Sparkasse rule overrode the MCC mapping at import time.

---

## HTTP test files

| File | Coverage |
|---|---|
| `http/sparkasse-rules.http` | Add test case for `COUNTERPARTY_NAME` target field |
| `http/asset-rules.http` | Full CRUD; filter by `?assetClass=`; example patterns per asset class |
| `http/categories.http` | Add `GET /api/mcc/{mcc}` and `GET /api/mcc?mapped=true` test cases |
