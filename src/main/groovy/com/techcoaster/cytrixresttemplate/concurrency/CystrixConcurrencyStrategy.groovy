package com.techcoaster.cytrixresttemplate.concurrency

import com.netflix.hystrix.strategy.concurrency.HystrixConcurrencyStrategy

import java.util.concurrent.Callable

/**
 * NOTE: This ConcurrencyStratey is only useful if we are using THREAD isolation strategy.
 * For SEMAPHOREs, this would not be invoked.
 */
class CystrixConcurrencyStrategy extends HystrixConcurrencyStrategy {

    @Override
    def <T> Callable<T> wrapCallable(Callable<T> callable) {
        println "=== wrapCallable: ${Thread.currentThread().getName()}"
        return new CystrixCallable<T>(callable)
    }
}

class CystrixCallable<T> implements Callable<T> {

    TestTlContext.RequestCountContext requestCountContext
    Callable<T> original

    CystrixCallable(Callable<T> original) {
        this.original = original
        this.requestCountContext = TestTlContext.get()
    }

    @Override
    T call() {
        TestTlContext.set(requestCountContext)
        println "==== CystrixCallable: On ${Thread.currentThread().getName()}"
        try {
            return original.call()
        } finally {
            TestTlContext.destroy()
        }
    }
}
