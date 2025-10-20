package es.unizar.webeng.lab3

import io.swagger.v3.oas.annotations.Operation
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
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.support.ServletUriComponentsBuilder
import io.swagger.v3.oas.annotations.parameters.RequestBody as OpenApiRequestBody

@RestController
@Tag(
    name = "Employee API (blocking)",
    description =
        "CRUD operations for managing Employee resources (blocking)." +
            " Demonstrates RESTful design principles with safe (GET) and" +
            " unsafe (POST, PUT, DELETE) operations.",
)
class EmployeeController(
    private val repository: EmployeeRepositoryService,
) {
    @Operation(
        summary = "Get all employees",
        description =
            "Returns a list of all employees. This is a **safe**, **idempotent**," +
                " and **cacheable** operation.",
    )
    @ApiResponse(
        responseCode = "200",
        description = "List of employees successfully retrieved",
        content = [
            Content(
                mediaType = "application/json",
                array =
                    io.swagger.v3.oas.annotations.media.ArraySchema(
                        schema = Schema(implementation = Employee::class),
                    ),
                examples = [
                    ExampleObject(
                        name = "EmployeeList",
                        summary = "Example response",
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
    fun all(): Iterable<Employee> = repository.findAll()

    @Operation(
        summary = "Create a new employee",
        description =
            "Adds a new employee to the system. This is an **unsafe** operation" +
                " because it modifies the server state.",
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
                                name = "CreatedEmployee",
                                summary = "Example response",
                                value = """{"id": 1, "name": "Alice", "role": "Developer"}""",
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
                            name = "NewEmployee",
                            summary = "Example request body",
                            value = """{"name": "Alice", "role": "Developer"}""",
                        ),
                    ],
                ),
            ],
        )
        @RequestBody newEmployee: Employee,
    ): ResponseEntity<Employee> {
        val employee = repository.save(newEmployee)
        val location =
            ServletUriComponentsBuilder
                .fromCurrentServletMapping()
                .path("/employees/{id}")
                .build(employee.id)
        return ResponseEntity.created(location).body(employee)
    }

    @Operation(
        summary = "Get an employee by ID",
        description = "Fetches a specific employee by its unique ID. **Safe** and **idempotent** operation.",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Employee found",
                content = [Content(schema = Schema(implementation = Employee::class))],
            ),
            ApiResponse(
                responseCode = "404",
                description = "Employee not found",
                content = [
                    Content(
                        mediaType = "application/json",
                        examples = [ExampleObject(value = """{"error": "Could not find employee 99"}""")],
                    ),
                ],
            ),
        ],
    )
    @GetMapping("/employees/{id}")
    fun one(
        @PathVariable id: Long,
    ): Employee = repository.findById(id).orElseThrow { EmployeeNotFoundException(id) }

    @Operation(
        summary = "Update or create an employee",
        description =
            "Replaces an existing employee or creates a new one if not found." +
                " **Unsafe** and **idempotent**.",
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
            description = "Updated employee information",
            required = true,
            content = [
                Content(
                    schema = Schema(implementation = Employee::class),
                    examples = [
                        ExampleObject(
                            value = """{"name": "Bob", "role": "Manager"}""",
                        ),
                    ],
                ),
            ],
        )
        @RequestBody newEmployee: Employee,
        @PathVariable id: Long,
    ): ResponseEntity<Employee> {
        val location =
            ServletUriComponentsBuilder
                .fromCurrentServletMapping()
                .path("/employees/{id}")
                .build(id)
                .toASCIIString()
        val (status, body) =
            repository
                .findById(id)
                .map { employee ->
                    employee.name = newEmployee.name
                    employee.role = newEmployee.role
                    repository.save(employee)
                    HttpStatus.OK to employee
                }.orElseGet {
                    newEmployee.id = id
                    repository.save(newEmployee)
                    HttpStatus.CREATED to newEmployee
                }
        return ResponseEntity.status(status).header("Content-Location", location).body(body)
    }

    @Operation(
        summary = "Delete an employee by ID",
        description = "Deletes the employee resource. This is an **unsafe**, **idempotent** operation.",
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
    ): ResponseEntity<Void> {
        repository.deleteById(id)
        return ResponseEntity.noContent().build()
    }
}

@ResponseStatus(HttpStatus.NOT_FOUND)
class EmployeeNotFoundException(
    id: Long,
) : Exception("Could not find employee $id")
