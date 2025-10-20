package es.unizar.webeng.lab3

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.info.License
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfiguration {
    @Bean
    fun customOpenAPI(): OpenAPI =
        OpenAPI()
            .info(
                Info()
                    .title("Employee Management API")
                    .description(
                        """
                        This API provides both blocking and reactive endpoints for managing Employee resources. 
                        Demonstrates RESTful design principles with full OpenAPI/Swagger documentation.
                        """.trimIndent(),
                    ).version("1.0.0")
                    .license(
                        License()
                            .name("Apache 2.0")
                            .url("http://springdoc.org"),
                    ),
            )
}
