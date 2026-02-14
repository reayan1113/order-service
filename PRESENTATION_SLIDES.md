# Order Service - Presentation Slides

---

## SLIDE 1: Title Slide

### **Order Service**
**Restaurant Order Management System**

*Microservice for handling customer orders from cart to delivery*

**Technology:** Spring Boot | MySQL | REST APIs  
**Port:** 8082  
**Team:** [Your Team Name]  
**Date:** December 2025

---

## SLIDE 2: What is Order Service?

### **Purpose**
Manages the complete order lifecycle in a restaurant system

### **Key Responsibilities**
- ✅ Create orders from customer cart
- ✅ Track order status (Kitchen → Table)
- ✅ Manage order history
- ✅ Provide real-time order views

### **Why It Matters**
Bridges the gap between customer ordering and kitchen operations

---

## SLIDE 3: System Architecture

```
┌──────────────┐
│   Customer   │
│  (Web/App)   │
└──────┬───────┘
       │ JWT + Cart
       ↓
┌──────────────────┐
│  API Gateway     │
└──────┬───────────┘
       │
       ↓
┌──────────────────────────────┐
│   ORDER SERVICE (8082)       │
│                              │
│  ┌────────────────────┐     │
│  │  OrderController    │     │
│  └─────────┬───────────┘     │
│            ↓                 │
│  ┌────────────────────┐     │
│  │  OrderService       │←────┼──→ Cart Service
│  └─────────┬───────────┘     │
│            ↓                 │
│  ┌────────────────────┐     │
│  │  MySQL Database     │     │
│  └────────────────────┘     │
└──────────────────────────────┘
```

---

## SLIDE 4: Database Design

### **orders Table**
| Column | Type | Description |
|--------|------|-------------|
| id | BIGINT | Primary key |
| user_id | BIGINT | Customer ID |
| table_id | BIGINT | Restaurant table |
| status | ENUM | Order state |
| total_amount | DECIMAL | Order total |
| created_at | TIMESTAMP | Order time |

### **order_items Table**
| Column | Type | Description |
|--------|------|-------------|
| id | BIGINT | Primary key |
| order_id | BIGINT | FK → orders |
| item_name | VARCHAR | Menu item |
| quantity | INT | Item count |
| unit_price | DECIMAL | Price per item |

**Design Choice:** Store item names as snapshots for historical accuracy

---

## SLIDE 5: Order Status Workflow

### **5 States of an Order**

```
1. CREATED     → Customer places order
      ↓
2. CONFIRMED   → Staff acknowledges
      ↓
3. PREPARING   → Kitchen is cooking
      ↓
4. READY       → Food is ready
      ↓
5. SERVED      → Delivered to customer (FINAL)
```

**Business Rule:** Only valid transitions allowed  
*Example:* Cannot jump from CREATED → SERVED

---

## SLIDE 6: API Endpoints (6 Total)

### **Create Order**
```
POST /api/orders
Headers: X-User-Id, X-Table-Id, Authorization
Response: Order with items
```

### **Query Orders**
```
GET /api/orders/{id}        - Single order
GET /api/orders/user        - User's order history
GET /api/orders/table       - Table's orders
GET /api/orders/active      - Kitchen view
```

### **Update Status**
```
PATCH /api/orders/{id}/status
Body: { "status": "PREPARING" }
```

---

## SLIDE 7: Cart Integration (Key Feature)

### **The Problem**
How do we know what the customer wants to order?

### **The Solution**
Fetch from Cart Service!

### **Order Creation Flow**
```
1. Customer → POST /api/orders
2. Order Service → GET /api/cart (fetch items)
3. Validate cart (not empty, valid items)
4. Create order with cart snapshot
5. Save to database
6. DELETE /api/cart (cleanup)
7. Return order to customer
```

**Smart Design:** If cart clearing fails, order still succeeds!

---

## SLIDE 8: Sample Request/Response

### **Request**
```http
POST http://localhost:8082/api/orders
Headers:
  X-User-Id: 42
  X-Table-Id: 5
  Authorization: Bearer eyJhbGc...
Body: {}
```

