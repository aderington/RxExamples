package com.layercake.rxexamples.retrywhen;

import com.layercake.rxexamples.retrywhen.RetryWhenObservable;

import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.observers.TestObserver;
import okhttp3.ResponseBody;
import retrofit2.HttpException;
import retrofit2.Response;
import timber.log.Timber;

import static org.junit.Assert.assertEquals;

public class RetryWhenObservableTest {

    private static final List<String> RESULTS = Arrays.asList("Try 1", "Try 2", "Try 3", "Try 4");

    @SuppressWarnings("ThrowableInstanceNeverThrown")
    private static final HttpException AUTH_ERROR = new HttpException(Response.error(401, ResponseBody.create(null, "Denied!")));

    @Before
    public void setup() {
        Timber.plant(new Timber.Tree() {
            @Override
            protected void log(final int priority, final String tag, final String message, final Throwable t) {
                System.out.println(message);
            }
        });
    }

    @Test
    public void success_firstTime() {
        final int expectedAttempt = 1;
        final TestRetryObservable testRetryObservable = new TestRetryObservable(expectedAttempt);

        final TestObserver<String> testObserver = TestObserver.create();

        final Observable<String> observable = Observable.create(testRetryObservable);

        observable.retryWhen(new RetryWhenObservable())
                  .subscribe(testObserver);

        testObserver.assertValueCount(1);
        testObserver.assertValue("Try " + expectedAttempt);
        assertEquals(expectedAttempt, testRetryObservable.getRetryAttempts());
    }

    @Test
    public void success_secondTime() throws InterruptedException {
        final int expectedAttempt = 2;
        final TestRetryObservable testRetryObservable = new TestRetryObservable(expectedAttempt);

        final TestObserver<String> testObserver = TestObserver.create();

        final Observable<String> observable = Observable.create(testRetryObservable);

        observable.retryWhen(new RetryWhenObservable())
                  .subscribe(testObserver);

        Thread.sleep(2100);

        testObserver.assertValueCount(1);
        testObserver.assertValue("Try " + expectedAttempt);
        assertEquals(expectedAttempt, testRetryObservable.getRetryAttempts());
    }

    @Test
    public void success_thirdTime() throws InterruptedException {
        final int expectedAttempt = 3;
        final TestRetryObservable testRetryObservable = new TestRetryObservable(expectedAttempt);

        final TestObserver<String> testObserver = TestObserver.create();

        final Observable<String> observable = Observable.create(testRetryObservable);

        observable.retryWhen(new RetryWhenObservable())
                  .subscribe(testObserver);

        Thread.sleep(11100);

        testObserver.assertValueCount(1);
        testObserver.assertValue("Try " + expectedAttempt);
        assertEquals(expectedAttempt, testRetryObservable.getRetryAttempts());
    }

    @Test
    public void failure_FourthTime() throws InterruptedException {
        final int expectedAttempt = 4;
        final TestRetryObservable testRetryObservable = new TestRetryObservable(expectedAttempt);

        final TestObserver<String> testObserver = TestObserver.create();

        final Observable<String> observable = Observable.create(testRetryObservable);

        observable.retryWhen(new RetryWhenObservable())
                  .subscribe(testObserver);

        Thread.sleep(11100);

        testObserver.assertErrorMessage("BOOM!");
        assertEquals("Should only retry a maximum of 3 times", 3, testRetryObservable.getRetryAttempts());
    }

    @Test
    public void failure_AuthRequired() throws InterruptedException {
        final TestAuthErrorRetryObservable testRetryObservable = new TestAuthErrorRetryObservable();

        final TestObserver<String> testObserver = TestObserver.create();

        final Observable<String> observable = Observable.create(testRetryObservable);

        observable.retryWhen(new RetryWhenObservable())
                  .subscribe(testObserver);

        Thread.sleep(2100);

        testObserver.assertError(AUTH_ERROR);
        assertEquals("Should have stopped retrying after auth error", 2, testRetryObservable.getRetryAttempts());
    }

    private class TestRetryObservable implements ObservableOnSubscribe<String> {

        private final AtomicInteger retryAttempts = new AtomicInteger();
        private final int successAttempt;

        TestRetryObservable(final int successAttempt) {
            // Convert to 0-based index
            this.successAttempt = successAttempt - 1;
        }

        @Override
        public void subscribe(final ObservableEmitter<String> emitter) throws Exception {
            final int increment = retryAttempts.getAndIncrement();

            if (successAttempt == increment) {
                emitter.onNext(RESULTS.get(increment));
            } else {
                emitter.onError(new Exception("BOOM!"));
            }
        }

        private int getRetryAttempts() {
            return retryAttempts.get();
        }
    }

    private class TestAuthErrorRetryObservable implements ObservableOnSubscribe<String> {

        private final AtomicInteger retryAttempts = new AtomicInteger();

        @Override
        public void subscribe(final ObservableEmitter<String> emitter) throws Exception {
            final int increment = retryAttempts.getAndIncrement();

            switch (increment) {
                case 0:
                    emitter.onError(new HttpException(Response.error(404, ResponseBody.create(null, "Foo"))));
                    break;
                case 1:
                    emitter.onError(AUTH_ERROR);
                    break;
                default:
                    emitter.onError(new HttpException(Response.error(500, ResponseBody.create(null, "Bar"))));
                    break;
            }
        }

        private int getRetryAttempts() {
            return retryAttempts.get();
        }
    }
}