# Smart Campus Sensor & Room Management API

**Module:** 5COSC022W – Client-Server Architectures  
**Student:** GPRS_Tharuka | w2119843@westminster.ac.uk  
**Technology Stack:** Java · JAX-RS (Jersey) · Maven · Grizzly HTTP Server

---

## API Overview

This project implements a RESTful API for the University of Westminster's "Smart Campus" initiative. The API manages **Rooms** and the **Sensors** deployed within them, including historical **Sensor Readings**. It is built entirely using JAX-RS (Jersey) with an in-memory data store (HashMap / ArrayList) and follows REST best practices including proper HTTP status codes, JSON responses, and a versioned resource hierarchy.

### Base URL

```
http://localhost:8080/api/v1
```

### Resource Hierarchy

```
/api/v1                          → Discovery endpoint
/api/v1/rooms                    → Room collection
/api/v1/rooms/{id}               → Individual room (DELETE)
/api/v1/sensors                  → Sensor collection (GET, POST, ?type=)
/api/v1/sensors/{id}/readings    → Sub-resource: sensor readings (GET, POST)
```

---

## Project Structure

```
SmartCampusAPI/
├── pom.xml
└── src/main/java/org/westminster/api/
    ├── config/
    │   └── SmartCampusApplication.java     # JAX-RS application config
    ├── data/
    │   └── DataStore.java                  # In-memory data store
    ├── model/
    │   ├── Room.java
    │   ├── Sensor.java
    │   └── SensorReading.java
    ├── resource/
    │   ├── DiscoveryResource.java          # GET /api/v1
    │   ├── SensorRoomResource.java         # /api/v1/rooms
    │   ├── SensorResource.java             # /api/v1/sensors
    │   └── SensorReadingResource.java      # /api/v1/sensors/{id}/readings
    ├── exception/
    │   ├── RoomNotEmptyException.java
    │   ├── LinkedResourceNotFoundException.java
    │   ├── SensorUnavailableException.java
    │   ├── ConflictMapper.java             # 409
    │   ├── DependencyMapper.java           # 422
    │   ├── ForbiddenMapper.java            # 403
    │   └── GlobalExceptionMapper.java      # 500
    └── filter/
        └── SmartCampusLoggingFilter.java   # Request/response logging
```

---

## How to Build & Run

### Prerequisites

- Java 11+
- Apache Maven 3.6+

### Steps

```bash
# 1. Clone the repository
git clone https://github.com/RavinduShenal/smart-campus-api.git
cd smart-campus-api

# 2. Build the project
mvn clean package

# 3. Run the server
mvn exec:java

# Server starts at http://localhost:8080/api/v1
```

---

## Sample curl Commands

### 1. Discovery Endpoint
```bash
curl -X GET http://localhost:8080/SmartCampusAPII/api/v1
```
**Expected:** `200 OK` with version info and resource links.

---

### 2. Create a Room
```bash
curl -X POST http://localhost:8080/SmartCampusAPII/api/v1/rooms \
  -H "Content-Type: application/json" \
  -d '{"id":"LIB-301","name":"Library Quiet Study","capacity":50}'
```
**Expected:** `201 Created` with the room object.

---

### 3. Create a Sensor (linked to a room)
```bash
curl -X POST http://localhost:8080/SmartCampusAPII/api/v1/sensors \
  -H "Content-Type: application/json" \
  -d '{"id":"TEMP-001","type":"Temperature","status":"ACTIVE","currentValue":22.5,"roomId":"LIB-301"}'
```
**Expected:** `201 Created` with the sensor object.

---

### 4. Post a Sensor Reading
```bash
curl -X POST http://localhost:8080/SmartCampusAPII/api/v1/sensors/TEMP-001/readings \
  -H "Content-Type: application/json" \
  -d '{"id":"READ-001","timestamp":1714000000000,"value":23.1}'
```
**Expected:** `201 Created` with the reading object. Also updates the sensor's `currentValue`.

---

### 5. Get All Sensors Filtered by Type
```bash
curl -X GET "http://localhost:8080/SmartCampusAPII/api/v1/sensors?type=Temperature"
```
**Expected:** `200 OK` with a filtered list of temperature sensors.

---

### 6. Attempt to Delete a Room That Has Sensors (409 Error)
```bash
curl -X DELETE http://localhost:8080/SmartCampusAPII/api/v1/rooms/LIB-301
```
**Expected:** `409 Conflict` — room still has sensors attached.

---

### 7. Attempt to Add a Sensor with Non-Existent Room (422 Error)
```bash
curl -X POST http://localhost:8080/SmartCampusAPII/api/v1/sensors \
  -H "Content-Type: application/json" \
  -d '{"id":"CO2-001","type":"CO2","status":"ACTIVE","currentValue":400,"roomId":"FAKE-ROOM"}'
```
**Expected:** `422 Unprocessable Entity` — referenced room does not exist.


