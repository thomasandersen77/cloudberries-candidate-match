package no.cloudberries.candidatematch.controllers.debug

import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class EchoProbeController {
    @PostMapping("/__probe__", consumes = ["text/plain"], produces = ["text/plain"])
    fun probe(@RequestBody body: String) = "ok:$body"
}