# Dashboard Endpoints

All endpoints are under `/api/dashboard` and return `200 OK` with no query parameters.

> **Sign convention in the database:** All `amount` values are already signed — inflows (deposits, interest, dividends, sell proceeds) are stored as positive numbers, outflows (card transactions, buys) are stored as negative numbers. Fees and taxes are also stored as negative numbers. The endpoints handle all negation internally so every response field represents a positive (or meaningful signed) value.

---

## GET /api/dashboard/overview

General account summary. `cashBalance` is the sum of **all non-null amounts** across every transaction. Because amounts are already signed, this naturally produces the correct net cash position without any type-based categorisation.

`totalFeesAndTaxesPaid` is the absolute value of the sum of all `fee` and `tax` fields (which are negative in the DB).

**Response**

```json
{
  "cashBalance": 4250.83,
  "currency": "EUR",
  "totalTransactionCount": 312,
  "totalFeesAndTaxesPaid": 38.50
}
```

| Field | Description |
|---|---|
| `cashBalance` | Net cash balance derived from transaction history |
| `currency` | Always `EUR` (all amounts are stored in EUR) |
| `totalTransactionCount` | Total number of transactions in the database |
| `totalFeesAndTaxesPaid` | Sum of all `fee` and `tax` fields across all transactions |

---

## GET /api/dashboard/portfolio

Portfolio summary based purely on transaction cost basis — does not reflect current market value.

**Response**

```json
{
  "totalInvested": 12500.00,
  "totalDividendsReceived": 320.40,
  "positions": [
    {
      "symbol": "AAPL",
      "name": "Apple Inc.",
      "totalShares": 5.5,
      "totalInvested": 850.00,
      "totalDividendsReceived": 12.50
    }
  ]
}
```

| Field | Description |
|---|---|
| `totalInvested` | Sum of `totalInvested` across all positions |
| `totalDividendsReceived` | Sum of `totalDividendsReceived` across all positions |
| `positions[].totalShares` | Net shares held — `SUM(shares)`, which are already signed in the DB (SELL shares are stored as negative) |
| `positions[].totalInvested` | Net cash spent on this asset — negation of `SUM(BUY/SELL amounts)`. BUY amounts are negative in the DB, SELL positive, so negating the sum gives a positive "still invested" value |
| `positions[].totalDividendsReceived` | Gross sum of `DIVIDEND` transaction amounts (positive in DB) |

---

## GET /api/dashboard/spending

Outgoing cash flow broken down by card spending and transfers, plus the top 10 merchants by total amount spent.

**Response**

```json
{
  "totalCardSpending": 2100.75,
  "totalTransferOutbound": 500.00,
  "topMerchants": [
    {
      "merchantName": "REWE",
      "totalSpent": 430.20,
      "transactionCount": 18
    }
  ]
}
```

| Field | Description |
|---|---|
| `totalCardSpending` | Absolute value of the sum of `CARD_TRANSACTION` + `CARD_TRANSACTION_INTERNATIONAL` amounts (negated since card amounts are negative in DB) |
| `totalTransferOutbound` | Absolute value of the sum of `TRANSFER_INSTANT_OUTBOUND` amounts |
| `topMerchants` | Top 10 merchants by total amount spent, descending. `totalSpent` is a positive value. |

---

## GET /api/dashboard/income

All income broken down by source type.

**Response**

```json
{
  "totalIncome": 6500.00,
  "interestPayments": 120.30,
  "dividends": 320.40,
  "bonuses": 50.00,
  "saveback": 15.20,
  "transferInbound": 5800.00,
  "customerInpayments": 194.10
}
```

| Field | Description |
|---|---|
| `totalIncome` | Sum of all fields below |
| `interestPayments` | `INTEREST_PAYMENT` transactions |
| `dividends` | `DIVIDEND` transactions |
| `bonuses` | `BONUS` transactions |
| `saveback` | `BENEFITS_SAVEBACK` transactions |
| `transferInbound` | `TRANSFER_INBOUND` + `TRANSFER_INSTANT_INBOUND` transactions |
| `customerInpayments` | `CUSTOMER_INPAYMENT` + `CUSTOMER_INBOUND` transactions |
