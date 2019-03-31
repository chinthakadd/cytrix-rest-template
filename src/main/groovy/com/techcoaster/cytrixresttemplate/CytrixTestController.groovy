package com.techcoaster.cytrixresttemplate

import com.techcoaster.cytrixresttemplate.concurrency.TestTlContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import rx.schedulers.Schedulers

import java.util.concurrent.atomic.AtomicInteger

@RestController
class CytrixTestController {

    AtomicInteger count = new AtomicInteger()

    @Autowired
    CytrixRestTemplate restTemplate

    @GetMapping("/cytrix-observable")
    String observable() {

        // init thread local
        TestTlContext.init(count.incrementAndGet())
        final String baseUrl = "http://slowwly.robertomurray.co.uk/delay/5000/url/http://www.google.co.uk";
        URI uri = new URI(baseUrl);

        def requestEntity = new HttpEntity<>(null, null)
        ParameterizedTypeReference<String> parameterizedTypeReference = new ParameterizedTypeReference<String>() {}

        try {
            return "=======: " + restTemplate.exchange(
                    uri, HttpMethod.GET, requestEntity, parameterizedTypeReference
            )
                    .subscribeOn(Schedulers.io())
                    .toBlocking().value()
        } finally {
            TestTlContext.destroy()
        }

    }

    @GetMapping("/hystrix-error-handling")
    String error() {

        // init thread local
        TestTlContext.init(count.incrementAndGet())
        final String baseUrl = "http://no-url";
        URI uri = new URI(baseUrl);

        def requestEntity = new HttpEntity<>(null, null)
        ParameterizedTypeReference<String> parameterizedTypeReference = new ParameterizedTypeReference<String>() {}

        try {
            return "=======: " + restTemplate.exchange(
                    uri, HttpMethod.GET, requestEntity, parameterizedTypeReference
            ).subscribeOn(Schedulers.io()).toBlocking().first().getBody()
        } finally {
            TestTlContext.destroy()
        }

    }
}
