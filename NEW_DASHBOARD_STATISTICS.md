# New Dashboard Statistics — Per-Account Breakdown

## Overview

The four existing dashboard endpoints (`/overview`, `/portfolio`, `/spending`, `/income`) already accept `?accountId=` to filter to a single account. This update adds one new endpoint that returns all four stat blocks for **every account** in a single call, so the frontend can render a side-by-side or tabbed per-account dashboard without making N×4 requests.

---

## New endpoint

### `GET /api/dashboard/by-account`

Returns an array with one entry per account. Each entry contains the account's metadata plus its own `overview`, `portfolio`, `spending`, and `income` stats — scoped exclusively to transactions belonging to that account.

**No query parameters.**

**Response** `200 OK`

```json
[
  {
    "account": {
      "id": 1,
      "name": "Trade Republic",
      "color": null,
      "icon": null,
      "source": "TRADE_REPUBLIC",
      "ownAccountIban": null,
      "createdAt": "2024-01-01T00:00:00Z",
      "updatedAt": "2024-01-01T00:00:00Z"
    },
    "overview": {
      "cashBalance": 1240.50,
      "currency": "EUR",
      "totalTransactionCount": 198,
      "totalFeesAndTaxesPaid": 22.10
    },
    "portfolio": {
      "totalInvested": 11200.00,
      "totalDividendsReceived": 310.40,
      "positions": [
        {
          "symbol": "AAPL",
          "name": "Apple Inc.",
          "totalShares": 5.5,
          "totalInvested": 850.00,
          "totalDividendsReceived": 12.50
        }
      ]
    },
    "spending": {
      "totalCardSpending": 890.30,
      "totalTransferOutbound": 0.00,
      "topMerchants": [
        {
          "merchantName": "REWE",
          "totalSpent": 210.40,
          "transactionCount": 9
        }
      ]
    },
    "income": {
      "totalIncome": 5310.80,
      "interestPayments": 98.20,
      "dividends": 310.40,
      "bonuses": 50.00,
      "saveback": 12.20,
      "transferInbound": 4700.00,
      "customerInpayments": 140.00
    }
  },
  {
    "account": {
      "id": 2,
      "name": "Sparkasse 4321",
      "color": "#3b82f6",
      "icon": "bank",
      "source": "SPARKASSE",
      "ownAccountIban": "AT12345678904321",
      "createdAt": "2024-06-01T00:00:00Z",
      "updatedAt": "2024-06-01T00:00:00Z"
    },
    "overview": {
      "cashBalance": 3010.33,
      "currency": "EUR",
      "totalTransactionCount": 114,
      "totalFeesAndTaxesPaid": 16.40
    },
    "portfolio": {
      "totalInvested": 0.00,
      "totalDividendsReceived": 0.00,
      "positions": []
    },
    "spending": {
      "totalCardSpending": 1210.45,
      "totalTransferOutbound": 500.00,
      "topMerchants": [
        {
          "merchantName": "Billa",
          "totalSpent": 320.10,
          "transactionCount": 14
        }
      ]
    },
    "income": {
      "totalIncome": 4720.78,
      "interestPayments": 22.10,
      "dividends": 0.00,
      "bonuses": 0.00,
      "saveback": 3.10,
      "transferInbound": 4695.58,
      "customerInpayments": 0.00
    }
  }
]
```

---

## Field reference

### `account`

Same shape as `GET /api/accounts/{id}`.

| Field | Type | Description |
|---|---|---|
| `id` | Long | Account ID |
| `name` | String | Display name (e.g. `"Sparkasse 4321"`) |
| `color` | String\|null | Hex color set via PATCH |
| `icon` | String\|null | Icon identifier set via PATCH |
| `source` | String | `TRADE_REPUBLIC` or `SPARKASSE` |
| `ownAccountIban` | String\|null | IBAN of the account; null for Trade Republic |
| `createdAt` | ISO-8601 | When the account was first seen during import |
| `updatedAt` | ISO-8601 | Last metadata update |

### `overview`

Identical to `GET /api/dashboard/overview?accountId={id}`.

| Field | Description |
|---|---|
| `cashBalance` | Net signed sum of all transaction amounts for this account |
| `currency` | Always `EUR` |
| `totalTransactionCount` | Number of transactions on this account |
| `totalFeesAndTaxesPaid` | Absolute value of all fees + taxes on this account |

### `portfolio`

Identical to `GET /api/dashboard/portfolio?accountId={id}`.

Sparkasse accounts will always return `positions: []` and zeroed totals — assets are only held in Trade Republic.

| Field | Description |
|---|---|
| `totalInvested` | Sum of cost basis across all positions |
| `totalDividendsReceived` | Sum of dividends received |
| `positions` | Per-asset breakdown (see existing docs) |

### `spending`

Identical to `GET /api/dashboard/spending?accountId={id}`.

| Field | Description |
|---|---|
| `totalCardSpending` | Absolute sum of card transactions |
| `totalTransferOutbound` | Absolute sum of outbound transfers |
| `topMerchants` | Top 10 merchants by spend for this account |

### `income`

Identical to `GET /api/dashboard/income?accountId={id}`.

| Field | Description |
|---|---|
| `totalIncome` | Sum of all income fields |
| `interestPayments` | Interest income |
| `dividends` | Dividend income |
| `bonuses` | Bonus payments |
| `saveback` | Saveback cashback |
| `transferInbound` | Incoming transfers |
| `customerInpayments` | Top-ups and customer inbound |

---

## Existing per-account filter (unchanged)

The original four endpoints still support `?accountId=` for querying one account at a time:

```
GET /api/dashboard/overview?accountId=2
GET /api/dashboard/portfolio?accountId=1
GET /api/dashboard/spending?accountId=2
GET /api/dashboard/income?accountId=2
```

Omit `accountId` for combined totals across all accounts. Unknown `accountId` → `404 Not Found`.

---

## Suggested UI patterns

| Pattern | Endpoint to use |
|---|---|
| Side-by-side account cards on a dashboard home screen | `GET /api/dashboard/by-account` |
| Tabbed view switching between accounts | `GET /api/dashboard/by-account` (load once, switch tabs client-side) |
| Single account drill-down page | `GET /api/dashboard/{overview\|portfolio\|spending\|income}?accountId={id}` |
| Combined totals across all accounts | `GET /api/dashboard/{overview\|portfolio\|spending\|income}` (no `accountId`) |
