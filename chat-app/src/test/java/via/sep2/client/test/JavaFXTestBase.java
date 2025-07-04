package via.sep2.client.test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javafx.application.Platform;

public abstract class JavaFXTestBase {

    private static boolean jfxInitialized = false;

    protected static void initializeJavaFX() {
        if (jfxInitialized) {
            return;
        }

        try {
            // Try to initialize JavaFX Platform
            if (!Platform.isFxApplicationThread()) {
                CountDownLatch latch = new CountDownLatch(1);

                Platform.startup(() -> {
                    latch.countDown();
                });

                latch.await(10, TimeUnit.SECONDS);
            }
            jfxInitialized = true;
        } catch (Exception e) {
            // If Platform.startup fails, try alternative initialization
            try {
                // This creates a minimal JavaFX environment
                System.setProperty("testfx.robot", "glass");
                System.setProperty("testfx.headless", "true");
                System.setProperty("prism.order", "sw");
                System.setProperty("prism.text", "t2k");
                System.setProperty("java.awt.headless", "true");

                // Force JavaFX platform initialization
                Platform.runLater(() -> {
                    // Empty runnable to trigger platform initialization
                });

                Thread.sleep(100); // Give JavaFX time to initialize
                jfxInitialized = true;
            } catch (Exception fallbackException) {
                System.err.println("Failed to initialize JavaFX: " + fallbackException.getMessage());
                // Continue without JavaFX - tests will run but some features may not work
            }
        }
    }

    /**
     * Utility method to run code on the JavaFX Application Thread and wait for
     * completion
     */
    protected void runOnFXThreadAndWait(Runnable runnable) throws InterruptedException {
        if (Platform.isFxApplicationThread()) {
            runnable.run();
        } else {
            CountDownLatch latch = new CountDownLatch(1);
            Platform.runLater(() -> {
                try {
                    runnable.run();
                } finally {
                    latch.countDown();
                }
            });
            latch.await(5, TimeUnit.SECONDS);
        }
    }

    /**
     * Utility method to run code on the JavaFX Application Thread
     */
    protected void runOnFXThread(Runnable runnable) {
        if (Platform.isFxApplicationThread()) {
            runnable.run();
        } else {
            Platform.runLater(runnable);
        }
    }
}
