package com.techcoaster.cytrixresttemplate

import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.ResponseEntity
import org.springframework.web.client.RestTemplate
import rx.Single

class CytrixRestTemplate {

    RestTemplate restTemplate

    CytrixRestTemplate(RestTemplate restTemplate) {
        this.restTemplate = restTemplate
    }

    def <T> Single<ResponseEntity<T>> exchange(URI url, HttpMethod httpMethod, HttpEntity<?> requestEntity,
                                               ParameterizedTypeReference<T> responseType) {
        return new RestTemplateCommand<T>(
                restTemplate, url,
                httpMethod, requestEntity,
                responseType
        ).toObservable().toSingle()
    }

}
