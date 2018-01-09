import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * TODO remove
 *
 * use this urls to test
 * //url = new URL("http://www.engr.colostate.edu/me/facil/dynamics/files/drop.avi");
 //url = new URL("http://ia600303.us.archive.org/19/items/Mario1_500/Mario1_500.avi");
 //url = new URL("https://archive.org/download/Mario1_500/Mario1_500.avi");
 */
public class IdcDm {

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

        // final Long maxBytesPerSecond = maxKBytesPerSecond != null ? maxKBytesPerSecond * 1000 : null;

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

        // parse the url
        URL url;
        try {
            url = new URL(downloadTarget);
        } catch (MalformedURLException e) {
            System.out.println("The Url you entered is not valid. Aborting.");
            return;
        }

        // setup singletons
        final BlockingQueue<Chunk> queue = new ArrayBlockingQueue<>(100000, true);
        final TokenBucket tokenBucket = new TokenBucket();
        final Thread rateLimiter = new Thread(new RateLimiter(tokenBucket, maxBytesPerSecond));
        rateLimiter.start();

        // get the filesize
        HttpHeadGetter httpHeadGetter = new HttpHeadGetter(url);
        long size = httpHeadGetter.getFileSize();

        DownloadableMetadata downloadableMetadata = new DownloadableMetadata(url, size);

        final Thread fileWriter = new Thread(new FileWriter(downloadableMetadata, queue));
        fileWriter.start();
        final Thread downloadStatus = new Thread(new DownloadStatus(downloadableMetadata));
        
        final List<Range> missingRanges = downloadableMetadata.getMissingRanges();

	     // A first time download - create the ranges for all workers
	     if (missingRanges.size() == 0) {
	     	long rangeSize = size / numberOfWorkers;
	     	long remainder = size % numberOfWorkers;
	     	
	     	for (int i = 0; i < numberOfWorkers; i++) {
	
	     		long rangeStart = i * rangeSize;
	     		long rangeEnd = (i + 1) * rangeSize - 1;
	
	     		if (i == numberOfWorkers - 1) {
	     			rangeEnd += remainder;
	     		}
	     		
	     		Range workerRange = new Range(rangeStart, rangeEnd);
	     		downloadableMetadata.addRange(workerRange);
	     	}
	     }
        
        int workersPerMissingRange = numberOfWorkers / missingRanges.size(); // TODO should always be != 0, if division is 0 set to 1

        // TODO "3/4 problem" - more ranges than workers: keep track of created workers and if workers exceed maxWorkers than skip downloading

        // iterate over the missing ranges

        List<Thread> downloadThreads = new ArrayList<>();

        for (int n = 0; n < missingRanges.size(); n++) {
	     	final Range missingRange = missingRanges.get(n);
	
	     	long workerRangeLength = missingRange.getLength() / workersPerMissingRange;
	     	long remainder = missingRange.getLength() % workersPerMissingRange;
	
	     	// start workers for every missing range
	     	for (int i = 0; i < workersPerMissingRange; i++) {
	
	     		long rangeStart = missingRange.getStart() + i * workerRangeLength;
	     		long rangeEnd = rangeStart + workerRangeLength - 1;
	
	     		if (i == workersPerMissingRange - 1) {
	     			rangeEnd += remainder;
	     		}
	     		
	     		Range workerRange = new Range(rangeStart, rangeEnd);
	     		// downloadableMetadata.addRange(workerRange);
	
	     		Thread downloadThread = new Thread(new HTTPRangeGetter(
	     				url,
	     				workerRange,
	     				queue,
	     				tokenBucket));
	     		downloadThreads.add(downloadThread);
	     		downloadThread.start();
	     	}
	     }

        downloadStatus.start();

        // wait until all threads have completed
        for (final Thread thread : downloadThreads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        // Stopping FileWriter
        final Chunk finishedChunk = new Chunk(null, 0, 0);
        finishedChunk.setAsFinishedMarker();
        queue.add(finishedChunk);

        // wait until the fileWriter has finished
        try {
            fileWriter.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // validate download
        if (downloadableMetadata.getMissingRanges().size() == 0) {
            // clean up metadata files
            downloadableMetadata.cleanUpMetadata();
            System.out.println("DownloadableMetadata: File is valid.");
        } else {
            System.err.println("DownloadableMetadata: File is not valid.");
        }

        // Stopping other
        tokenBucket.terminate();
        downloadStatus.stop();
        downloadableMetadata.cleanUpMetadata();

        System.err.println("IdcDm: Done");
    }
}
