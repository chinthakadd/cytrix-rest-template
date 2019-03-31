package com.techcoaster.cytrixresttemplate.concurrency

import rx.functions.Action0

/**
 * RxJava Action0 for custom copying of thread locals.
 */
class TestAction0 implements Action0 {

    TestTlContext.RequestCountContext requestCountContext
    Action0 original

    TestAction0(Action0 original) {
        this.original = original
        this.requestCountContext = TestTlContext.get()
    }

    @Override
    void call() {
        TestTlContext.set(requestCountContext)
        println "==== TestAction0: On ${Thread.currentThread().getName()}"
        try {
            original.call()
        } finally {
            TestTlContext.destroy()
        }
    }
}