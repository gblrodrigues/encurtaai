package com.gblrod.encurtaai.doc.annotation

import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@ApiResponses(
    value = [
        ApiResponse(
            responseCode = "401",
            ref = "#/components/responses/Unauthorized"
        ),
        ApiResponse(
            responseCode = "429",
            ref = "#/components/responses/TooManyRequests"
        ),
        ApiResponse(
            responseCode = "500",
            ref = "#/components/responses/InternalServerError"
        )
    ]
)

annotation class ApiAdminResponses