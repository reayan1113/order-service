# Order Service - API Documentation for Frontend

**Base URL:** `http://localhost:8083` (Development) | `https://your-app.azurecontainerapps.io` (Production)

**Last Updated:** February 15, 2026

---

## Table of Contents
1. [Authentication & Headers](#authentication--headers)
2. [Order Status Flow](#order-status-flow)
3. [API Endpoints](#api-endpoints)
4. [Data Models](#data-models)
5. [Error Handling](#error-handling)
6. [Frontend Integration Guide](#frontend-integration-guide)

---

## Authentication & Headers

All requests require the following headers:

| Header | Type | Required | Description |
|--------|------|----------|-------------|
| `Authorization` | String | Yes | Bearer token from authentication service |
| `X-User-Id` | Long | Conditional | Required for creating orders and user-specific queries |
| `X-Table-Id` | Long | Conditional | Required for creating orders and table-specific queries |

---

## Order Status Flow

Orders progress through the following statuses:

```
CREATED → CONFIRMED → PREPARING → READY → SERVED
```

| Status | Description | User Action | Next Status |
|--------|-------------|-------------|-------------|
| `CREATED` | Order placed, waiting for confirmation | Wait for restaurant | `CONFIRMED` |
| `CONFIRMED` | Restaurant accepted the order | Wait for preparation | `PREPARING` |
| `PREPARING` | Kitchen is preparing the order | Wait for completion | `READY` |
| `READY` | Order is ready for pickup/serving | Notify customer | `SERVED` |
| `SERVED` | Order has been delivered/served | Complete | - |

---

## API Endpoints

### 1. Create Order

**Endpoint:** `POST /api/orders`

**Purpose:** Create a new order from the user's cart

**Frontend Usage:** 
- **Page:** Checkout / Order Confirmation page
- **Trigger:** User clicks "Place Order" button
- **Flow:** Fetches items from cart service and creates order

**Headers:**
```
Authorization: Bearer <token>
X-User-Id: 123
X-Table-Id: 5
Content-Type: application/json
```

**Request Body:** (Optional - items fetched from cart service)
```json
{
  "items": []  // Optional: Can be empty or omitted
}
```

**Response:** `201 Created`
```json
{
  "id": 1,
  "tableId": 5,
  "userId": 123,
  "status": "CREATED",
  "totalAmount": 45.50,
  "createdAt": "2026-02-15T10:30:00",
  "items": [
    {
      "id": 1,
      "itemId": 10,
      "itemName": "Burger",
      "quantity": 2,
      "unitPrice": 12.50
    },
    {
      "id": 2,
      "itemId": 15,
      "itemName": "Fries",
      "quantity": 1,
      "unitPrice": 5.00
    }
  ]
}
```

**Frontend Implementation:**
```javascript
async function createOrder(userId, tableId, authToken) {
  const response = await fetch('/api/orders', {
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${authToken}`,
      'X-User-Id': userId,
      'X-Table-Id': tableId,
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({})  // Empty body - items from cart
  });
  
  if (response.status === 201) {
    const order = await response.json();
    // Navigate to order confirmation page
    // Clear cart
    return order;
  }
}
```

**Notes:**
- ⚠️ This endpoint automatically fetches items from the Cart Service using the Authorization header
- ⚠️ After successful order creation, the cart is automatically cleared
- ⚠️ Returns 400 if cart is empty or not found

---

### 2. Get Order by ID

**Endpoint:** `GET /api/orders/{orderId}`

**Purpose:** Retrieve details of a specific order

**Frontend Usage:**
- **Page:** Order Details page
- **Trigger:** User clicks on an order from their order history
- **Flow:** Display order information, status, and items

**Headers:**
```
Authorization: Bearer <token>
```

**Path Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| `orderId` | Long | The order ID |

**Response:** `200 OK`
```json
{
  "id": 1,
  "tableId": 5,
  "userId": 123,
  "status": "PREPARING",
  "totalAmount": 45.50,
  "createdAt": "2026-02-15T10:30:00",
  "items": [
    {
      "id": 1,
      "itemId": 10,
      "itemName": "Burger",
      "quantity": 2,
      "unitPrice": 12.50
    }
  ]
}
```

**Frontend Implementation:**
```javascript
async function getOrderDetails(orderId, authToken) {
  const response = await fetch(`/api/orders/${orderId}`, {
    method: 'GET',
    headers: {
      'Authorization': `Bearer ${authToken}`
    }
  });
  
  if (response.ok) {
    const order = await response.json();
    // Display order details
    // Show status badge with appropriate color
    return order;
  }
}
```

**Notes:**
- Use for order tracking page
- Poll this endpoint every 30-60 seconds for real-time status updates
- Returns 404 if order not found

---

### 3. Get Orders by Table

**Endpoint:** `GET /api/orders/table`

**Purpose:** Retrieve all orders for a specific table

**Frontend Usage:**
- **Page:** Table Dashboard / Waiter App
- **Trigger:** When viewing a specific table
- **Flow:** Show all orders associated with a table

**Headers:**
```
Authorization: Bearer <token>
X-Table-Id: 5
```

**Response:** `200 OK`
```json
[
  {
    "id": 1,
    "tableId": 5,
    "userId": 123,
    "status": "PREPARING",
    "totalAmount": 45.50,
    "createdAt": "2026-02-15T10:30:00",
    "items": [...]
  },
  {
    "id": 2,
    "tableId": 5,
    "userId": 124,
    "status": "READY",
    "totalAmount": 32.00,
    "createdAt": "2026-02-15T11:00:00",
    "items": [...]
  }
]
```

**Frontend Implementation:**
```javascript
async function getTableOrders(tableId, authToken) {
  const response = await fetch('/api/orders/table', {
    method: 'GET',
    headers: {
      'Authorization': `Bearer ${authToken}`,
      'X-Table-Id': tableId
    }
  });
  
  if (response.ok) {
    const orders = await response.json();
    // Display orders in table view
    // Group by status or user
    return orders;
  }
}
```

**Notes:**
- Useful for waiter apps to see all orders for a table
- Returns empty array if no orders found
- Can be used to calculate total bill for a table

---

### 4. Get Orders by User

**Endpoint:** `GET /api/orders/user`

**Purpose:** Retrieve all orders placed by a specific user

**Frontend Usage:**
- **Page:** User Order History / My Orders page
- **Trigger:** User navigates to their order history
- **Flow:** Display user's past and current orders

**Headers:**
```
Authorization: Bearer <token>
X-User-Id: 123
```

**Response:** `200 OK`
```json
[
  {
    "id": 1,
    "tableId": 5,
    "userId": 123,
    "status": "SERVED",
    "totalAmount": 45.50,
    "createdAt": "2026-02-15T10:30:00",
    "items": [...]
  },
  {
    "id": 5,
    "tableId": 3,
    "userId": 123,
    "status": "PREPARING",
    "totalAmount": 28.00,
    "createdAt": "2026-02-15T12:00:00",
    "items": [...]
  }
]
```

**Frontend Implementation:**
```javascript
async function getUserOrders(userId, authToken) {
  const response = await fetch('/api/orders/user', {
    method: 'GET',
    headers: {
      'Authorization': `Bearer ${authToken}`,
      'X-User-Id': userId
    }
  });
  
  if (response.ok) {
    const orders = await response.json();
    // Display in order history list
    // Sort by createdAt (newest first)
    // Filter by status if needed
    return orders.sort((a, b) => 
      new Date(b.createdAt) - new Date(a.createdAt)
    );
  }
}
```

**Notes:**
- Use for "My Orders" page
- Orders are returned unsorted - implement sorting in frontend
- Returns empty array if no orders found
- Consider pagination for users with many orders

---

### 5. Get Active Orders

**Endpoint:** `GET /api/orders/active`

**Purpose:** Retrieve all orders that are not yet served

**Frontend Usage:**
- **Page:** Kitchen Display System / Admin Dashboard
- **Trigger:** Real-time monitoring of active orders
- **Flow:** Show orders in queue for kitchen

**Headers:**
```
Authorization: Bearer <token>
```

**Response:** `200 OK`
```json
[
  {
    "id": 1,
    "tableId": 5,
    "userId": 123,
    "status": "PREPARING",
    "totalAmount": 45.50,
    "createdAt": "2026-02-15T10:30:00",
    "items": [...]
  },
  {
    "id": 2,
    "tableId": 3,
    "userId": 125,
    "status": "CONFIRMED",
    "totalAmount": 32.00,
    "createdAt": "2026-02-15T10:45:00",
    "items": [...]
  }
]
```

**Frontend Implementation:**
```javascript
async function getActiveOrders(authToken) {
  const response = await fetch('/api/orders/active', {
    method: 'GET',
    headers: {
      'Authorization': `Bearer ${authToken}`
    }
  });
  
  if (response.ok) {
    const orders = await response.json();
    // Group by status
    const grouped = {
      confirmed: orders.filter(o => o.status === 'CONFIRMED'),
      preparing: orders.filter(o => o.status === 'PREPARING'),
      ready: orders.filter(o => o.status === 'READY')
    };
    return grouped;
  }
}

// Poll for updates every 10 seconds
setInterval(() => {
  getActiveOrders(authToken).then(displayActiveOrders);
}, 10000);
```

**Notes:**
- "Active" means status is NOT "SERVED"
- Perfect for kitchen display screens
- Implement polling (every 10-30 seconds) for real-time updates
- Returns empty array if no active orders

---

### 6. Update Order Status

**Endpoint:** `PATCH /api/orders/{orderId}/status`

**Purpose:** Update the status of an order (kitchen/waiter use)

**Frontend Usage:**
- **Page:** Kitchen Dashboard / Waiter App / Admin Panel
- **Trigger:** Kitchen confirms, completes, or serves order
- **Flow:** Change order status through workflow

**Headers:**
```
Authorization: Bearer <token>
Content-Type: application/json
```

**Path Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| `orderId` | Long | The order ID to update |

**Request Body:**
```json
{
  "status": "PREPARING"
}
```

**Valid Status Values:**
- `CREATED`
- `CONFIRMED`
- `PREPARING`
- `READY`
- `SERVED`

**Response:** `200 OK`
```json
{
  "id": 1,
  "tableId": 5,
  "userId": 123,
  "status": "PREPARING",
  "totalAmount": 45.50,
  "createdAt": "2026-02-15T10:30:00",
  "items": [...]
}
```

**Frontend Implementation:**
```javascript
async function updateOrderStatus(orderId, newStatus, authToken) {
  const response = await fetch(`/api/orders/${orderId}/status`, {
    method: 'PATCH',
    headers: {
      'Authorization': `Bearer ${authToken}`,
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({
      status: newStatus
    })
  });
  
  if (response.ok) {
    const updatedOrder = await response.json();
    // Update UI with new status
    // Show success notification
    return updatedOrder;
  }
}

// Example: Kitchen confirms order
function confirmOrder(orderId) {
  updateOrderStatus(orderId, 'CONFIRMED', authToken)
    .then(() => showNotification('Order confirmed!'));
}

// Example: Mark order as ready
function markOrderReady(orderId) {
  updateOrderStatus(orderId, 'READY', authToken)
    .then(() => {
      showNotification('Order ready for serving!');
      notifyWaiter(orderId);
    });
}
```

**Status Transition Buttons:**
```javascript
// Kitchen UI Example
const statusButtons = {
  CREATED: { next: 'CONFIRMED', label: 'Confirm Order', color: 'blue' },
  CONFIRMED: { next: 'PREPARING', label: 'Start Preparing', color: 'orange' },
  PREPARING: { next: 'READY', label: 'Mark as Ready', color: 'green' },
  READY: { next: 'SERVED', label: 'Mark as Served', color: 'gray' }
};
```

**Notes:**
- ⚠️ Validate status transitions in frontend to prevent invalid flows
- Only authorized users (kitchen/waiters) should access this endpoint
- Returns 404 if order not found
- Returns 400 if status value is invalid

---

## Data Models

### OrderResponse

```typescript
interface OrderResponse {
  id: number;
  tableId: number;
  userId: number;
  status: 'CREATED' | 'CONFIRMED' | 'PREPARING' | 'READY' | 'SERVED';
  totalAmount: number;  // Decimal as number (e.g., 45.50)
  createdAt: string;    // ISO 8601 date string
  items: OrderItemResponse[];
}
```

### OrderItemResponse

```typescript
interface OrderItemResponse {
  id: number;
  itemId: number;       // Reference to menu item
  itemName: string;
  quantity: number;
  unitPrice: number;    // Decimal as number (e.g., 12.50)
}
```

### CreateOrderRequest

```typescript
interface CreateOrderRequest {
  items?: OrderItemRequest[];  // Optional - fetched from cart if not provided
}

interface OrderItemRequest {
  itemId: number;
  itemName: string;
  quantity: number;
  unitPrice: number;
}
```

### UpdateOrderStatusRequest

```typescript
interface UpdateOrderStatusRequest {
  status: 'CREATED' | 'CONFIRMED' | 'PREPARING' | 'READY' | 'SERVED';
}
```

---

## Error Handling

### Common Error Responses

#### 400 Bad Request
```json
{
  "timestamp": "2026-02-15T10:30:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Cart not found. Please add items to cart before placing an order.",
  "path": "/api/orders"
}
```

**Frontend Handling:**
```javascript
if (response.status === 400) {
  const error = await response.json();
  showError(error.message);
  // Redirect to menu/cart page
}
```

#### 404 Not Found
```json
{
  "timestamp": "2026-02-15T10:30:00",
  "status": 404,
  "error": "Not Found",
  "message": "Order not found with id: 999",
  "path": "/api/orders/999"
}
```

**Frontend Handling:**
```javascript
if (response.status === 404) {
  showError('Order not found');
  // Redirect to order list
}
```

#### 401 Unauthorized
```json
{
  "timestamp": "2026-02-15T10:30:00",
  "status": 401,
  "error": "Unauthorized",
  "message": "Invalid or expired token"
}
```

**Frontend Handling:**
```javascript
if (response.status === 401) {
  // Clear auth token
  // Redirect to login
  redirectToLogin();
}
```

#### 500 Internal Server Error
```json
{
  "timestamp": "2026-02-15T10:30:00",
  "status": 500,
  "error": "Internal Server Error",
  "message": "An unexpected error occurred"
}
```

**Frontend Handling:**
```javascript
if (response.status === 500) {
  showError('Something went wrong. Please try again later.');
  // Log error for debugging
  console.error('Server error:', error);
}
```

---

## Frontend Integration Guide

### 1. Order Placement Flow

**User Journey:**
```
Menu → Cart → Checkout → Place Order → Order Confirmation → Track Order
```

**Implementation:**
```javascript
// Step 1: User adds items to cart (Cart Service)
// Step 2: User clicks "Checkout"
// Step 3: Show order summary from cart
// Step 4: User confirms and places order

async function placeOrder(userId, tableId, authToken) {
  try {
    // Create order from cart
    const order = await createOrder(userId, tableId, authToken);
    
    // Show success message
    showSuccessNotification(`Order #${order.id} placed successfully!`);
    
    // Navigate to order tracking page
    navigateTo(`/orders/${order.id}`);
    
    // Start polling for status updates
    startOrderTracking(order.id);
    
  } catch (error) {
    showError('Failed to place order. Please try again.');
  }
}
```

### 2. Order Tracking

**User Journey:**
```
Order Confirmation → Track Status → Receive Notifications → Order Complete
```

**Implementation:**
```javascript
function OrderTrackingComponent({ orderId }) {
  const [order, setOrder] = useState(null);
  
  useEffect(() => {
    // Initial fetch
    fetchOrder();
    
    // Poll every 30 seconds
    const interval = setInterval(fetchOrder, 30000);
    
    return () => clearInterval(interval);
  }, [orderId]);
  
  async function fetchOrder() {
    const orderData = await getOrderDetails(orderId, authToken);
    setOrder(orderData);
    
    // Show notification if status changed
    if (orderData.status !== order?.status) {
      showStatusNotification(orderData.status);
    }
  }
  
  return (
    <div>
      <h2>Order #{order?.id}</h2>
      <StatusBadge status={order?.status} />
      <OrderTimeline status={order?.status} />
      <ItemsList items={order?.items} />
      <Total amount={order?.totalAmount} />
    </div>
  );
}
```

### 3. Kitchen Display System

**Implementation:**
```javascript
function KitchenDisplay() {
  const [activeOrders, setActiveOrders] = useState([]);
  
  useEffect(() => {
    fetchActiveOrders();
    
    // Poll every 10 seconds
    const interval = setInterval(fetchActiveOrders, 10000);
    
    return () => clearInterval(interval);
  }, []);
  
  async function fetchActiveOrders() {
    const orders = await getActiveOrders(authToken);
    setActiveOrders(orders);
  }
  
  async function handleStatusUpdate(orderId, newStatus) {
    await updateOrderStatus(orderId, newStatus, authToken);
    fetchActiveOrders(); // Refresh list
  }
  
  return (
    <div className="kitchen-grid">
      <OrderColumn 
        title="New Orders" 
        orders={activeOrders.filter(o => o.status === 'CONFIRMED')}
        onAction={(id) => handleStatusUpdate(id, 'PREPARING')}
        actionLabel="Start Preparing"
      />
      <OrderColumn 
        title="Preparing" 
        orders={activeOrders.filter(o => o.status === 'PREPARING')}
        onAction={(id) => handleStatusUpdate(id, 'READY')}
        actionLabel="Mark Ready"
      />
      <OrderColumn 
        title="Ready" 
        orders={activeOrders.filter(o => o.status === 'READY')}
        onAction={(id) => handleStatusUpdate(id, 'SERVED')}
        actionLabel="Mark Served"
      />
    </div>
  );
}
```

### 4. Order History

**Implementation:**
```javascript
function OrderHistory({ userId }) {
  const [orders, setOrders] = useState([]);
  const [filter, setFilter] = useState('all');
  
  useEffect(() => {
    fetchUserOrders();
  }, [userId]);
  
  async function fetchUserOrders() {
    const userOrders = await getUserOrders(userId, authToken);
    setOrders(userOrders);
  }
  
  const filteredOrders = filter === 'all' 
    ? orders 
    : orders.filter(o => o.status === filter);
  
  return (
    <div>
      <h2>My Orders</h2>
      <FilterButtons 
        options={['all', 'PREPARING', 'READY', 'SERVED']}
        selected={filter}
        onChange={setFilter}
      />
      <OrderList orders={filteredOrders} />
    </div>
  );
}
```

### 5. Status Badges & Colors

```javascript
const statusConfig = {
  CREATED: {
    label: 'Order Placed',
    color: '#3B82F6',  // Blue
    icon: '📝'
  },
  CONFIRMED: {
    label: 'Confirmed',
    color: '#8B5CF6',  // Purple
    icon: '✓'
  },
  PREPARING: {
    label: 'Preparing',
    color: '#F59E0B',  // Orange
    icon: '👨‍🍳'
  },
  READY: {
    label: 'Ready',
    color: '#10B981',  // Green
    icon: '✓✓'
  },
  SERVED: {
    label: 'Served',
    color: '#6B7280',  // Gray
    icon: '🍽️'
  }
};

function StatusBadge({ status }) {
  const config = statusConfig[status];
  
  return (
    <span 
      style={{ 
        backgroundColor: config.color,
        color: 'white',
        padding: '4px 12px',
        borderRadius: '12px'
      }}
    >
      {config.icon} {config.label}
    </span>
  );
}
```

---

## Best Practices

### 1. Polling vs WebSockets
- Currently uses REST API - implement polling for real-time updates
- Recommended polling intervals:
  - Order tracking page: 30 seconds
  - Kitchen display: 10 seconds
  - Active orders dashboard: 15 seconds

### 2. Error Handling
```javascript
async function safeApiCall(apiFunction) {
  try {
    return await apiFunction();
  } catch (error) {
    if (error.status === 401) {
      // Unauthorized - redirect to login
      handleUnauthorized();
    } else if (error.status === 404) {
      // Not found - show appropriate message
      showNotFound();
    } else {
      // Generic error
      showError('Something went wrong. Please try again.');
    }
  }
}
```

### 3. Loading States
```javascript
function OrderDetails({ orderId }) {
  const [order, setOrder] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  
  useEffect(() => {
    setLoading(true);
    getOrderDetails(orderId, authToken)
      .then(setOrder)
      .catch(setError)
      .finally(() => setLoading(false));
  }, [orderId]);
  
  if (loading) return <Spinner />;
  if (error) return <ErrorMessage error={error} />;
  if (!order) return <NotFound />;
  
  return <OrderDetailsView order={order} />;
}
```

### 4. Caching Strategy
```javascript
// Use React Query or similar for caching
import { useQuery } from 'react-query';

function useOrder(orderId) {
  return useQuery(['order', orderId], () => 
    getOrderDetails(orderId, authToken),
    {
      refetchInterval: 30000,  // Refetch every 30 seconds
      staleTime: 10000,        // Consider data stale after 10 seconds
    }
  );
}
```

---

## Testing Endpoints

### Using cURL

```bash
# Create Order
curl -X POST http://localhost:8083/api/orders \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "X-User-Id: 123" \
  -H "X-Table-Id: 5" \
  -H "Content-Type: application/json" \
  -d '{}'

# Get Order
curl http://localhost:8083/api/orders/1 \
  -H "Authorization: Bearer YOUR_TOKEN"

# Get User Orders
curl http://localhost:8083/api/orders/user \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "X-User-Id: 123"

# Get Table Orders
curl http://localhost:8083/api/orders/table \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "X-Table-Id: 5"

# Get Active Orders
curl http://localhost:8083/api/orders/active \
  -H "Authorization: Bearer YOUR_TOKEN"

# Update Status
curl -X PATCH http://localhost:8083/api/orders/1/status \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"status": "PREPARING"}'
```

---

## Support & Questions

For issues or questions about the API:
- Check the service logs for detailed error messages
- Verify authentication tokens are valid
- Ensure required headers are included
- Test with the health endpoint: `GET /actuator/health`

**Health Check:**
```bash
curl http://localhost:8083/actuator/health
```

Expected response:
```json
{
  "status": "UP"
}
```

---

**Document Version:** 1.0  
**API Version:** 1.0  
**Service:** Order Service  
**Maintained by:** Backend Team

