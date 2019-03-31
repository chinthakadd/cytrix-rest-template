package com.techcoaster.cytrixresttemplate.concurrency

class TestTlContext {

    static ThreadLocal<RequestCountContext> contextThreadLocal = new ThreadLocal<>()


    static void init(int counter) {
        contextThreadLocal.set(new RequestCountContext(requestCount: counter))
    }

    static void set(RequestCountContext requestCountContext) {
        contextThreadLocal.set(requestCountContext)
    }

    static RequestCountContext get() {
        contextThreadLocal.get()
    }

    static void destroy() {
        contextThreadLocal.remove()
    }

    static class RequestCountContext {
        int requestCount
    }
}

