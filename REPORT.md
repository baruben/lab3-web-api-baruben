# Lab 3 Complete a Web API -- Project Report

## Description of Changes
### 1. Complete and verify tests
- Developed and completed the test suite for the EmployeeController to verify HTTP method semantics, including safety and idempotency.

### 2. Replace with Spring WebFlux Reactive Implementation
- Created a reactive version of the EmployeeController using Spring WebFlux and R2DBC, with a wrapper service for the blocking controller that simulates latency using `.block()`.
- Reused the same tests from the blocking controller to ensure the reactive implementation behaves consistently.

### 3. Implement RESTful API Documentation with OpenAPI/Swagger
- Integrated OpenAPI/Swagger documentation using springdoc-openapi, providing detailed REST endpoint descriptions, request/response examples, and error scenarios.

### 4. Add load tests with Gatling
> - Tried to Developed Gatling simulations to compare blocking and reactive implementations.
> - Results were inconclusive due to both implementations using an in-memory R2DBC database, limiting realistic concurrency and performance testing.
> - Thus, attempts to simulate realistic database latency using `Thread.sleep` and `.delayElement()` were not included in the repository.


## Technical Decisions

### 1. Reactive vs. Blocking Design
- The reactive controller uses `Flux` and `Mono` types to handle asynchronous, non-blocking requests.
- The blocking controller wraps reactive repository calls with `.block()` to simulate a synchronous, thread-blocking design.
- Artificial delays (`Thread.sleep()` and `.delayElement()`) were attempted to highlight behavioral differences under load.

### 2. Database
- Both controllers use `R2DBC` with `H2` for simplicity.
- Although `R2DBC` is non-blocking, the blocking version forces synchronous execution.
- A real reactive database (like `PostgreSQL` R2DBC) would yield clearer performance differences.

### 3. API Documentation
- Adopted SpringDoc `OpenAPI` for automated generation of Swagger UI since it had already been used in past laboratory sessions.
- Added endpoint-level annotations with:
  - `@Operation`, `@ApiResponse`, and `@Parameter`
  - Example request/response payloads
  - Status codes for success and error scenarios (`200`, `201`, `404`, `204`)

### 4. Load Testing
- Created Gatling simulations for both controllers.
- Each virtual user performs create, read-all, and read-by-id operations.
- The reactive simulation showed higher `503` errors and latency, likely due to:
  - In-memory database limitations
  - Thread starvation under artificial delays
- Future tests should use a real database to compare throughput more accurately.


## Learning Outcomes
Through this assignment, I learned:
- How to design and document RESTful APIs following HTTP method semantics.
- The difference between blocking and reactive data access patterns.
- How to run Gatling load tests and interpret performance metrics (latency, throughput, errors).
- That reactive programming benefits depend heavily on I/O-bound operations and non-blocking infrastructure — not just using reactive APIs.


## AI Disclosure
### AI Tools Used
- ChatGPT (GPT-5)

### AI-Assisted Work
- Assisted in generating OpenAPI documentation annotations.
- Helped design and write Gatling simulation scripts.
- Provided guidance on Spring Boot configuration and R2DBC reactive patterns.

**Estimated AI assistance:** around 50% of total work.

**Modifications:** I adapted, reviewed, and integrated the AI suggestions into my own code and report to ensure correctness and alignment with my assignment.

### Original Work
- Implemented reactive controller.
- Created and debugged Gatling simulations and repository wrappers.
- Wrote and ran tests.
- Analyzed performance results and documented findings.