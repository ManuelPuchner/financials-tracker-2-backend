# Financials Tracker — Backend Reference for Frontend Development

## Overview

This is a Spring Boot 3.4 REST API for managing **Trade Republic** brokerage account transactions. It stores trades, cash movements, dividends, card transactions, and more. The primary use case for the admin dashboard is:

- Viewing and filtering all transactions
- Manually adding a single transaction
- Bulk-importing transactions from a Trade Republic CSV export
- Browsing normalised assets and counterparties

---

## Base URL

```
http://localhost:8080
```

No authentication is implemented. All endpoints are open.

---

## Pagination

All list endpoints return a Spring `Page` object and support standard pagination query parameters:

| Parameter | Default | Description |
|-----------|---------|-------------|
| `page` | `0` | Zero-based page index |
| `size` | `50` | Items per page |
| `sort` | — | e.g. `date,desc` or `amount,asc` |

### Page response envelope

```json
{
  "content": [ ...items... ],
  "page": {
    "size": 50,
    "number": 0,
    "totalElements": 312,
    "totalPages": 7
  }
}
```

---

## Endpoints

### Transactions

#### `GET /api/transactions`
Returns a paginated list of all transactions.

**Query params:** `page`, `size`, `sort`

**Response:** `Page<TransactionResponse>`

---

#### `GET /api/transactions/{transactionId}`
Returns a single transaction by its Trade Republic UUID.

**Path param:** `transactionId` — UUID

**Response:** `TransactionResponse`

**Errors:**
- `404` — transaction not found

---

#### `GET /api/transactions/by-category/{category}`
Filter by category.

**Path param:** `category` — one of `TRADING`, `CASH`, `DELIVERY`

**Response:** `Page<TransactionResponse>`

---

#### `GET /api/transactions/by-type/{type}`
Filter by transaction type.

**Path param:** `type` — see `TransactionType` enum below

**Response:** `Page<TransactionResponse>`

---

#### `GET /api/transactions/by-date`
Filter by date range (inclusive).

**Query params:**
| Param | Format | Required |
|-------|--------|----------|
| `from` | `yyyy-MM-dd` | yes |
| `to` | `yyyy-MM-dd` | yes |

**Response:** `Page<TransactionResponse>`

---

#### `POST /api/transactions`
Create a single transaction manually.

**Request body:** `TransactionRequest` (JSON)

**Response:** `201 Created` with `TransactionResponse`

**Errors:**
- `400` — validation failure or duplicate `transactionId`

---

#### `POST /api/transactions/import/csv`
Bulk-import transactions from a Trade Republic CSV export file. Duplicates (matched by `transactionId`) are silently skipped.

**Request:** `multipart/form-data`, field name `file`

**Response:** `200 OK`
```json
{
  "total": 168,
  "imported": 45,
  "skipped": 123
}
```

---

## Data Schemas

### `TransactionResponse`

Returned by all read endpoints.

```jsonc
{
  "id": 1,                                        // internal DB id (Long)
  "transactionId": "ed915aa4-c463-4b3d-bafa-92b54245d5e4",  // Trade Republic UUID
  "datetime": "2024-02-15T13:07:55.080Z",         // ISO-8601 UTC timestamp
  "date": "2024-02-15",                           // yyyy-MM-dd
  "accountType": "DEFAULT",
  "category": "TRADING",
  "type": "BUY",
  "amount": -30.00,                               // negative = outflow, positive = inflow
  "fee": -1.00,                                   // nullable
  "tax": null,                                    // nullable
  "currency": "EUR",
  "description": null,                            // nullable free text

  // Present for TRADING, DIVIDEND, STOCKPERK, MIGRATION — null otherwise
  "assetInfo": {
    "assetId": 7,                                 // FK to assets table
    "symbol": "LU1681048804",
    "name": "S&P 500 EUR (Acc)",
    "assetClass": "FUND",
    "shares": 0.3309210000,                       // transaction-specific quantity
    "price": 90.656000                            // transaction-specific price
  },

  // Present when payment was in a foreign currency — null otherwise
  "fxInfo": {
    "originalAmount": 0.01,
    "originalCurrency": "USD",
    "fxRate": 1.0802
  },

  // Present for CUSTOMER_INBOUND, TRANSFER_INBOUND, etc. — null otherwise
  "counterpartyInfo": {
    "counterpartyId": 1,
    "iban": "AT832032032202757965",
    "name": "Manuel Puchner",
    "paymentReference": null
  },

  // Present for CARD_TRANSACTION / CARD_TRANSACTION_INTERNATIONAL — null otherwise
  "merchantName": "McDonalds 126",
  "mccCode": "5812"
}
```

