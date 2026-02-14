# Order Service - Implementation Summary

## ✅ Implementation Complete - BUILD SUCCESS! 🎉

All components have been successfully implemented inside the `com.example.order_service` package.
**Maven compilation successful - all 13 source files compiled without errors.**

---

## 📁 Project Structure

```
com.example.order_service/
├── OrderServiceApplication.java          # Main Spring Boot application
├── controller/
│   └── OrderController.java             # REST API endpoints
├── service/
│   └── OrderService.java                # Business logic layer
├── repository/
│   └── OrderRepository.java             # Data access layer
├── entity/
│   ├── Order.java                       # Order entity with JPA mappings
│   └── OrderItem.java                   # Order item entity
├── dto/
│   ├── CreateOrderRequest.java          # Request DTO for creating orders
│   ├── OrderItemRequest.java            # Request DTO for order items
│   ├── OrderResponse.java               # Response DTO
│   └── UpdateOrderStatusRequest.java    # Request DTO for status updates
└── exception/
    ├── BadRequestException.java         # Custom exception for bad requests
    ├── ResourceNotFoundException.java   # Custom exception for 404
    └── GlobalExceptionHandler.java      # @ControllerAdvice for error handling
```

---

## 🗄️ Database Schema

### Orders Table
```sql
orders (
  id              BIGINT PRIMARY KEY AUTO_INCREMENT,
  table_id        BIGINT NOT NULL,
  user_id         BIGINT NOT NULL,
  status          VARCHAR(50) NOT NULL,
  total_amount    DECIMAL(10,2),
  created_at      TIMESTAMP NOT NULL
)
```

### Order Items Table
```sql
order_items (
  id              BIGINT PRIMARY KEY AUTO_INCREMENT,
  order_id        BIGINT NOT NULL (FK -> orders.id),
  item_id         BIGINT NOT NULL,
  item_name       VARCHAR(255) NOT NULL,
  quantity        INT NOT NULL,
  unit_price      DECIMAL(10,2) NOT NULL
)
```

---

## 🎯 Entity Design

### Order Entity
- **Bidirectional relationship** with OrderItem (OneToMany)
- **Cascade ALL** operations to child items
- **Lazy fetching** for performance
- **Auto-generated timestamps** using @CreationTimestamp
- **Helper methods**:
  - `addItem()`: Maintains bidirectional relationship
  - `calculateTotalAmount()`: Calculates order total from items

### OrderItem Entity
- **ManyToOne** relationship with Order
- **@JsonIgnore** on order field to prevent circular serialization
- Immutable after creation (no setOrder exposed publicly)

---

## 🔄 Status Transition Logic

The service enforces valid status transitions:

```
CREATED → CONFIRMED or PREPARING
CONFIRMED → PREPARING
PREPARING → READY
READY → SERVED
SERVED → (no further transitions)
```

Any invalid transition throws `BadRequestException`.

---

## 📡 API Endpoints

### 1️⃣ Create Order
**POST** `/api/orders`

**Headers Required:**
- `X-User-Id`: User ID (provided by API Gateway from JWT)
- `X-Table-Id`: Table ID (provided by API Gateway from JWT)
- `Authorization`: Bearer token

**Request Body:** ⚠️ **OPTIONAL** (can be empty `{}` or omitted entirely)

The order items are fetched from the Cart Service automatically. `userId` and `tableId` come from request headers.

**Example Request (No Body):**
```http
POST /api/orders
Authorization: Bearer eyJhbGc...
X-User-Id: 42
X-Table-Id: 5

(empty body or {})
```

**Response:** `201 CREATED`
```json
{
  "id": 1,
  "tableId": 5,
  "userId": 42,
  "status": "CREATED",
  "totalAmount": 2400.00,
  "createdAt": "2025-01-01T10:00:00",
  "items": [...]
}
```

---

### 2️⃣ Get Order by ID
**GET** `/api/orders/{orderId}`