### **Response**
```json
{
  "id": 123,
  "userId": 42,
  "tableId": 5,
  "status": "CREATED",
  "totalAmount": 2400.00,
  "items": [
    {
      "itemName": "Margherita Pizza",
      "quantity": 2,
      "unitPrice": 1200.00
    }
  ],
  "createdAt": "2025-12-14T10:30:00"
}
```

---

## SLIDE 9: Error Handling

### **Types of Errors**

**400 Bad Request**
- Cart is empty
- Invalid cart items
- Invalid status transition

**404 Not Found**
- Order doesn't exist

**503 Service Unavailable**
- Cart Service down

### **Error Response Format**
```json
{
  "timestamp": "2025-12-14T10:30:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Cart is empty. Please add items..."
}
```

---

## SLIDE 10: Technology Stack

### **Backend**
- **Framework:** Spring Boot 4.0.0
- **Language:** Java 17
- **Build:** Maven

### **Database**
- **Type:** MySQL 8.0
- **ORM:** Hibernate + JPA
- **Schema:** Auto-generated via `ddl-auto: update`

### **Communication**
- **REST:** Spring Web
- **HTTP Client:** RestTemplate
- **Validation:** Jakarta Bean Validation

### **Security**
- **Auth:** JWT (via headers)
- **Validation:** Bean validation on all inputs

---

## SLIDE 11: Design Patterns Used

### **1. Repository Pattern**
Abstracts database operations
```java
OrderRepository extends JpaRepository<Order, Long>
```

### **2. Service Layer Pattern**
Business logic separated from controllers

### **3. DTO Pattern**
API responses differ from database entities

### **4. REST Client Pattern**
Reusable client for Cart Service

### **5. State Machine Pattern**
Order status transitions with validation

---

## SLIDE 12: Key Features Demonstrated

### **✅ Technical Skills**
- Microservices architecture
- REST API design
- Database design & ORM
- Service-to-service communication
- Transaction management
- Error handling

### **✅ Real-World Concepts**
- Cart-to-order conversion
- Order lifecycle management
- Historical data preservation
- Graceful failure handling

---

## SLIDE 13: Transaction Management

### **Why Transactions Matter**

**Scenario:** Creating an order with 5 items

**Without Transaction:**
- Item 1 saved ✅
- Item 2 saved ✅
- Item 3 fails ❌
- Result: **Incomplete order in database** 💥

**With @Transactional:**
- All items saved ✅ → COMMIT
- One item fails ❌ → ROLLBACK all
- Result: **All-or-nothing** ✅

**Our Implementation:** `@Transactional` on `createOrder()`

---

## SLIDE 14: Logging & Monitoring

### **What We Log**
```
INFO  - Creating order for userId: 42, tableId: 5
INFO  - Cart fetched - items: 2, total: 2400.00
INFO  - Order created - orderId: 123
INFO  - Cart cleared successfully
```

### **Error Logs**
```
ERROR - Cart Service error: 404 - Cart not found
WARN  - Failed to clear cart (non-critical)
```

### **Benefits**
- Debugging issues
- Tracking user behavior
- Performance monitoring

---

## SLIDE 15: Demo Scenarios

### **Scenario 1: Happy Path** 😊
1. Customer adds 2 pizzas to cart
2. Customer clicks "Place Order"
3. Order Service fetches cart
4. Order created with status CREATED
5. Cart automatically cleared
6. Customer sees order confirmation

### **Scenario 2: Empty Cart** 🛒❌
1. Customer has empty cart
2. Customer tries to order
3. System rejects: "Cart is empty"
4. Customer adds items first

### **Scenario 3: Kitchen View** 👨‍🍳
1. Kitchen staff opens active orders
2. Sees all PREPARING & READY orders
3. Updates status as food is cooked
4. Real-time updates to customers

---

## SLIDE 16: Business Value

### **For Customers**
- ✅ Seamless checkout from cart
- ✅ Order tracking
- ✅ Order history

### **For Restaurant Staff**
- ✅ Kitchen display of active orders
- ✅ Order status management
- ✅ Table-based order grouping

### **For Business**
- ✅ Data integrity (transactions)
- ✅ Historical order data
- ✅ Scalable microservice design

