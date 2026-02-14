# Order Service - Presentation Overview

## 🎯 Service Purpose
**Manages customer orders from cart to delivery in a restaurant management system**

---

## 📊 Key Statistics

| Metric | Value |
|--------|-------|
| **Technology** | Spring Boot 4.0.0, Java 17 |
| **Database** | MySQL (orderdb) |
| **Port** | 8082 |
| **API Endpoints** | 6 endpoints |
| **Order States** | 5 states (CREATED → SERVED) |
| **External Dependencies** | Cart Service |

---

## 🏗️ Architecture Overview

```
┌─────────────────────────────────────────────────┐
│              ORDER SERVICE (Port 8082)          │
├─────────────────────────────────────────────────┤
│                                                 │
│  Controller Layer                               │
│  ├─ OrderController (REST APIs)                │
│                                                 │
│  Service Layer                                  │
│  ├─ OrderService (Business Logic)              │
│  ├─ CartServiceClient (External Integration)   │
│                                                 │
│  Repository Layer                               │
│  ├─ OrderRepository (JPA)                      │
│                                                 │
│  Database                                       │
│  └─ MySQL (orders, order_items tables)         │
└─────────────────────────────────────────────────┘
         ↓                           ↑
    [Cart Service]            [API Gateway/Clients]
```

---

## 🔑 Core Features

### 1. **Cart-Based Order Creation**
- Fetches items from user's cart
- Validates cart contents
- Creates order snapshot
- Automatically clears cart after success

### 2. **Order Lifecycle Management**
- Track orders through 5 states
- Enforce valid state transitions
- Timestamp tracking (created_at)

### 3. **Multi-View Queries**
- Orders by user (customer history)
- Orders by table (table management)
- Active orders (kitchen view)
- Single order details

### 4. **Status Management**
- Update order status
- Validate state transitions
- Prevent invalid status changes

---

## 📡 API Endpoints

### **Customer Operations**

| Method | Endpoint | Purpose | Auth |
|--------|----------|---------|------|
| POST | `/api/orders` | Create order from cart | ✅ JWT + Headers |
| GET | `/api/orders/{id}` | Get order details | ❌ |
| GET | `/api/orders/user` | Get user's orders | ✅ X-User-Id |
| GET | `/api/orders/table` | Get table orders | ✅ X-Table-Id |

### **Staff/Admin Operations**

| Method | Endpoint | Purpose | Auth |
|--------|----------|---------|------|
| GET | `/api/orders/active` | Get active orders | ❌ |
| PATCH | `/api/orders/{id}/status` | Update order status | ❌ |

---

## 🔄 Order Status Workflow

```
CREATED
  ↓ (Staff confirms)
CONFIRMED
  ↓ (Kitchen starts)
PREPARING
  ↓ (Food ready)
READY
  ↓ (Delivered to table)
SERVED (Final)
```

**Valid Transitions:**
- CREATED → CONFIRMED or PREPARING
- CONFIRMED → PREPARING
- PREPARING → READY
- READY → SERVED
- SERVED → (No further transitions)

---

## 💾 Database Schema

### **orders Table**
```sql
- id (Primary Key)
- user_id (Customer)
- table_id (Restaurant table)
- status (ENUM: CREATED/CONFIRMED/PREPARING/READY/SERVED)
- total_amount (DECIMAL 10,2)
- created_at (Timestamp)
```

### **order_items Table**
```sql
- id (Primary Key)
- order_id (Foreign Key → orders)
- item_id (Menu item reference)
- item_name (Snapshot)
- quantity (Integer)
- unit_price (DECIMAL 10,2)
- total_price (DECIMAL 10,2)
```

**Design Note:** Order items store snapshots (name, price) to preserve historical data even if menu changes.

---

## 🔗 External Integration

### **Cart Service Integration**

**Purpose:** Fetch cart items before creating order

**Flow:**
1. POST /api/orders receives Authorization header
2. Order Service calls: `GET http://localhost:8080/api/cart`
3. Validates cart (not empty, valid items)
4. Creates order from cart snapshot
5. Saves order to database
6. Calls: `DELETE http://localhost:8080/api/cart` (cleanup)

**Error Handling:**
- Cart not found → 400 Bad Request
- Cart empty → 400 Bad Request
- Cart Service down → 400 Bad Request
- Cart clearing fails → Log warning (order still succeeds)

---

## 📝 Sample API Usage

### **Create Order**
```http
POST http://localhost:8082/api/orders
Headers:
  X-User-Id: 42
  X-Table-Id: 5
  Authorization: Bearer eyJhbGc...
Body: {}

Response: 201 Created
{
  "id": 123,
  "userId": 42,
  "tableId": 5,
  "status": "CREATED",
  "totalAmount": 2400.00,
  "items": [
    {
      "itemId": 10,
      "itemName": "Margherita Pizza",
      "quantity": 2,
      "unitPrice": 1200.00,
      "totalPrice": 2400.00
    }
  ],
  "createdAt": "2025-12-14T10:30:00"
}
```

