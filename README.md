# Microfinance Management System — Backend

Spring Boot 3 / Java 21 REST API for the billing collection management system: member records, weekly payment entry, automatic remaining/credit calculation, reports, and dashboard analytics.

This is phase 1 of the project (backend only). The React frontend and a dedicated MySQL schema script will follow as separate phases.

## Tech stack

- Java 21, Spring Boot 3.3
- Spring Security + JWT (jjwt 0.12)
- Spring Data JPA / Hibernate
- MySQL 8
- Lombok
- springdoc-openapi (Swagger UI)

## Project layout

```
src/main/java/com/microfinance/
  config/        Security and OpenAPI configuration
  security/      JWT filter, util, entry point, user details service
  entity/        JPA entities (Admin, Member, Payment, WeeklyCollection) + enums
  dto/           Request/response DTOs
  mapper/        Entity <-> DTO mapping
  repository/    Spring Data JPA repositories
  service/       Interfaces + impl/ business logic
  controller/    REST controllers
  exception/     Custom exceptions + global handler
  util/          ID/receipt generators, date helpers
```

## 1. Prerequisites

- JDK 21 ([Adoptium](https://adoptium.net/))
- Maven 3.9+ (or use the wrapper if you add one)
- MySQL 8 running locally or accessible remotely
- An IDE — VS Code (with the "Extension Pack for Java" + "Spring Boot Extension Pack"), IntelliJ, or Eclipse

## 2. Configure MySQL

Create the database:

```sql
CREATE DATABASE microfinance_db CHARACTER SET utf8mb4;
```

Open `src/main/resources/application.properties` and update:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/microfinance_db?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
spring.datasource.username=root
spring.datasource.password=your_mysql_password
```

Tables are created automatically on first run (`spring.jpa.hibernate.ddl-auto=update`) and a default admin is seeded by `data.sql`:

```
username: admin
password: admin123
```

Change this password (or remove the `data.sql` insert) before any real deployment — `ddl-auto=update` and a placeholder password are dev-only conveniences.

Also replace `jwt.secret` in `application.properties` with your own random 32+ character string.

## 3. Import into VS Code

1. Open VS Code → File → Open Folder → select `microfinance-backend`.
2. Install the **Extension Pack for Java** and **Spring Boot Extension Pack** if prompted.
3. VS Code will detect the `pom.xml` and resolve dependencies automatically (first run downloads from Maven Central, so you need internet access).
4. Once indexing finishes, open `MicrofinanceApplication.java` and click **Run** above the `main` method, or use the Maven command below.

## 4. Run the backend

```bash
cd microfinance-backend
mvn spring-boot:run
```

Or build a runnable jar:

```bash
mvn clean package
java -jar target/microfinance-backend.jar
```

The API starts on `http://localhost:8080`.

## 5. Explore the API with Swagger

Visit `http://localhost:8080/swagger-ui.html`. Authenticate first via `POST /api/auth/login`, copy the returned token, then click **Authorize** in Swagger UI and paste `Bearer <token>`.

## 6. Test with Postman

1. **Login** — `POST http://localhost:8080/api/auth/login`
   Body (JSON): `{"username": "admin", "password": "admin123"}`
   Copy `data.token` from the response.
2. For every other request, add header: `Authorization: Bearer <token>`.
3. **Create a member** — `POST /api/members`
   ```json
   {
     "name": "Lakshmi Nair",
     "phoneNumber": "9876543210",
     "address": "12 Main Street, Colachel",
     "weeklyAmount": 500,
     "joinDate": "2026-01-05",
     "notes": "Referred by Anitha"
   }
   ```
4. **Record a payment** — `POST /api/payments`
   ```json
   {
     "memberId": 1,
     "weekNumber": 26,
     "paymentYear": 2026,
     "amountPaid": 300,
     "paymentDate": "2026-06-29",
     "paymentMethod": "CASH",
     "remarks": "Partial payment"
   }
   ```
   The response includes the auto-calculated `remainingAmount` (200) and `status` (PARTIAL).
5. **Dashboard cards** — `GET /api/dashboard/summary`
6. **Reports** — `GET /api/reports/weekly`, `/api/reports/monthly?month=6&year=2026`, `/api/reports/yearly?year=2026`

## Key business rules implemented

- **Remaining amount**: `max(weeklyAmount - amountPaid, 0)`.
- **Extra/credit**: `max(amountPaid - weeklyAmount, 0)`, accumulated on `Member.creditBalance` and reconciled automatically on payment edit/delete.
- **Duplicate payment guard**: one payment per member per `(weekNumber, paymentYear)` — attempting a second `POST` for the same week returns `409 Conflict` with a message pointing to the edit endpoint instead.
- **Online payments require a UPI transaction ID** — enforced server-side regardless of what the frontend sends.
- **Auto-generated IDs**: `memberCode` (`MEM0001…`) and `receiptNumber` (`RCPT-2026-000123`), both with collision-retry against the unique DB constraint.
- **WeeklyCollection** is a recomputed cache, refreshed every time a payment in that week is added/edited/deleted, so dashboard reads don't re-aggregate the full payments table.

## Common errors

| Symptom | Fix |
|---|---|
| `Communications link failure` on startup | MySQL isn't running, or the URL/port in `application.properties` is wrong |
| `Access denied for user 'root'@'localhost'` | Wrong `spring.datasource.password` |
| `401 Unauthorized` on every request after login | Missing `Bearer ` prefix on the `Authorization` header, or the token expired (default 24h) |
| `JJWT` weak-key error on startup | `jwt.secret` is shorter than 32 characters |
| `409 Conflict` on `POST /api/payments` | A payment already exists for that member/week/year — use `PUT /api/payments/{id}` instead |

## What's next

- **Database schema phase**: a standalone `schema.sql` + realistic sample data set, independent of `ddl-auto`.
- **Frontend phase**: React + Vite + MUI app wired to these exact endpoints.
- **Production build / deployment guide**: covered once the frontend exists, so both halves can be deployed together.