**Response:** `200 OK` or `404 NOT FOUND`

---

### 3️⃣ Get Orders by Table
**GET** `/api/orders/table/{tableId}`

Returns all orders for a table (sorted by createdAt DESC).

---

### 4️⃣ Get Orders by User
**GET** `/api/orders/user/{userId}`

Returns all orders for a user (sorted by createdAt DESC).
Used for order history in customer portal.

---

### 5️⃣ Get Active Orders (Kitchen View)
**GET** `/api/orders/active`

Returns orders with status: CREATED, CONFIRMED, or PREPARING.
Sorted by createdAt ASC (oldest first).

---

### 6️⃣ Update Order Status
**PATCH** `/api/orders/{orderId}/status`

**Request Body:**
```json
{
  "status": "PREPARING"
}
```

**Response:** `200 OK` with updated order, or `400 BAD REQUEST` if invalid transition.

---

## 🛡️ Error Handling

### Error Response Format
```json
{
  "timestamp": "2025-01-01T10:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Invalid status transition from READY to CREATED"
}
```

### Handled Exceptions
- **ResourceNotFoundException** → 404 NOT FOUND
- **BadRequestException** → 400 BAD REQUEST
- **MethodArgumentNotValidException** → 400 BAD REQUEST (validation errors)
- **Exception** → 500 INTERNAL SERVER ERROR

---

## 📝 Logging

### Logged Events
1. **Incoming requests**: Method, path, and key parameters
2. **Order creation**: orderId, userId, totalAmount
3. **Order retrieval**: orderId, userId
4. **Status changes**: orderId, userId, old status → new status
5. **Errors**: Full stack trace with context

### Log Levels
- **INFO**: Request/response flow, business operations
- **DEBUG**: Query details, found results count
- **ERROR**: Exceptions, validation failures

---

## ⚙️ Configuration (application.yml)

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/order_db?createDatabaseIfNotExist=true
    username: root
    password: root
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true

server:
  port: 8082

logging:
  level:
    com.example.order_service: DEBUG
    org.hibernate.SQL: DEBUG
```

---

## 🚀 Running the Application

### Prerequisites
- Java 17
- MySQL 8+ running on localhost:3306
- Maven (or use included mvnw)

### Steps

1. **Start MySQL** (ensure it's running)

2. **Compile the project:**
   ```bash
   ./mvnw clean compile
   ```

3. **Run the application:**
   ```bash
   ./mvnw spring-boot:run
   ```

4. **Test the API:**
   ```bash
   curl -X POST http://localhost:8082/api/orders \
     -H "Content-Type: application/json" \
     -d '{
       "tableId": 5,
       "userId": 1,
       "items": [{
         "itemId": 10,
         "itemName": "Pizza",
         "quantity": 2,
         "unitPrice": 1200
       }]
     }'
   ```

---

## ✨ Key Features Implemented

✅ **Mandatory userId field** in all orders  
✅ **Validation** on all request DTOs (Bean Validation)  
✅ **Transactional integrity** using @Transactional  
✅ **Status transition validation** with detailed error messages  
✅ **Comprehensive logging** at all layers  
✅ **DTO pattern** - entities never exposed directly  
✅ **Global exception handling** with @ControllerAdvice  
✅ **Lazy loading** with fetch strategies  
✅ **Bidirectional relationships** properly maintained  
✅ **RESTful API design** with proper HTTP status codes

---

## 🎓 University Project Notes

This is a **demonstration project** for educational purposes:
- No authentication/security (will be handled by auth-service)
- No Kafka integration (simplified for demo)
- DDL auto-update enabled (not for production)
- Simple error messages (not internationalized)

---

## 📞 Support

For issues or questions about this implementation, refer to:
- Entity relationships in `Order.java` and `OrderItem.java`
- Business logic in `OrderService.java`
- API contracts in `OrderController.java`
- Error handling in `GlobalExceptionHandler.java`

**Implementation completed successfully!** 🎉

