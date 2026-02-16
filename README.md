# Order Service

Spring Boot microservice for managing restaurant orders with Azure Container Apps deployment support.

## Features

- RESTful API for order management
- MySQL database integration
- Cart service integration
- Health checks and monitoring
- Docker containerization
- CI/CD with GitHub Actions

## Prerequisites

- Java 17
- Maven 3.9+
- MySQL 8.0+
- Docker (optional)

## Local Development

### Environment Variables

```bash
DATABASE_URL=jdbc:mysql://localhost:3306/orderdb
DATABASE_USERNAME=root
DATABASE_PASSWORD=password
CART_SERVICE_URL=http://localhost:8080/api/cart
```

### Run Application

```bash
# Development mode
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# Build
mvn clean package

# Run JAR
java -jar target/order-service-0.0.1-SNAPSHOT.jar
```

### Run with Docker

```bash
docker build -t order-service .
docker run -p 8083:8083 \
  -e DATABASE_URL=jdbc:mysql://host.docker.internal:3306/orderdb \
  -e DATABASE_USERNAME=root \
  -e DATABASE_PASSWORD=password \
  order-service
```

### Load Synthetic Data

For testing and development, you can load 2 months of synthetic order data:

```bash
# In MySQL Workbench:
# 1. Open synthetic_data.sql
# 2. Select 'orderdb' database
# 3. Press Ctrl+Shift+Enter to execute

# Or via command line:
mysql -u root -p orderdb < synthetic_data.sql
```

**Synthetic Data Includes:**
- 324 orders (Dec 16, 2025 - Feb 15, 2026)
- 304 completed (SERVED) orders
- 20 active orders (CREATED, CONFIRMED, PREPARING, READY)
- ~1,400 order items
- Realistic menu items, quantities, and prices

## API Endpoints

### Health
- `GET /actuator/health` - Health check
- `GET /actuator/health/liveness` - Liveness probe
- `GET /actuator/health/readiness` - Readiness probe

### Orders
- `POST /api/orders` - Create order
- `GET /api/orders/{id}` - Get order by ID
- `GET /api/orders/table` - Get orders by table
- `GET /api/orders/user` - Get orders by user
- `GET /api/orders/active` - Get active orders
- `PATCH /api/orders/{id}/status` - Update order status

## Configuration

### Profiles

- **Default** - Base configuration
- **dev** - Development (verbose logging, auto-update schema)
- **prod** - Production (optimized, minimal logging)

### Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `DATABASE_URL` | `jdbc:mysql://localhost:3306/orderdb` | Database connection URL |
| `DATABASE_USERNAME` | `root` | Database username |
| `DATABASE_PASSWORD` | `password` | Database password |
| `CART_SERVICE_URL` | `http://localhost:8080/api/cart` | Cart service URL |
| `PORT` | `8083` | Server port |
| `SPRING_PROFILES_ACTIVE` | - | Active profile (dev/prod) |

## Azure Deployment

### Required GitHub Secrets

| Secret | Description |
|--------|-------------|
| `AZURE_CREDENTIALS` | Service principal JSON |
| `AZURE_CONTAINER_REGISTRY` | ACR login server (e.g., `myacr.azurecr.io`) |
| `ACR_USERNAME` | ACR username |
| `ACR_PASSWORD` | ACR password |
| `AZURE_RESOURCE_GROUP` | Resource group name |

**Note:** Environment variables (database credentials, service URLs, etc.) should be configured directly in Azure Container Apps settings, not in GitHub Secrets.

### Setup Azure Resources

```bash
# Create resource group
az group create --name order-service-rg --location eastus

# Create container registry
az acr create --resource-group order-service-rg --name orderacr --sku Basic --admin-enabled true

# Create container apps environment
az containerapp env create --name order-env --resource-group order-service-rg --location eastus

# Create MySQL database
az mysql flexible-server create \
  --resource-group order-service-rg \
  --name order-mysql \
  --admin-user adminuser \
  --admin-password <password> \
  --sku-name Standard_B1ms
```

### Deploy

1. Configure GitHub Secrets (see table above)
2. Configure environment variables in Azure Container Apps:
   - `SPRING_PROFILES_ACTIVE=prod`
   - `DATABASE_URL`
   - `DATABASE_USERNAME`
   - `DATABASE_PASSWORD`
   - `CART_SERVICE_URL`
3. Push to `main` branch

GitHub Actions will automatically:
- Build and test
- Create Docker image
- Push to Azure Container Registry
- Deploy to Azure Container Apps

### Manual Deployment

```bash
# Build image
docker build -t <acr-name>.azurecr.io/order-service:latest .

# Push to ACR
az acr login --name <acr-name>
docker push <acr-name>.azurecr.io/order-service:latest

# Deploy to Container Apps
az containerapp create \
  --name order-service \
  --resource-group order-service-rg \
  --environment order-env \
  --image <acr-name>.azurecr.io/order-service:latest \
  --target-port 8083 \
  --ingress external \
  --env-vars \
    SPRING_PROFILES_ACTIVE=prod \
    DATABASE_URL=<db-url> \
    DATABASE_USERNAME=<username> \
    DATABASE_PASSWORD=<password>
```

## Troubleshooting

### Check Logs

```bash
az containerapp logs show --name order-service --resource-group order-service-rg --follow
```

### Test Health Endpoint

```bash
curl https://<app-url>/actuator/health
```

### Database Connection Issues

Verify firewall rules allow Azure services:
```bash
az mysql flexible-server firewall-rule create \
  --resource-group order-service-rg \
  --name order-mysql \
  --rule-name AllowAzure \
  --start-ip-address 0.0.0.0 \
  --end-ip-address 0.0.0.0
```

## License

MIT License



