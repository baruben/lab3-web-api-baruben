package es.unizar.webeng.lab3

import io.gatling.javaapi.core.CoreDsl.*
import io.gatling.javaapi.core.Simulation
import io.gatling.javaapi.http.HttpDsl.*
import java.time.Duration

class ReactiveEmployeeSimulation : Simulation() {
    private val httpProtocol =
        http
            .baseUrl("http://localhost:8080/reactive") // reactive controller
            .acceptHeader("application/json")
            .contentTypeHeader("application/json")

    private val feeder =
        generateSequence(1) { it + 1 }
            .map { mapOf("name" to "Employee$it", "role" to "Developer") }
            .iterator()

    private val scn =
        scenario("ReactiveEmployeeScenario")
            .feed(feeder)
            .exec(
                http("Create Employee (reactive)")
                    .post("/employees")
                    .body(StringBody("""{"name": "#{name}", "role": "#{role}"}"""))
                    .asJson()
                    .check(status().`is`(201))
                    .check(jsonPath("$.id").saveAs("employeeId")),
            ).pause(Duration.ofMillis(100))
            .exec(
                http("Get Employee By ID (reactive)")
                    .get("/employees/#{employeeId}")
                    .check(status().`is`(200)),
            )

    init {
        setUp(
            scn.injectOpen(
                rampUsers(30000).during(Duration.ofSeconds(60))
            ),
        ).protocols(httpProtocol)
    }
}
