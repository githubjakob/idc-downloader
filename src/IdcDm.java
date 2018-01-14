import java.io.IOException;
import java.net.MalformedURLException;
import java.net.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class IdcDm {

    public static AtomicBoolean downloadStopped = new AtomicBoolean(false);

    /**
     * Receive arguments from the command-line, provide some feedback and start the download.
     *
     * @param args command-line arguments
     */
    public static void main(String[] args) {
    	int numberOfWorkers = 1;
        Long maxBytesPerSecond = null;

        if (args.length < 1 || args.length > 3) {
            System.err.printf("usage:\n\tjava IdcDm URL [MAX-CONCURRENT-CONNECTIONS] [MAX-DOWNLOAD-LIMIT]\n");
            System.exit(1);
        } else if (args.length >= 2) {
            numberOfWorkers = Integer.parseInt(args[1]);
            if (args.length == 3)
                maxBytesPerSecond = Long.parseLong(args[2]);
        }

        String url = args[0];

        System.err.printf("Downloading");
        if (numberOfWorkers > 1)
            System.err.printf(" using %d connections", numberOfWorkers);
        if (maxBytesPerSecond != null)
            System.err.printf(" limited to %d KBps", maxBytesPerSecond / 1000);
        System.err.printf("...\n");

        DownloadURL(url, numberOfWorkers, maxBytesPerSecond);
    }

    /**
     * Initiate the file's metadata, and iterate over missing ranges. For each:
     * 1. Setup the Queue, TokenBucket, DownloadableMetadata, FileWriter, RateLimiter, and a pool of HTTPRangeGetters
     * 2. Join the HTTPRangeGetters, send finish marker to the Queue and terminate the TokenBucket
     * 3. Join the FileWriter and RateLimiter
     *
     * Finally, print "Download succeeded/failed" and delete the metadata as needed.
     *
     * @param downloadTarget URL to download
     * @param numberOfWorkers number of concurrent connections
     * @param maxBytesPerSecond limit on download bytes-per-second
     */
    private static void DownloadURL(String downloadTarget, int numberOfWorkers, Long maxBytesPerSecond) {

        final URL url = parseUrl(downloadTarget);
        if (url == null) return;

        Long size = getFileSize(url);
        if (size == null) return;

        // setup objects
        final BlockingQueue<Chunk> queue = new ArrayBlockingQueue<>(100000, true);
        final TokenBucket tokenBucket = new TokenBucket();
        final Thread rateLimiter = new Thread(new RateLimiter(tokenBucket, maxBytesPerSecond));
        rateLimiter.start();
        final DownloadableMetadata downloadableMetadata = new DownloadableMetadata(url, size);
        final Thread fileWriter = new Thread(new FileWriter(downloadableMetadata, queue));
        fileWriter.start();
        final Thread downloadStatus = new Thread(new DownloadStatus(downloadableMetadata));
        downloadStatus.start();

        // setup download workers thread pool
        ExecutorService executor = Executors.newFixedThreadPool(numberOfWorkers);
        for (int i = 0; i < numberOfWorkers; i++) {
            executor.execute(new HTTPRangeGetter(url, queue, tokenBucket, downloadableMetadata));
        }
        executor.shutdown();
        try {
            executor.awaitTermination(12, TimeUnit.HOURS);
        } catch (InterruptedException e) {
            IdcDm.exitWithFailure();
        }

        // Stopping FileWriter
        final Chunk finishedChunk = new Chunk(null, 0, 0);
        finishedChunk.setAsFinishedMarker();
        queue.add(finishedChunk);

        // wait until the fileWriter has finished
        try {
            fileWriter.join();
        } catch (InterruptedException e) {
            IdcDm.exitWithFailure();
        }
        
        tokenBucket.terminate();
        
        // wait until the rateLimiter has finished
        try {
			rateLimiter.join();
		} catch (InterruptedException e) {
			System.err.println("RateLimiter: InterruptedException occurred");
			IdcDm.exitWithFailure();
		}

        // validate download
        if (downloadableMetadata.getMissingRanges().size() == 0) {
            // clean up metadata files
            downloadableMetadata.cleanUpMetadata();
            System.err.println("Download Succeeded.");
        } else {
            System.err.println("Download Failed.");
        }

        // Stopping DownloadStatus
        IdcDm.downloadStopped.set(true);
    }
    
    public static void exitWithFailure() {
    	System.err.println("Download Failed.");
        System.exit(0);
    }

    private static Long getFileSize(URL url) {
        try {
            return url.openConnection().getContentLengthLong();
        } catch (IOException e) {
            System.out.println("Could not get Filesize.");
            return null;
        }
    }

    private static URL parseUrl(String downloadTarget) {
        try {
            return new URL(downloadTarget);
        } catch (MalformedURLException e) {
            System.out.println("The Url you entered is not valid.");
            return null;
        }
    }
}
