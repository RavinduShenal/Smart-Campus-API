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

---

## Conceptual Report (Question Answers)

### Part 1.1 — JAX-RS Resource Lifecycle

By default, JAX-RS creates a **new instance of each resource class for every incoming HTTP request** (per-request lifecycle). This is the default behaviour specified by the JAX-RS specification (JSR 370). The runtime instantiates the resource object, handles the request, then discards the object.

This has a direct implication for state management: because each request gets its own object, **instance variables cannot be used to share data between requests**. Any data stored as an instance field would be lost the moment the request completes. To persist data across requests, shared state must be stored in a static or application-scoped structure. In this project, `DataStore` uses `public static` maps (`HashMap<String, Room>`, `HashMap<String, Sensor>`, etc.) which live at the class level and survive across multiple request instances. In a production system with concurrent requests, these structures would need synchronisation (e.g., `ConcurrentHashMap`) to prevent race conditions.

---

### Part 1.2 — HATEOAS (Hypermedia as the Engine of Application State)

HATEOAS is considered a hallmark of mature RESTful design because it makes an API **self-describing and navigable**. Rather than forcing clients to rely solely on external documentation to know which URLs to call, a HATEOAS-compliant API embeds hyperlinks inside its responses that tell the client what actions are available next and where to find related resources.

For client developers, this offers several advantages. First, it reduces tight coupling between client and server: if a URL changes, the server updates the link in its response rather than requiring every client to be updated. Second, it lowers the barrier to discovering available operations, since responses themselves act as a guide. Third, it aligns with how the web itself works — browsers navigate the web through hyperlinks embedded in HTML without needing to know every URL in advance. In this API, the discovery endpoint returns `_links` pointing to `/api/v1/rooms` and `/api/v1/sensors`, enabling clients to start at one known entry point and explore from there.

---

### Part 2.1 — Returning IDs vs Full Room Objects

When a client requests a list of rooms, the API can return either only the IDs of each room, or the full room objects. Returning only IDs keeps responses small and reduces **network bandwidth**, which is beneficial when the list is large and the client only needs to identify which rooms exist. However, it forces the client to make additional HTTP requests to fetch the details of each room it actually needs, increasing **latency and round-trips**.

Returning full objects is more network-intensive upfront but eliminates those follow-up requests, reducing overall latency and simplifying client logic. This trade-off depends on usage patterns: if clients almost always need room details, full objects are preferable. If clients only need a subset, returning IDs with an option to expand (or supporting pagination and field projection) is better. This API returns full objects for simplicity, which is appropriate for a campus-scale deployment where room lists are not excessively large.

---

### Part 2.2 — Idempotency of DELETE

The DELETE operation is **idempotent by definition** in HTTP: multiple identical requests should have the same effect as a single one. In this implementation, the first DELETE on a room that exists (and has no sensors) removes it from the `DataStore`. A second identical DELETE request will find no room with that ID, and `DataStore.rooms.remove(id)` will simply return `null` without error. The service returns `204 No Content` in both cases — whether the room was actually removed or was already absent. Therefore, the outcome is the same regardless of how many times the request is sent, satisfying idempotency. The only exception is if the room still has sensors attached, in which case the first attempt returns `409 Conflict` (and the room remains).

---

### Part 3.1 — @Consumes and Content-Type Mismatch

The `@Consumes(MediaType.APPLICATION_JSON)` annotation declares that the POST endpoint **only accepts requests with a `Content-Type: application/json` header**. If a client sends data with a different format — for example, `text/plain` or `application/xml` — JAX-RS will reject the request before it even reaches the resource method. The runtime returns an **HTTP 415 Unsupported Media Type** response automatically, without any custom handling needed. This protects the API from receiving malformed or unexpected data formats. The resource method body is never executed in such cases, so there is no risk of a `ClassCastException` or deserialization error within the application logic.

---

### Part 3.2 — @QueryParam vs Path Segment for Filtering

Using `@QueryParam("type")` (e.g., `GET /api/v1/sensors?type=CO2`) is considered superior to embedding the filter value in the path (e.g., `/api/v1/sensors/type/CO2`) for several reasons.