---

### `TransactionRequest`

Used for `POST /api/transactions`. Send as JSON.

```jsonc
{
  // Required fields
  "transactionId": "ed915aa4-c463-4b3d-bafa-92b54245d5e4",  // must be the TR UUID — no auto-generation
  "datetime": "2024-02-15T13:07:55.080Z",
  "date": "2024-02-15",
  "accountType": "DEFAULT",
  "category": "TRADING",
  "type": "BUY",
  "amount": -30.00,
  "currency": "EUR",

  // Optional core fields
  "fee": -1.00,
  "tax": null,
  "description": null,

  // Asset fields — provide when category=TRADING or type=DIVIDEND/STOCKPERK/MIGRATION
  "assetSymbol": "LU1681048804",
  "assetName": "S&P 500 EUR (Acc)",   // only needed if asset is new; existing symbol reuses DB record
  "assetClass": "FUND",               // only needed if asset is new
  "shares": 0.330921,
  "price": 90.656,

  // Counterparty fields — provide for CUSTOMER_INBOUND etc.
  "counterpartyIban": "AT832032032202757965",
  "counterpartyName": "Manuel Puchner",  // only needed if counterparty is new
  "paymentReference": null,

  // Card transaction fields
  "merchantName": null,
  "mccCode": null,

  // FX fields — provide when transaction was in a foreign currency
  "fxOriginalAmount": null,
  "fxOriginalCurrency": null,
  "fxRate": null
}
```

**Asset / Counterparty upsert behaviour:** if `assetSymbol` already exists in the database, the existing asset is reused and `assetName`/`assetClass` are ignored. Same for `counterpartyIban`.

---

## Enums

### `AccountType`
| Value | Notes |
|-------|-------|
| `DEFAULT` | Only value currently used by Trade Republic |

### `Category`
| Value | Notes |
|-------|-------|
| `TRADING` | Buy / sell orders |
| `CASH` | All cash movements |
| `DELIVERY` | Portfolio migration transfers |

### `TransactionType`
| Value | Category | Description |
|-------|----------|-------------|
| `BUY` | TRADING | Purchase of an asset |
| `SELL` | TRADING | Sale of an asset |
| `CUSTOMER_INPAYMENT` | CASH | Top-up via card / Apple Pay |
| `CUSTOMER_INBOUND` | CASH | Incoming SEPA transfer |
| `STOCKPERK` | CASH | Free stock reward |
| `INTEREST_PAYMENT` | CASH | Interest on cash balance |
| `DIVIDEND` | CASH | Dividend payment |
| `CARD_TRANSACTION` | CASH | TR card purchase (EUR) |
| `CARD_TRANSACTION_INTERNATIONAL` | CASH | TR card purchase (foreign currency) |
| `MIGRATION` | DELIVERY | Portfolio migration in/out |
| `TRANSFER_INBOUND` | CASH | Incoming instant/standard transfer |
| `TRANSFER_INSTANT_INBOUND` | CASH | Incoming instant transfer |
| `TRANSFER_INSTANT_OUTBOUND` | CASH | Outgoing instant transfer |
| `BENEFITS_SAVEBACK` | CASH | Saveback cashback |
| `TAX_OPTIMIZATION` | CASH | Tax optimisation adjustment |
| `BONUS` | CASH | Bonus payment |
| `EARNINGS` | CASH | Other earnings |

### `AssetClass`
| Value |
|-------|
| `FUND` |
| `STOCK` |
| `DERIVATIVE` |
| `CRYPTO` |

---

## Error Responses

All errors follow the RFC 9457 Problem Detail format (Spring default):

```json
{
  "type": "about:blank",
  "title": "Not Found",
  "status": 404,
  "detail": "Transaction not found: ed915aa4-c463-4b3d-bafa-92b54245d5e4",
  "instance": "/api/transactions/ed915aa4-c463-4b3d-bafa-92b54245d5e4"
}
```

