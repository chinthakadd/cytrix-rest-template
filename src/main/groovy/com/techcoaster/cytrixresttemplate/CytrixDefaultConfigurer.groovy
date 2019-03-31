package com.techcoaster.cytrixresttemplate

import com.netflix.config.ConfigurationManager
import com.netflix.hystrix.HystrixCommandProperties
import groovy.util.logging.Slf4j
import org.springframework.stereotype.Component

import javax.annotation.PostConstruct


/**
 * Construct a sensible set of default configurations based on
 * https://github.com/Netflix/Hystrix/wiki/Configuration.
 * <p>
 * This is done for my own understanding and future references.
 */
@Slf4j
@Component
class CytrixDefaultConfigurer {

    /**
     * This configuration will be setting all default values that are sensible.
     * <p>
     * The following values are not overridden, because the default value made sense.
     * <p>
     * ==========================================================================================
     * hystrix.command.default.execution.timeout.enabled = true
     * hystrix.command.default.execution.isolation.thread.interruptOnTimeout = true
     * hystrix.command.default.execution.isolation.thread.interruptOnCancel = false
     * hystrix.command.default.fallback.enabled = true
     * hystrix.command.default.circuitBreaker.enabled = true
     * hystrix.command.default.circuitBreaker.forceOpen = false
     * hystrix.command.default.circuitBreaker.forceClosed = false
     * ==========================================================================================
     * <p>
     * The following property defines the error percentage that has to be reached or surpass,
     * for Hystrix to short-circuit a request and move to the fallback logic.
     * Default is set to 50%.
     * ==========================================================================================
     * hystrix.command.default.circuitBreaker.errorThresholdPercentage = 50
     * ==========================================================================================
     * <p>
     * ==========================================================================================
     * Rolling Percentile Metrics
     * hystrix.command.default.metrics.rollingPercentile.enabled = true
     * <p>
     * How many execution times are kept per bucket.
     * If more than the bucketSize is executed during the window, only the number equivalent to
     * bucketSize will be kept from the rear-end of the executed request set.
     * <p>
     * hystrix.command.default.metrics.rollingPercentile.bucketSize = 100
     * ==========================================================================================
     *
     * ==========================================================================================
     * Request Properties
     * hystrix.command.HystrixCommandKey.requestCache.enabled = true
     * hystrix.command.default.requestLog.enabled = true
     * ==========================================================================================
     *
     * ==========================================================================================
     * ThreadPool Properties
     * Only required if Isolation Strategy is Thread Pool.
     * hystrix.threadpool.default.coreSize = 10
     * hystrix.threadpool.HystrixThreadPoolKey.maximumSize = 10
     *
     * -1 makes Hystrix use a Synchronous Queue
     * ( A blocking queue in which each insert operation must wait for a corresponding
     * remove operation by another thread, and vice versa.)
     * hystrix.threadpool.HystrixThreadPoolKey.maxQueueSize = -1
     * Not applicable because maxQueueSize is -1
     * hystrix.threadpool.HystrixThreadPoolKey.queueSizeRejectionThreshold = 5
     *
     * If coreSize < maximumSize, then this property controls how long a thread will go unused
     * before being released.
     * hystrix.threadpool.HystrixThreadPoolKey.keepAliveTimeMinutes = 1
     * Only applicable if coreSize < maximumSize. By default diverging is set to false.
     * hystrix.threadpool.default.allowMaximumSizeToDivergeFromCoreSize = false
     *
     * This property sets the duration of the statistical rolling window, in milliseconds.
     * This is how long metrics are kept for the thread pool.
     * hystrix.threadpool.default.metrics.rollingStats.timeInMilliseconds = 10000
     * hystrix.threadpool.default.metrics.rollingStats.numBuckets = 10
     * ==========================================================================================
     */
    @PostConstruct
    void init() {

        log.info("====================================================================================")
        log.info("Initializing Default Cytrix Properties")
        log.info("====================================================================================")

        // Setting default as Semaphore.
        // it executes on the calling thread and concurrent requests are limited by the semaphore count
        // for HystrixObservableCommand, Hystrix recommends using Semaphore instead of Thread isolation
        // Timeouts were not working with Semaphores before v 1.4. But not they have fixed it.
        ConfigurationManager.getConfigInstance()
                .setProperty("hystrix.command.default.execution.isolation.strategy",
                HystrixCommandProperties.ExecutionIsolationStrategy.SEMAPHORE)

        // default timeout value.
        // we are setting this to 10 S. Hystrix default is 1 S.
        ConfigurationManager.getConfigInstance()
                .setProperty("hystrix.command.default.execution.isolation.thread.timeoutInMilliseconds",
                200)


        // This property sets the maximum number of requests allowed to a HystrixCommand.run()
        // method when you are using ExecutionIsolationStrategy.SEMAPHORE.
        // Once this max concurrent limit is reached, the subsequent requests will be rejected.
        // This is semaphore isolation strategy specific way of defining bulk heads.
        // It is synonymous to defining a thread pool size in the THREAD isolation strategy.
        // Hystrix's default is 10. We are increasing to 50.
        ConfigurationManager.getConfigInstance()
                .setProperty("hystrix.command.default.execution.isolation.semaphore.maxConcurrentRequests",
                50)

        // Number of concurrent requests that can be passed to the fallback.
        // Setting this to same value as hystrix.command.default.execution.isolation.semaphore.maxConcurrentRequests.
        ConfigurationManager.getConfigInstance()
                .setProperty("hystrix.command.default.fallback.isolation.semaphore.maxConcurrentRequests",
                50)


        // This property sets the minimum number of requests in a rolling window that will trip the circuit.
        // This value co-relates to the rolling window.
        // Meaning, if rolling window is 10 s and if there are 10 requests that are sent. Assuming that 8 has failed,
        // it will not trip the circuit open IF the volumeThreshold is set to 9 or above.
        // Hystrix has set this 20 and we are also setting it 10.
        // This may be a higher value, and must be probably configured at individual command key level.
        // The co-relation between requestVolumeThreshold and errorThresholdPercentage are bit confusing.
        // But according to
        // https://github.com/spring-cloud/spring-cloud-netflix/issues/1943
        // and
        // https://stackoverflow.com/questions/38524259/hystrix-configuration,
        //
        // errorThresholdPercentage defines the error percentage that has to be reached for circuit to open.
        // However, Hystrix will not even bother to check whether the circuit needs to be tripped
        // if the requestVolumeThreshold is not reached. Therefore even if the error percentage is reached,
        // if the number of request has not reached the percentage, the circuit will not be tripped.
        // Therefore it essential to set this at command level based on the expected load.
        ConfigurationManager.getConfigInstance()
                .setProperty("hystrix.command.default.circuitBreaker.requestVolumeThreshold",
                10)

        // This property defines how long Hystrix will keep the circuit OPEN without retrying once more.
        // Once this window is over, Hystrix will attempt again. If it succeeds,
        // the circuit will be closed, otherwise it would go back to rejection mode.
        // Hystrix sets this at 5 S, but we have increased the default to 10 S.
        ConfigurationManager.getConfigInstance()
                .setProperty("hystrix.command.default.circuitBreaker.sleepWindowInMilliseconds", 10000)


        // Rolling Window defines how long Hystrix will keep metric data for circuit health calculation.
        // By default, Hystrix set this to 10 S. That is because, in their eco-system, APIs has to respond
        // in 300 MS or less for 95 % of the time and throughput will be extremely high.
        // However, in enterprise application development, This may not be true.
        // In my experience, typically in enterprise, most APIs would range between 50 MS to 300 MS (90%)
        // of the time, and the rest may be around 300 MS - 2 S.
        // Therefore extrapolating what netflix has done to our world, we have set the rolling window to be 2 minutes.
        // This means 2 things in terms of performance.
        // The number of calculations can be less, that means CPU taxing will be reduced
        // The metrics kept in memory will be slightly high, but this may not be a factor, because Hystrix only
        // holds onto the SUCCESS, FAILURE, TIMEOUT, REJECTION values in the bucket data.
        ConfigurationManager.getConfigInstance()
                .setProperty("hystrix.command.default.metrics.rollingStats.timeInMilliseconds", 2 * 60 * 1000)


        // Number of buckets that are part of the rolling window.
        // Default value is 10.  We have made it 6 which means, its a 20 second split in the rolling window.
        ConfigurationManager.getConfigInstance()
                .setProperty("hystrix.command.default.metrics.rollingStats.numBuckets", 6)

        // Rolling Percentile  calculation is done by hystrix for
        // the window defined here.
        // TODO: Figure out if this is important for circuit breakers? OR just for metrics?
        // Refer the following discussions on rollingPercentile
        // https://github.com/Netflix/Hystrix/issues/1339
        // https://groups.google.com/forum/#!topic/hystrixoss/zcd4kVAGreg
        ConfigurationManager.getConfigInstance()
                .setProperty("hystrix.command.default.metrics.rollingPercentile.timeInMilliseconds", 2 * 60 * 1000)

        // numBuckets defines how many buckets rolling percentile window will contain.
        // Reducing this to 1 to reduce the number of calculations.
        ConfigurationManager.getConfigInstance()
                .setProperty("hystrix.command.default.metrics.rollingPercentile.numBuckets", 6)


        // Health Snapshots are used to calculate the success and error percentages.
        // This value governs how often the circuit breaker statuses are checked by Hystix.
        // On high-volume circuits the continual calculation of error percentages
        // can become CPU intensive thus this property allows you to control how often it is calculated.
        // NOTE: On high-volume circuits the continual calculation of error percentages can become CPU intensive
        // thus this property allows you to control how often it is calculated.
        ConfigurationManager.getConfigInstance()
                .setProperty("hystrix.command.default.metrics.healthSnapshot.intervalInMilliseconds", 6 * 1000)

    }
}
