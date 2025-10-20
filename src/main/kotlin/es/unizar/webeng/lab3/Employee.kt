package es.unizar.webeng.lab3

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table

@Table("employees")
data class Employee(
    var name: String,
    var role: String,
    @Id
    var id: Long? = null,
)
