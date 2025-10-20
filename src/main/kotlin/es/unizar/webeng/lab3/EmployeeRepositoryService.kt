package es.unizar.webeng.lab3

import org.springframework.stereotype.Service
import java.util.Optional

@Service
class EmployeeRepositoryService(
    private val reactiveRepository: ReactiveEmployeeRepository,
) {
    fun findAll(): List<Employee> = reactiveRepository.findAll().collectList().block()!!

    fun save(employee: Employee): Employee = reactiveRepository.save(employee).block()!!

    fun findById(id: Long): Optional<Employee> = Optional.ofNullable(reactiveRepository.findById(id).block())

    fun deleteById(id: Long) = reactiveRepository.deleteById(id).block()
}
