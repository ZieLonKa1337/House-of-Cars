package de.codazz.houseofcars;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.TimeUnit;

/** a direct executor to mock multi-threaded code with
 * @author rstumm2s */
public class ExecutorServiceMock extends AbstractExecutorService {
    private boolean shutdown = false;

    @Override
    public void shutdown() {
        shutdown = true;
    }

    @Override
    public List<Runnable> shutdownNow() {
        return Collections.emptyList();
    }

    @Override
    public boolean isShutdown() {
        return shutdown;
    }

    @Override
    public boolean isTerminated() {
        return shutdown;
    }

    @Override
    public boolean awaitTermination(final long l, final TimeUnit timeUnit) {
        return true;
    }

    @Override
    public void execute(final Runnable runnable) {
        runnable.run();
    }
}
