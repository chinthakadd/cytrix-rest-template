package com.techcoaster.cytrixresttemplate

import com.netflix.hystrix.HystrixCommandGroupKey
import com.netflix.hystrix.HystrixCommandKey
import com.netflix.hystrix.HystrixObservableCommand
import com.techcoaster.cytrixresttemplate.concurrency.TestTlContext
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.ResponseEntity
import org.springframework.web.client.RestTemplate
import rx.Observable
import rx.Observer
import rx.observables.SyncOnSubscribe

/**
 * N
 */
class RestTemplateCommand<T> extends HystrixObservableCommand<ResponseEntity<T>> {

    RestTemplate restTemplateDelegate
    URI url
    HttpMethod httpMethod
    ParameterizedTypeReference<T> responseType
    HttpEntity<?> requestEntity
    String hystrixGroup

    /**
     *
     * @param restTemplate
     * @param hystrixGroup : Grouping for bulk head.
     * @param url :
     */
    RestTemplateCommand(
            RestTemplate restTemplate, URI url, HttpMethod httpMethod, HttpEntity<?> requestEntity,
            ParameterizedTypeReference<T> responseType
    ) {
        super(
                Setter
                        .withGroupKey(HystrixCommandGroupKey.Factory.asKey(commandKey(url, HttpMethod.GET)))
                        .andCommandKey(HystrixCommandKey.Factory.asKey(commandGroup(url))
                ))

        this.restTemplateDelegate = restTemplate
        this.url = url
        this.hystrixGroup = hystrixGroup
        this.httpMethod = httpMethod
        this.responseType = responseType
        this.requestEntity = requestEntity
    }

    /**
     * Command Group is equivalent to URI.
     *
     * @return
     */
    private static String commandGroup(URI url) {
        return "CG-" + url.getHost();
    }

    private static String commandKey(URI url, HttpMethod httpMethod) {
        return "CK-${httpMethod}-${url.toString()}"
    }

    @Override
    protected Observable<ResponseEntity<T>> construct() {
        return Observable.create(
                new SyncOnSubscribe<ResponseEntity<T>, ResponseEntity<T>>() {
                    @Override
                    protected ResponseEntity<T> generateState() {
                        //Thread.sleep(200)
                        println "REQUEST COUNT: ${TestTlContext.get().requestCount} ON THREAD: ${Thread.currentThread().getName()}"
                        // a real example would do work like a network call here
                        return restTemplateDelegate.exchange(
                                url, httpMethod, requestEntity, responseType
                        )
                    }

                    @Override
                    protected ResponseEntity<T> next(ResponseEntity<T> state, Observer<? super ResponseEntity<T>> observer) {
                        observer.onNext(state)
                        observer.onCompleted()
                        return state
                    }
                }
        )
    }

    @Override
    protected Observable<ResponseEntity<T>> resumeWithFallback() {
        println "=====: FALLBACK"
        return Observable.just(ResponseEntity.ok("ERROR") as ResponseEntity<T>)
    }
}


//    public static void main(String[] args) {
//        Observable.create(
//                new SyncOnSubscribe<Integer, Integer>() {
//
//                    @Override
//                    protected Integer generateState() {
//                        return 1;
//                    }
//
//                    @Override
//                    protected Integer next(Integer state, Observer<? super Integer> observer) {
//                        if (state == 10) {
//                            observer.onCompleted();
//                            return state;
//                        }
//                        observer.onNext(state++);
//                        return state;
//                    }
//                }
//        ).toBlocking().forEach(
//                new Action1<Integer>() {
//                    @Override
//                    public void call(Integer integer) {
//                        System.out.println(integer);
//                    }
//                }
//        );
//    }