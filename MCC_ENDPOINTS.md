# MCC Endpoints

## GET /api/mcc

Returns all MCC codes with their descriptions. Intended for populating dropdowns.

**Response** `200 OK`
```json
[
  {
    "mcc": "5411",
    "description": "Grocery Stores, Supermarkets"
  },
  {
    "mcc": "5812",
    "description": "Eating Places, Restaurants"
  }
]
```

The `description` field prefers `editedDescription` and falls back to `combinedDescription`.

---

## GET /api/transactions/by-mcc/{mcc}

Returns a paginated list of transactions that have the given MCC code assigned.

**Path parameter**

| Parameter | Type   | Description                  |
|-----------|--------|------------------------------|
| `mcc`     | String | 4-digit MCC code, e.g. `5411` |

**Query parameters (pagination)**

| Parameter | Default | Description            |
|-----------|---------|------------------------|
| `page`    | `0`     | Page index (zero-based) |
| `size`    | `50`    | Page size              |
| `sort`    | —       | e.g. `date,desc`       |

**Response** `200 OK` — paginated `TransactionResponse` objects.

```json
{
  "content": [ { "..." } ],
  "totalElements": 42,
  "totalPages": 1,
  "size": 50,
  "number": 0
}
```

Transactions without an MCC code assigned will never appear in this endpoint's results.
