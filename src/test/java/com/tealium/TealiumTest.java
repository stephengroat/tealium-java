package com.tealium;

import static org.junit.Assert.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import com.tealium.Logger.Level;
import com.tealium.Tealium.DispatchCallback;

public class TealiumTest {

    @Test
    public void testTealiumInit() throws Exception {
        new Tealium.Builder("tealiummobile", "main", "dev").build();
    }

    @Test
    public void testTealiumInitWithLogLevel() throws Exception {
        new Tealium.Builder("tealiummobile", "main", "dev")
                .setLogLevel(Level.WARNINGS)
                .build();

    }

    // This is really not a great test...whatever
    @Test
    public void testTrack() throws InterruptedException {

        FileUtils.deletePersistentFile(TestLibraryContext.newInstance());

        Tealium tealium = new Tealium.Builder("tealiummobile", "demo", "dev").build();

        final CountDownLatch barrier = new CountDownLatch(3);

        DispatchCallback callBack = new DispatchCallback() {
            @Override
            public void dispatchComplete(boolean success, String encodedUrl, String error) {
                assertTrue(success);
                barrier.countDown();
            }
        };

        tealium.track("test", null, callBack);
        barrier.await(1, TimeUnit.SECONDS);

    }

}