---

## SLIDE 17: Challenges & Solutions

### **Challenge 1: Cart-Order Synchronization**
**Problem:** What if cart service is down?  
**Solution:** Comprehensive error handling, informative messages

### **Challenge 2: Order Item Snapshots**
**Problem:** Menu prices change, but old orders should show old prices  
**Solution:** Store item_name and unit_price in order_items

### **Challenge 3: Status Transitions**
**Problem:** Staff might skip states accidentally  
**Solution:** Validate transitions in code (State Machine pattern)

---

## SLIDE 18: Testing Strategy

### **Unit Tests**
- Service methods
- Validation logic
- Status transitions

### **Integration Tests**
- Database operations
- REST endpoints

### **Manual Testing**
- Postman collections
- cURL commands
- Browser testing

**Test Coverage Goal:** 80%+

---

## SLIDE 19: Future Enhancements

### **Planned Features**
- 🔄 Order cancellation
- 💳 Payment integration
- 📧 Email notifications
- 📊 Order analytics dashboard
- 🕐 Scheduled orders
- 🔁 Order modification (before preparing)

### **Technical Improvements**
- 📈 Add metrics (Prometheus)
- 🔍 Distributed tracing (Zipkin)
- 💾 Add caching (Redis)
- 🔐 Enhanced security (OAuth2)

---

## SLIDE 20: Lessons Learned

### **What Worked Well**
✅ Clean architecture (Controller → Service → Repository)  
✅ DTO pattern prevented tight coupling  
✅ RestTemplate made external calls easy  
✅ Transaction management ensured data integrity

### **What We'd Improve**
🔄 Add retry logic for Cart Service calls  
🔄 Implement circuit breaker pattern  
🔄 Add more comprehensive error codes  
🔄 Create OpenAPI/Swagger documentation

---

## SLIDE 21: Code Quality

### **Best Practices Followed**
- ✅ Separation of concerns
- ✅ Dependency injection
- ✅ Logging at appropriate levels
- ✅ Meaningful variable/method names
- ✅ Input validation
- ✅ Exception handling

### **Code Metrics**
- Classes: 17
- Endpoints: 6
- Test Coverage: [Your coverage]%
- Build Time: ~3 seconds
- No compilation warnings ✅

---

## SLIDE 22: Deployment

### **Local Development**
```bash
mvn spring-boot:run
# Runs on http://localhost:8082
```

### **Production Deployment**
```bash
mvn clean package
java -jar target/order-service.jar
```

### **Docker**
```dockerfile
FROM openjdk:17-slim
COPY target/order-service.jar app.jar
ENTRYPOINT ["java","-jar","/app.jar"]
```

### **Environment Variables**
- `DB_URL` - MySQL connection
- `CART_SERVICE_URL` - Cart service endpoint

---

## SLIDE 23: Q&A - Common Questions

**Q: Why separate order-items table?**  
A: One-to-many relationship, allows multiple items per order

**Q: Why store item names/prices?**  
A: Historical accuracy - menu changes shouldn't affect past orders

**Q: What if cart service is down?**  
A: Order creation fails with 400 error, user retries later

**Q: Can customers modify orders?**  
A: Not yet - future enhancement

**Q: How do you prevent duplicate orders?**  
A: Each order creation clears cart automatically

---

## SLIDE 24: Summary

### **Order Service in 3 Points**

1️⃣ **Converts cart into orders** with validation  
2️⃣ **Manages order lifecycle** through 5 states  
3️⃣ **Provides multiple views** for customers and staff  

### **Technologies Demonstrated**
Spring Boot • MySQL • REST APIs • Microservices • JWT • Transactions

### **Key Achievement**
✅ Production-ready service with 0 compilation errors

---

## SLIDE 25: Thank You!

### **Order Service**
*Powering seamless restaurant operations* 🍽️

**Service URL:** `http://localhost:8082/api/orders`

**Documentation:**
- `PRESENTATION_OVERVIEW.md` - Detailed guide
- `CART_INTEGRATION_DOCS.md` - Integration details
- `ORDER_SERVICE_README.md` - Setup instructions

**Questions?**

---

*End of Presentation*