## Table of Contents

- [Part 1: Service Architecture & Setup](#part-1-service-architecture--setup)
- [Part 2: Room Management](#part-2-room-management)
- [Part 3: Sensor Operations & Linking](#part-3-sensor-operations--linking)
- [Part 4: Deep Nesting with Sub-Resources](#part-4-deep-nesting-with-sub-resources)
- [Part 5: Error Handling & Logging](#part-5-error-handling--logging)

---

## Part 1: Service Architecture & Setup

### Q: What is the default lifecycle of a JAX-RS Resource class?

By default, JAX-RS resources are **request-scoped** — a new instance is created for every incoming request. To prevent data loss between these transient instances, the `DataStore` is implemented using **static `ConcurrentHashMap` structures**.

This architectural choice ensures that:
- Data is treated as a **singleton**, surviving the resource instance's destruction
- Thread synchronization is handled automatically, preventing **race conditions** during concurrent updates

---

### Q: Why is Hypermedia (HATEOAS) considered a hallmark of advanced RESTful design?

Providing links in the Discovery endpoint makes the API **self-descriptive**. Compared to static documentation, this benefits client developers by:

- Allowing the client to **navigate the API dynamically**
- Ensuring the API remains **evolvable** — if a URI changes, clients relying on links do not break

---

## Part 2: Room Management

### Q: What are the implications of returning only IDs versus full room objects?

| Approach | Pros | Cons |
|---|---|---|
| **IDs only** | Minimises network bandwidth; ideal for mobile | Requires multiple round-trips (N+1 problem) |
| **Full objects** | Reduces round-trips; simpler client processing | Higher initial payload size |

---

### Q: Is the DELETE operation idempotent in this implementation?

**Yes.** The implementation is idempotent:

- **First DELETE** → removes the resource and returns `204 No Content`
- **Subsequent identical DELETEs** → return `404 Not Found`

Because the final state of the server is identical (the room remains deleted) regardless of how many times the request is sent, it adheres to **idempotency principles**.

---

## Part 3: Sensor Operations & Linking

### Q: What happens if a client sends data in a format other than `application/json`?

The `@Consumes(MediaType.APPLICATION_JSON)` annotation enforces **Content Negotiation**. If a client sends data as `text/plain` or `application/xml`:

1. JAX-RS **intercepts the request** before it reaches any method logic
2. The runtime returns **`HTTP 415 Unsupported Media Type`**
3. Only valid JSON is ever processed

---

### Q: Why is `@QueryParam` preferred over path-based filtering (e.g., `/sensors/type/CO2`)?

Using `@QueryParam` (e.g., `?type=CO2`) is the superior approach for filtering because:

- It correctly treats `CO2` as a **search criteria** of the sensor collection
- A path segment like `/sensors/type/CO2` implies `CO2` is a **unique resource/directory**, which is semantically incorrect
- Query parameters make it straightforward to **combine multiple filters** (e.g., `?type=CO2&status=active`) simultaneously

---

## Part 4: Deep Nesting with Sub-Resources

### Q: What are the architectural benefits of the Sub-Resource Locator pattern?

Delegating logic to a separate `SensorReadingResource` class provides several advantages:

- **Separation of concerns** — keeps "Sensor" logic and "Historical Data" logic distinct
- **Prevents a God Object** — the main controller stays lean and focused
- **Improved modularity** — each sub-resource class is independently unit-testable
- **Scalability** — large APIs remain manageable without a single massive controller class

---

## Part 5: Error Handling & Logging

### Q: Why is `HTTP 422` more semantically accurate than `404` for a missing reference inside a valid payload?

| Status Code | Meaning | When to use |
|---|---|---|
| `404 Not Found` | The **URI** does not point to a resource | Wrong endpoint path |
| `422 Unprocessable Entity` | The request body is syntactically correct but contains a **logical error** | e.g., a `roomId` that doesn't exist |

`422` is more descriptive for **business-logic failures**, as it signals the server understood the request but could not process its content.

---

### Q: What are the cybersecurity risks of exposing internal Java stack traces?

Exposing stack traces is a significant **security vulnerability**. An attacker can extract:

- Internal **file paths** and **package names**
- Specific **library versions**, enabling targeted CVE exploitation

A global `ExceptionMapper<Throwable>` should be used to intercept all unhandled exceptions, returning a sanitised error response and preventing **information leakage**.

---

### Q: Why use JAX-RS filters for logging instead of manual `Logger.info()` calls?

Using JAX-RS filters for cross-cutting concerns like logging is advantageous because:

- It **separates observability from business logic**, keeping resource methods clean
- It follows the **DRY (Don't Repeat Yourself)** principle — one filter handles all requests instead of duplicating logger statements across dozens of methods
- It ensures **consistent log output** and makes future changes easier to maintain

---

*Generated from the JAX-RS REST API Design Report.*