### **Update Status**
```http
PATCH http://localhost:8082/api/orders/123/status
Body:
{
  "status": "PREPARING"
}

Response: 200 OK
{
  "id": 123,
  "status": "PREPARING",
  ...
}
```

### **Get Active Orders (Kitchen Display)**
```http
GET http://localhost:8082/api/orders/active

Response: 200 OK
[
  {
    "id": 123,
    "status": "PREPARING",
    "items": [...],
    "createdAt": "2025-12-14T10:30:00"
  },
  ...
]
```

---

## ⚙️ Technical Implementation

### **Technology Stack**
- **Framework:** Spring Boot 4.0.0
- **Language:** Java 17
- **Database:** MySQL 8.0
- **ORM:** Spring Data JPA + Hibernate
- **Validation:** Jakarta Bean Validation
- **REST Client:** RestTemplate
- **Build Tool:** Maven

### **Design Patterns**
- **Repository Pattern** - Data access abstraction
- **Service Layer Pattern** - Business logic separation
- **DTO Pattern** - API/domain decoupling
- **REST Client Pattern** - External service integration

### **Key Annotations**
- `@Transactional` - Ensures data consistency
- `@Valid` - Request validation
- `@RequestHeader` - Extract JWT/user context
- `@OneToMany` - Order-Items relationship

---

## 🛡️ Error Handling

### **Exception Types**
- `BadRequestException` (400) - Invalid input
- `ResourceNotFoundException` (404) - Order not found

### **Global Error Response Format**
```json
{
  "timestamp": "2025-12-14T10:30:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Cart is empty",
  "path": "/api/orders"
}
```

---

## 🔐 Security Features

### **Authentication**
- JWT token via Authorization header
- User ID extracted by API Gateway → X-User-Id header
- Table ID from session → X-Table-Id header

### **Validation**
- Required headers validation
- Cart item validation (quantity > 0, price > 0)
- Order status transition validation

---

## 📈 Key Metrics & Logging

### **Logged Events**
- ✅ Order creation (with user, table, amount)
- ✅ Cart fetching and validation
- ✅ Status updates
- ✅ Query operations
- ⚠️ Cart clearing failures
- ❌ All errors with context

### **Log Level Configuration**
```yaml
logging:
  level:
    com.example.order_service: DEBUG
    org.hibernate.SQL: DEBUG
```

---

## 🚀 Deployment Configuration

### **Database Connection**
```yaml
spring.datasource.url: jdbc:mysql://localhost:3306/orderdb
spring.datasource.username: root
spring.datasource.password: orderservice@12345
```

### **External Services**
```yaml
cart-service.base-url: http://localhost:8080/api/cart
```

### **Server**
```yaml
server.port: 8082
```

**Note:** Use environment variables for production:
- `${DB_URL}`
- `${DB_PASSWORD}`
- `${CART_SERVICE_URL}`

---

## ✅ Business Benefits

1. **Cart Integration** - Seamless checkout experience
2. **Order Tracking** - Real-time status updates
3. **Kitchen Management** - Active orders view for staff
4. **Historical Data** - Order history per user/table
5. **Data Integrity** - Transactional order creation
6. **Failure Resilience** - Graceful handling of cart service issues

---

## 🎓 Learning Outcomes (For University Demo)

### **Demonstrated Concepts**
✅ **Microservices Architecture** - Service-to-service communication
✅ **REST API Design** - RESTful principles
✅ **Database Design** - Relational schema, foreign keys
✅ **State Machine** - Order status workflow
✅ **Transaction Management** - ACID properties
✅ **Error Handling** - Global exception handling
✅ **External Integration** - HTTP client (RestTemplate)
✅ **JWT Authentication** - Token-based auth
✅ **Logging** - SLF4J for debugging

---

## 🔮 Future Enhancements

- 🔄 **Order Cancellation** - Allow customers to cancel pending orders
- 💳 **Payment Integration** - Link with payment service
- 📧 **Notifications** - Send order updates via email/SMS
- 📊 **Analytics** - Order trends, popular items
- 🕐 **Scheduled Orders** - Future order placement
- 🔁 **Order Modification** - Edit items before preparation
- 📱 **Push Notifications** - Real-time status updates to mobile
- 🤖 **AI Integration** - Estimated preparation time prediction

---

## 📞 Contact & Resources

**Service Health:** `http://localhost:8082/actuator/health` (if enabled)
**API Documentation:** Available in `CART_INTEGRATION_DOCS.md`
**Order README:** `ORDER_SERVICE_README.md`

---

## 🎬 Demo Scenarios

### **Scenario 1: Happy Path**
1. Customer adds items to cart
2. Customer places order
3. Kitchen receives order
4. Kitchen updates status: PREPARING → READY
5. Waiter marks as SERVED

### **Scenario 2: Empty Cart**
1. Customer tries to order without items
2. System rejects with helpful error message

### **Scenario 3: Multiple Users**
1. Multiple customers ordering simultaneously
2. Each order tracked independently
3. Kitchen sees all active orders

---

**Order Service - Powering Seamless Restaurant Operations** 🍽️

