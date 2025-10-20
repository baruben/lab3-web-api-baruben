package es.unizar.webeng.lab3

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.put
import java.util.Optional

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
       "name" : "$name",
       "role" : "Manager",
       "id" : $id
    }
    """
}

@SpringBootTest(webEnvironment = WebEnvironment.MOCK)
@AutoConfigureMockMvc
class EmployeeControllerTests {
    @Autowired
    private lateinit var mvc: MockMvc

    @MockkBean
    private lateinit var employeeRepository: EmployeeRepositoryService

    @Test
    fun `POST is not safe and not idempotent`() {
        // Mock save to return different IDs on multiple calls
        every {
            employeeRepository.save(any<Employee>())
        } answers {
            Employee("Mary", "Manager", 1)
        } andThenAnswer {
            Employee("Mary", "Manager", 2)
        }

        mvc
            .post("/employees") {
                contentType = MediaType.APPLICATION_JSON
                content = MANAGER_REQUEST_BODY("Mary")
                accept = MediaType.APPLICATION_JSON
            }.andExpect {
                status { isCreated() }
                header { string("Location", "http://localhost/employees/1") }
                content {
                    contentType(MediaType.APPLICATION_JSON)
                    json(MANAGER_RESPONSE_BODY("Mary", 1))
                }
            }

        mvc
            .post("/employees") {
                contentType = MediaType.APPLICATION_JSON
                content = MANAGER_REQUEST_BODY("Mary")
                accept = MediaType.APPLICATION_JSON
            }.andExpect {
                status { isCreated() }
                header { string("Location", "http://localhost/employees/2") }
                content {
                    contentType(MediaType.APPLICATION_JSON)
                    json(MANAGER_RESPONSE_BODY("Mary", 2))
                }
            }

        // Check that save was called exactly twice
        verify(exactly = 2) {
            employeeRepository.save(any<Employee>())
        }
    }

    @Test
    fun `GET is safe and idempotent`() {
        // Mock repository to return an employee with id = 1
        every {
            employeeRepository.findById(1)
        } answers {
            Optional.of(Employee("Mary", "Manager", 1))
        }
        // Mock repository to return empty for id = 2
        every {
            employeeRepository.findById(2)
        } answers {
            Optional.empty()
        }

        mvc.get("/employees/1").andExpect {
            status { isOk() }
            content {
                contentType(MediaType.APPLICATION_JSON)
                json(MANAGER_RESPONSE_BODY("Mary", 1))
            }
        }

        mvc.get("/employees/1").andExpect {
            status { isOk() }
            content {
                contentType(MediaType.APPLICATION_JSON)
                json(MANAGER_RESPONSE_BODY("Mary", 1))
            }
        }

        mvc.get("/employees/2").andExpect {
            status { isNotFound() }
        }

        // Check that findById was called exactly three times
        verify(exactly = 2) {
            employeeRepository.findById(1)
        }
        verify(exactly = 1) {
            employeeRepository.findById(2)
        }
        // Check that no modification methods were called
        verify(exactly = 0) {
            employeeRepository.save(any<Employee>())
            employeeRepository.deleteById(any())
            employeeRepository.findAll()
        }
    }

    @Test
    fun `PUT is idempotent but not safe`() {
        // Mock repository to return empty first, then employee
        every {
            employeeRepository.findById(1)
        } answers {
            Optional.empty()
        } andThenAnswer {
            Optional.of(Employee("Tom", "Manager", 1))
        }
        // Mock save to return employee with specified ID
        every {
            employeeRepository.save(any<Employee>())
        } answers {
            Employee("Tom", "Manager", 1)
        }

        mvc
            .put("/employees/1") {
                contentType = MediaType.APPLICATION_JSON
                content = MANAGER_REQUEST_BODY("Tom")
                accept = MediaType.APPLICATION_JSON
            }.andExpect {
                status { isCreated() }
                header { string("Content-Location", "http://localhost/employees/1") }
                content {
                    contentType(MediaType.APPLICATION_JSON)
                    json(MANAGER_RESPONSE_BODY("Tom", 1))
                }
            }

        mvc
            .put("/employees/1") {
                contentType = MediaType.APPLICATION_JSON
                content = MANAGER_REQUEST_BODY("Tom")
                accept = MediaType.APPLICATION_JSON
            }.andExpect {
                status { isOk() }
                header { string("Content-Location", "http://localhost/employees/1") }
                content {
                    contentType(MediaType.APPLICATION_JSON)
                    json(MANAGER_RESPONSE_BODY("Tom", 1))
                }
            }

        // Check that findById was called exactly twice
        verify(exactly = 2) {
            employeeRepository.findById(1)
        }
        // Check that save was called exactly twice
        verify(exactly = 2) {
            employeeRepository.save(any<Employee>())
        }
    }

    @Test
    fun `DELETE is idempotent but not safe`() {
        // Allow deleteById method to be called
        every { employeeRepository.deleteById(1) } returns null

        mvc.delete("/employees/1").andExpect {
            status { isNoContent() }
        }

        mvc.delete("/employees/1").andExpect {
            status { isNoContent() }
        }

        // Check deleteById calls and no other methods
        verify(exactly = 2) {
            employeeRepository.deleteById(1)
        }
        verify(exactly = 0) {
            employeeRepository.save(any<Employee>())
            employeeRepository.findById(any())
            employeeRepository.findAll()
        }
    }
}
