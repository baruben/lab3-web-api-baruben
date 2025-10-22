# Lab 3 Complete a Web API -- Project Report

>##### Note on CI
>- The **CI.yaml pipeline may report a failure** due to `ktlintCheck` rejecting wildcard (`*`) imports used in the Gatling simulation files.
>- Despite this formatting warning, **all tests pass successfully**.


## Description of Changes
### 1. Complete and verify tests
- Developed and completed the test suite for the EmployeeController to verify HTTP method semantics, including safety and idempotency.

### 2. Replace with Spring WebFlux Reactive Implementation
- Created a reactive version of the EmployeeController using Spring WebFlux and R2DBC, with a wrapper service for the blocking controller that simulates latency using `.block()`.
- Reused the same tests from the blocking controller to ensure the reactive implementation behaves consistently.

### 3. Implement RESTful API Documentation with OpenAPI/Swagger
- Integrated OpenAPI/Swagger documentation using springdoc-openapi, providing detailed REST endpoint descriptions, request/response examples, and error scenarios.

### 4. Add load tests with Gatling
- Conducted **Gatling load tests** to compare the blocking and reactive implementations.
- The tests simulated **30,000 users ramped up over 60 seconds**. Attempts to increase the load caused the host system’s RAM to freeze.
- Each virtual user:
    1. Sends a **POST** request to create a new employee.
    2. Sends a **GET** request to retrieve the same employee by ID.
    - An intermediate “get all employees” step was initially included, but it triggered a **blocking database operation**, causing the simulation to fail under load.
- To run the **reactive implementation**, it is necessary to **comment out** the following dependency in `build.gradle.kts`:
  ```kotlin
  implementation(libs.spring.boot.starter.web)
  ```
  This ensures that the application uses **Netty** instead of **Tomcat**.  
  Conversely, the **blocking implementation** requires this dependency enabled to start the **Tomcat** server instead of Netty.

- Results were **not fully conclusive**, as both implementations produced comparable outcomes:
    - Approximately **17,000 OK** responses and **13,000 KO** responses.
    - However, the **reactive version peaked at 25,000 concurrent users**, whereas the **blocking version peaked at 19,500 concurrent users**.
- This suggests that while the reactive model handled higher concurrency, both were constrained by the in-memory R2DBC database and available system resources.

#### Gatling Results

| Metric | Blocking | Reactive |
|:--------|:----------:|:----------:|
| Peak concurrent users | 19,500 | **25,000** |
| OK responses | ~17,000 | ~17,000 |
| KO responses | ~13,000 | ~13,000 |
| Ramp-up duration | 60s | 60s |

##### Figures

- **Figure 1 – Blocking Implementation Load Test Result**  
  ![Blocking Load Test Result](images/Blocking.png)

- **Figure 2 – Reactive Implementation Load Test Result**  
  ![Reactive Load Test Result](images/Reactive.png)

## Technical Decisions

### 1. Reactive vs. Blocking Design
- The **reactive controller** uses `Mono` and `Flux` types to handle non-blocking, asynchronous operations.
- The **blocking controller** wraps reactive repository calls with `.block()` to force synchronous execution and simulate traditional servlet-style blocking behavior.
- Artificial delays (`Thread.sleep()` and `.delayElement()`) were tested to emphasize behavioral differences under load, though these were removed from the repository.

#### Justification for Return Types
- Reactive endpoints use `Mono<ResponseEntity<Employee>>` rather than `ResponseEntity<Mono<Employee>>` or `Mono<Employee>` for the following reasons:
    - Returning `ResponseEntity` **inside the `Mono`** allows asynchronous construction of the response **after** the repository operation completes.
    - In the case of **POST (create)** operations, the controller must **wait until the repository’s `.save()` returns the employee ID** before building the `ResponseEntity` with the correct `Location` header and response status.
    - This design preserves **reactive non-blocking flow**, while maintaining proper **HTTP semantics and response metadata**.


### 2. Database
- Both controllers use **R2DBC with H2** for simplicity.
- Although R2DBC is non-blocking, the blocking version forces synchronous execution.
- Using a **real reactive database** (e.g., PostgreSQL R2DBC) with a lot of data persisted would reveal clearer performance differences.

### 3. API Documentation
- Adopted **SpringDoc OpenAPI** for automated generation of Swagger UI.
- Annotated endpoints with:
    - `@Operation`, `@ApiResponse`, and `@Parameter`
    - Example request/response payloads
    - Status codes for success and error scenarios (`200`, `201`, `404`, `204`)

### 4. Load Testing
- Gatling simulations were designed to compare **blocking** vs. **reactive** performance under heavy load.
- While both showed similar aggregate results, the **reactive approach sustained more concurrent users**, indicating better scalability under high concurrency despite similar success ratios.
- Future tests should use a real database to compare throughput more accurately.


## Learning Outcomes
Through this assignment, I learned:
Through this assignment, I learned:
- How to design and document RESTful APIs that respect HTTP method semantics.
- The architectural and runtime differences between **blocking** and **reactive** data access patterns.
- How to run and interpret **Gatling** load tests, analyzing throughput, latency, and error distributions.
- That reactive programming’s benefits depend heavily on **non-blocking I/O** and infrastructure, not just reactive syntax.


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