**Semantics:** A path segment implies a distinct resource identity. `/api/v1/sensors/CO2` suggests there is a resource literally *called* "CO2" rather than a filtered view of the sensor collection. Query parameters correctly convey that the result is a **filtered projection** of the collection, not a separate resource.

**Flexibility:** Query parameters can be combined easily (`?type=CO2&status=ACTIVE`) without requiring new path definitions for every combination. Path-based filters would require exponentially more route definitions.

**Cacheability and REST conventions:** Collection resources at a stable path (`/api/v1/sensors`) can be cached by intermediaries. Filtering is an operation on that collection, naturally expressed via query strings. This is consistent with how search engines and REST APIs across the industry handle filtering.

---

### Part 4.1 — Sub-Resource Locator Pattern

The Sub-Resource Locator pattern allows a resource method to **delegate request handling to another class** rather than returning a response directly. In this API, `SensorResource` has a method annotated with `@Path("/{sensorId}/readings")` that returns an instance of `SensorReadingResource`. JAX-RS then continues matching the remainder of the URL and dispatches to the appropriate method in the returned object.

The architectural benefit is **separation of concerns and manageability**. Without this pattern, all logic for sensors and their readings would be crammed into one massive resource class, making it hard to read, test, and maintain. By delegating to `SensorReadingResource`, the reading-related logic (posting new readings, retrieving history, updating the parent sensor's current value) is encapsulated in its own class. As an API grows to include dozens of nested resources, this keeps each class focused on a single responsibility, consistent with the Single Responsibility Principle.

---

### Part 5.2 — HTTP 422 vs 404 for Missing Referenced Resources

When a client posts a new sensor referencing a `roomId` that does not exist, a **404 Not Found** would be misleading because the requested endpoint (`/api/v1/sensors`) *does* exist and was found successfully. The problem is not with the endpoint — it is with the **semantic validity of the request payload**: the body is syntactically correct JSON, but it contains a reference to a resource that does not exist in the system.

**HTTP 422 Unprocessable Entity** is more semantically accurate because it signals that the server understood the request format and the content type, but the **business logic validation failed** — specifically, the referenced entity (`roomId`) cannot be resolved. It communicates "your JSON is valid, but the data inside it is logically invalid in this context." This gives client developers a more actionable error signal than a generic 404, helping them distinguish between a wrong URL and invalid foreign key data.

---

### Part 5.4 — Security Risks of Exposing Stack Traces

Exposing raw Java stack traces in API responses poses significant cybersecurity risks:

1. **Technology fingerprinting:** A stack trace reveals the exact server-side framework (e.g., Jersey, Grizzly), Java version, and library versions being used. An attacker can use this to look up known CVEs for those specific versions and craft targeted exploits.

2. **Internal path disclosure:** Stack traces often contain fully qualified class names and file paths (e.g., `org.westminster.api.data.DataStore.java:42`), revealing the internal package structure of the application. This information aids reverse engineering.

3. **Logic and data structure exposure:** Method names and class hierarchies in a trace can reveal how the application processes data — for example, exposing that an in-memory HashMap is used, or the names of methods that handle authentication — which could help an attacker identify injection points.

The `GlobalExceptionMapper` in this project intercepts all uncaught `Throwable` errors, logs the stack trace server-side (visible only to developers), and returns a safe, generic `500 Internal Server Error` JSON message to the client — exposing nothing about internal implementation.

---

### Part 5.5 — JAX-RS Filters for Cross-Cutting Concerns

JAX-RS filters are preferred over inserting `Logger.info()` statements in every resource method for several reasons:

**DRY (Don't Repeat Yourself):** Logging is a cross-cutting concern — it applies to every endpoint. Embedding log statements in each resource method duplicates code across dozens of methods and creates maintenance overhead.

**Separation of concerns:** Resource methods should focus exclusively on business logic. Mixing logging, authentication, or metrics tracking into resource code violates the Single Responsibility Principle and makes the codebase harder to reason about.

**Consistency:** A filter guarantees that *every* request and response is logged, even if a developer forgets to add a log statement to a newly created endpoint. Manual insertion is error-prone.

**Pluggability:** Filters can be added or removed without touching any resource class. If logging requirements change (e.g., switching from `java.util.logging` to SLF4J), only the filter class needs to be updated.

---

*Report prepared by GPRS_Tharuka — w2119843@westminster.ac.uk*
