package com.layercake.rxexamples.retrywhen;

import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.functions.Function;
import retrofit2.HttpException;
import retrofit2.Response;
import timber.log.Timber;

public class RetryWhenObservable implements Function<Observable<Throwable>, ObservableSource<?>> {

    private static final int MAX_RETRIES = 3;

    @Override
    public ObservableSource<?> apply(final Observable<Throwable> throwableObservable) throws Exception {
        return throwableObservable.zipWith(Observable.range(1, MAX_RETRIES), Tuple::create)
                                  .flatMap(this::retryWhenNotUnauthorized);
    }

    private Observable<? extends Long> retryWhenNotUnauthorized(final Tuple tuple) {
        final Throwable throwable = tuple.getThrowable();

        final Integer retryCount = tuple.getInteger();
        if (retryCount == MAX_RETRIES) {
            return Observable.error(throwable);
        }

        if (throwable instanceof HttpException) {
            if (authenticationRequired((HttpException) throwable)) {
                // Do not retry if authorization is required
                return Observable.error(throwable);
            }
        }

        return buildWaitObservable(retryCount);
    }

    private boolean authenticationRequired(final HttpException httpException) {
        final Response response = httpException.response();
        return response != null && httpException.code() == 401;
    }

    private Observable<? extends Long> buildWaitObservable(final Integer retryCount) {
        // Increase the wait time between retries to give the service reliability or data availability to improve
        final long exponentialTimeout = (long) Math.pow(retryCount + 1, retryCount);
        Timber.i("Waiting for " + exponentialTimeout + "s until retrying call again");

        return Observable.timer(exponentialTimeout, TimeUnit.SECONDS);
    }

    // Using a tuple instead of Pair to allow for JUnit testing
    private static final class Tuple {
        private final Throwable throwable;
        private final Integer integer;

        private Tuple(final Throwable throwable, final Integer integer) {
            this.throwable = throwable;
            this.integer = integer;
        }

        private static Tuple create(final Throwable throwable, final Integer integer) {
            return new Tuple(throwable, integer);
        }

        private Integer getInteger() {
            return integer;
        }

        private Throwable getThrowable() {
            return throwable;
        }
    }
}
