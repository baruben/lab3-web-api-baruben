package es.unizar.webeng.lab3

import org.springframework.data.repository.reactive.ReactiveCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface ReactiveEmployeeRepository : ReactiveCrudRepository<Employee, Long>