| HTTP Status | Trigger |
|-------------|---------|
| `400 Bad Request` | Missing required fields, invalid enum value, duplicate `transactionId` |
| `404 Not Found` | Transaction UUID not found |

---

## Database Structure (3NF)

Three tables. Assets and counterparties are normalised out of transactions.

```
assets
  id          BIGSERIAL PK
  symbol      VARCHAR(50)  UNIQUE NOT NULL   -- e.g. "LU1681048804"
  name        VARCHAR(255) NOT NULL          -- e.g. "S&P 500 EUR (Acc)"
  asset_class VARCHAR(20)  NOT NULL          -- FUND | STOCK | DERIVATIVE | CRYPTO

counterparties
  id   BIGSERIAL PK
  iban VARCHAR(34) UNIQUE NOT NULL
  name VARCHAR(255)

transactions
  id                   BIGSERIAL PK
  transaction_id       UUID        UNIQUE NOT NULL    -- Trade Republic's ID
  transaction_datetime TIMESTAMPTZ NOT NULL
  date                 DATE        NOT NULL
  account_type         VARCHAR(20) NOT NULL
  category             VARCHAR(20) NOT NULL
  type                 VARCHAR(40) NOT NULL
  amount               NUMERIC(20,6) NOT NULL
  fee                  NUMERIC(20,6)
  tax                  NUMERIC(20,6)
  currency             VARCHAR(3)  NOT NULL
  description          TEXT
  asset_id             BIGINT FK → assets(id)
  asset_shares         NUMERIC(20,10)
  asset_price          NUMERIC(20,6)
  counterparty_id      BIGINT FK → counterparties(id)
  payment_reference    TEXT
  merchant_name        VARCHAR(255)           -- CARD_TRANSACTION* only
  fx_original_amount   NUMERIC(20,6)
  fx_original_currency VARCHAR(3)
  fx_rate              NUMERIC(20,10)
  mcc_code             VARCHAR(4)             -- CARD_TRANSACTION* only
```

---

## CSV Import Format

The CSV import endpoint accepts the exact export format from Trade Republic. Column order is fixed:

```
datetime, date, account_type, category, type, asset_class, name, symbol,
shares, price, amount, fee, tax, currency, original_amount, original_currency,
fx_rate, description, transaction_id, counterparty_name, counterparty_iban,
payment_reference, mcc_code
```

- First row is a header and is skipped
- All fields are quoted strings
- Empty string `""` means null/not applicable
- Duplicate rows (matched by `transaction_id`) are skipped without error

---

## Suggested Dashboard Views

| View | Data source |
|------|-------------|
| Transaction list table with filters | `GET /api/transactions` + filter endpoints |
| Transaction detail modal/page | `GET /api/transactions/{transactionId}` |
| Add transaction form | `POST /api/transactions` |
| CSV import screen with result summary | `POST /api/transactions/import/csv` |
| Filter by category tabs | `GET /api/transactions/by-category/{category}` |
| Filter by type dropdown | `GET /api/transactions/by-type/{type}` |
| Date range picker | `GET /api/transactions/by-date?from=&to=` |

### Useful derived metrics (compute on frontend from paginated data or add backend endpoints later)

- Total portfolio value (sum of TRADING amounts)
- Cash balance (sum of CASH amounts)
- Total fees paid
- P&L per asset (group by `assetInfo.symbol`, sum `amount`)
- Monthly cash flow chart (group by `date` month, sum `amount`)
- Card spending by merchant or MCC category

---

## Technical Notes

- All timestamps in responses are UTC ISO-8601 strings (`2024-02-15T13:07:55.080Z`)
- All dates are `yyyy-MM-dd` strings
- All monetary values are decimal numbers (not strings) — use a decimal library, not float
- `amount` is negative for outflows (buys, fees, card payments) and positive for inflows (sells, dividends, transfers in)
- `fee` and `tax` are always negative when present
- The `id` field is an internal auto-increment Long; use `transactionId` (UUID) as the stable business key in URLs and links
- CORS is not configured — proxy API calls through your dev server or add a `@CrossOrigin` annotation / Spring Security CORS config
