package es.unizar.webeng.lab3

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.util.UriComponentsBuilder
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import io.swagger.v3.oas.annotations.parameters.RequestBody as OpenApiRequestBody

@RestController
@RequestMapping("/reactive")
@Tag(
    name = "Employee API (reactive)",
    description =
        "Reactive CRUD operations for Employee resources using Project Reactor (Flux/Mono). " +
            "Demonstrates RESTful design principles and non-blocking I/O.",
)
class ReactiveEmployeeController(
    private val repository: ReactiveEmployeeRepository,
) {
    @Operation(
        summary = "Get all employees (reactive)",
        description =
            "Returns a **Flux** stream of all employees. This is a **safe**, **idempotent**," +
                " and **cacheable** operation.",
    )
    @ApiResponse(
        responseCode = "200",
        description = "List of employees successfully retrieved",
        content = [
            Content(
                mediaType = "application/json",
                array = ArraySchema(schema = Schema(implementation = Employee::class)),
                examples = [
                    ExampleObject(
                        name = "EmployeeListReactive",
                        summary = "Example Flux<Employee> response",
                        value = """
                    [
                      {"id": 1, "name": "Alice", "role": "Developer"},
                      {"id": 2, "name": "Bob", "role": "Manager"},
                      {"id": 3, "name": "Carol", "role": "Designer"}
                    ]
                """,
                    ),
                ],
            ),
        ],
    )
    @GetMapping("/employees")
    fun all(): Flux<Employee> = repository.findAll()

    @Operation(
        summary = "Create a new employee (reactive)",
        description =
            "Adds a new employee asynchronously. This is an **unsafe** operation because it" +
                " modifies the server state.",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "201",
                description = "Employee successfully created",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = Employee::class),
                        examples = [
                            ExampleObject(
                                name = "CreatedEmployeeReactive",
                                summary = "Example created employee response",
                                value = """{"id": 5, "name": "Alice", "role": "Developer"}""",
                            ),
                        ],
                    ),
                ],
            ),
            ApiResponse(responseCode = "400", description = "Invalid input", content = [Content()]),
        ],
    )
    @PostMapping("/employees")
    fun newEmployee(
        @OpenApiRequestBody(
            description = "Employee to create",
            required = true,
            content = [
                Content(
                    schema = Schema(implementation = Employee::class),
                    examples = [
                        ExampleObject(
                            name = "NewEmployeeReactive",
                            summary = "Example request body",
                            value = """{"name": "Alice", "role": "Developer"}""",
                        ),
                    ],
                ),
            ],
        )
        @RequestBody newEmployee: Employee,
        uriBuilder: UriComponentsBuilder,
    ): Mono<ResponseEntity<Employee>> =
        repository
            .save(newEmployee)
            .map { saved ->
                val location = uriBuilder.path("/reactive/employees/{id}").build(saved.id)
                ResponseEntity.created(location).body(saved)
            }

    @Operation(
        summary = "Get an employee by ID (reactive)",
        description = "Fetches a specific employee asynchronously by ID. **Safe** and **idempotent**.",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Employee found",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = Employee::class),
                        examples = [
                            ExampleObject(
                                name = "FoundEmployeeReactive",
                                summary = "Example response",
                                value = """{"id": 1, "name": "Alice", "role": "Developer"}""",
                            ),
                        ],
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "404",
                description = "Employee not found",
                content = [
                    Content(
                        mediaType = "application/json",
                        examples = [
                            ExampleObject(
                                value = """{"error": "Could not find employee 99"}""",
                            ),
                        ],
                    ),
                ],
            ),
        ],
    )
    @GetMapping("/employees/{id}")
    fun one(
        @PathVariable id: Long,
    ): Mono<Employee> =
        repository
            .findById(id)
            .switchIfEmpty(Mono.error(EmployeeNotFoundException(id)))

    @Operation(
        summary = "Update or create an employee (reactive)",
        description =
            "Replaces an existing employee or creates one if it does not exist. **Unsafe** but" +
                " **idempotent** operation.",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Employee successfully updated",
                content = [Content(schema = Schema(implementation = Employee::class))],
            ),
            ApiResponse(
                responseCode = "201",
                description = "Employee created as new resource",
                content = [Content(schema = Schema(implementation = Employee::class))],
            ),
        ],
    )
    @PutMapping("/employees/{id}")
    fun replaceEmployee(
        @OpenApiRequestBody(
            description = "Updated employee data",
            required = true,
            content = [
                Content(
                    schema = Schema(implementation = Employee::class),
                    examples = [
                        ExampleObject(
                            name = "UpdatedEmployeeReactive",
                            summary = "Example update request body",
                            value = """{"name": "Bob", "role": "Manager"}""",
                        ),
                    ],
                ),
            ],
        )
        @RequestBody newEmployee: Employee,
        @PathVariable id: Long,
        uriBuilder: UriComponentsBuilder,
    ): Mono<ResponseEntity<Employee>> {
        val location = uriBuilder.path("/reactive/employees/{id}").build(id).toString()
        return repository
            .findById(id)
            .flatMap { existing: Employee ->
                existing.name = newEmployee.name
                existing.role = newEmployee.role
                repository
                    .save(existing)
                    .map<ResponseEntity<Employee>> { updated ->
                        ResponseEntity
                            .ok()
                            .header("Content-Location", location)
                            .body(updated)
                    }
            }.switchIfEmpty(
                repository
                    .save(newEmployee.copy(id = id))
                    .map<ResponseEntity<Employee>> { created ->
                        ResponseEntity
                            .status(HttpStatus.CREATED)
                            .header("Content-Location", location)
                            .body(created)
                    },
            )
    }

    @Operation(
        summary = "Delete an employee by ID (reactive)",
        description = "Deletes an employee asynchronously by ID. **Unsafe** but **idempotent** operation.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "204", description = "Employee successfully deleted"),
            ApiResponse(responseCode = "404", description = "Employee not found", content = [Content()]),
        ],
    )
    @DeleteMapping("/employees/{id}")
    fun deleteEmployee(
        @PathVariable id: Long,
    ): Mono<ResponseEntity<Void>> =
        repository
            .deleteById(id)
            .then(Mono.just(ResponseEntity.noContent().build()))
}
