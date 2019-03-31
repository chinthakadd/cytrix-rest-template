package com.techcoaster.cytrixresttemplate

import com.netflix.hystrix.strategy.HystrixPlugins
import com.techcoaster.cytrixresttemplate.concurrency.CystrixConcurrencyStrategy
import com.techcoaster.cytrixresttemplate.concurrency.TestAction0
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import rx.functions.Action0
import rx.functions.Func1
import rx.plugins.RxJavaHooks

@SpringBootApplication
class App {

    static void main(String[] args) {

        /**
         * This is required for passing the thread local to the subscriber thread.
         * Since we are using our own thread poolsw ith SEMAPHORE.
         */
        RxJavaHooks.setOnScheduleAction(new Func1<Action0, Action0>() {
            @Override
            Action0 call(Action0 action0) {
                return new TestAction0(action0)
            }
        })

        HystrixPlugins.getInstance().registerConcurrencyStrategy(new CystrixConcurrencyStrategy())

        SpringApplication.run(App, args)
    }

}
