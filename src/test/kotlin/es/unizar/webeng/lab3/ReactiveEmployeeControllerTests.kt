package es.unizar.webeng.lab3

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import reactor.core.publisher.Mono

private val MANAGER_REQUEST_BODY = { name: String ->
    """
    {
        "role": "Manager",
        "name": "$name"
    }
    """
}

private val MANAGER_RESPONSE_BODY = { name: String, id: Int ->
    """
    {
        "name": "$name",
        "role": "Manager",
        "id": $id
    }
    """
}

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class ReactiveEmployeeControllerTests {
    @Autowired
    private lateinit var client: WebTestClient

    @MockkBean
    private lateinit var employeeRepository: ReactiveEmployeeRepository

    @LocalServerPort
    private var port: Int = 0

    @Test
    fun `POST is not safe and not idempotent`() {
        every { employeeRepository.save(any<Employee>()) } returnsMany
            listOf(
                Mono.just(Employee("Mary", "Manager", 1)),
                Mono.just(Employee("Mary", "Manager", 2)),
            )

        client
            .post()
            .uri("/reactive/employees")
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(MANAGER_REQUEST_BODY("Mary"))
            .exchange()
            .expectStatus()
            .isCreated
            .expectHeader()
            .valueEquals("Location", "http://localhost:$port/reactive/employees/1")
            .expectBody()
            .json(MANAGER_RESPONSE_BODY("Mary", 1))

        client
            .post()
            .uri("/reactive/employees")
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(MANAGER_REQUEST_BODY("Mary"))
            .exchange()
            .expectStatus()
            .isCreated
            .expectHeader()
            .valueEquals("Location", "http://localhost:$port/reactive/employees/2")
            .expectBody()
            .json(MANAGER_RESPONSE_BODY("Mary", 2))

        verify(exactly = 2) { employeeRepository.save(any<Employee>()) }
    }

    @Test
    fun `GET is safe and idempotent`() {
        every { employeeRepository.findById(1) } returns Mono.just(Employee("Mary", "Manager", 1))
        every { employeeRepository.findById(2) } returns Mono.empty()

        client
            .get()
            .uri("/reactive/employees/1")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .json(MANAGER_RESPONSE_BODY("Mary", 1))

        client
            .get()
            .uri("/reactive/employees/1")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .json(MANAGER_RESPONSE_BODY("Mary", 1))

        client
            .get()
            .uri("/reactive/employees/2")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isNotFound

        verify(exactly = 2) { employeeRepository.findById(1) }
        verify(exactly = 1) { employeeRepository.findById(2) }
        verify(exactly = 0) {
            employeeRepository.save(any<Employee>())
            employeeRepository.deleteById(any<Long>())
            employeeRepository.findAll()
        }
    }

    @Test
    fun `PUT is idempotent but not safe`() {
        every { employeeRepository.findById(1) } returnsMany
            listOf(
                Mono.empty(),
                Mono.just(Employee("Tom", "Manager", 1)),
            )
        every { employeeRepository.save(any<Employee>()) } returns Mono.just(Employee("Tom", "Manager", 1))

        client
            .put()
            .uri("/reactive/employees/1")
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(MANAGER_REQUEST_BODY("Tom"))
            .exchange()
            .expectStatus()
            .isCreated
            .expectHeader()
            .valueEquals("Content-Location", "http://localhost:$port/reactive/employees/1")
            .expectBody()
            .json(MANAGER_RESPONSE_BODY("Tom", 1))

        client
            .put()
            .uri("/reactive/employees/1")
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(MANAGER_REQUEST_BODY("Tom"))
            .exchange()
            .expectStatus()
            .isOk
            .expectHeader()
            .valueEquals("Content-Location", "http://localhost:$port/reactive/employees/1")
            .expectBody()
            .json(MANAGER_RESPONSE_BODY("Tom", 1))

        verify(exactly = 2) { employeeRepository.findById(1) }
        // Might call more than 2 times because of reactive and how it subscribes
        verify(atLeast = 2) { employeeRepository.save(any<Employee>()) }
    }

    @Test
    fun `DELETE is idempotent but not safe`() {
        every { employeeRepository.deleteById(1) } returns Mono.empty()

        client
            .delete()
            .uri("/reactive/employees/1")
            .exchange()
            .expectStatus()
            .isNoContent

        client
            .delete()
            .uri("/reactive/employees/1")
            .exchange()
            .expectStatus()
            .isNoContent

        verify(exactly = 2) { employeeRepository.deleteById(1) }
        verify(exactly = 0) {
            employeeRepository.save(any<Employee>())
            employeeRepository.findById(any<Long>())
            employeeRepository.findAll()
        }
    }
}
