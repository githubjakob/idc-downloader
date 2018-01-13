import java.io.IOException;
import java.net.MalformedURLException;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class IdcDm {

    public static AtomicBoolean downloadStopped = new AtomicBoolean(false);
    final static long rangeSize = 49152; // 48KB

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

        // parse the url
        URL url;
        try {
            url = new URL(downloadTarget);
        } catch (MalformedURLException e) {
            System.err.println("The Url you entered is not valid. Aborting.");
            return;
        }

        // setup singletons
        final BlockingQueue<Chunk> queue = new ArrayBlockingQueue<>(100000, true);
        final TokenBucket tokenBucket = new TokenBucket();
        final Thread rateLimiter = new Thread(new RateLimiter(tokenBucket, maxBytesPerSecond));
        rateLimiter.start();

        // get the filesize
        long size = 0;
        try {
            size = url.openConnection().getContentLengthLong();
        } catch (IOException e) {
            //e.printStackTrace();
            System.err.println("Could not get Filesize.");
            IdcDm.endDownload();
        }

        DownloadableMetadata downloadableMetadata = new DownloadableMetadata(url, size);

        final Thread fileWriter = new Thread(new FileWriter(downloadableMetadata, queue));
        fileWriter.start();
        final Thread downloadStatus = new Thread(new DownloadStatus(downloadableMetadata));
        
        final List<Range> missingRanges = downloadableMetadata.getMissingRanges();

        // A first time download - create the ranges for all workers using the fixed rangeSize
	 	if (missingRanges.size() == 0) {
	 		long numRanges = size / rangeSize;
		 	long remainder = size % rangeSize;
		 	
		 	if (numRanges == 0) {
		 		long rangeStart = 0;
				long rangeEnd = size - 1;
				Range workerRange = new Range(rangeStart, rangeEnd);
		 		downloadableMetadata.addRange(workerRange);
		 	} else {
			 	for (int i = 0; i < numRanges; i++) {
			
			 		long rangeStart = i * rangeSize;
			 		long rangeEnd = ((i + 1) * rangeSize) - 1;
			
			 		if (i == numRanges - 1) {
			 			rangeEnd += remainder;
			 		}
			 		
			 		Range workerRange = new Range(rangeStart, rangeEnd);
			 		downloadableMetadata.addRange(workerRange);
			 	}
		 	}
	 	} else {
	 		for (Range range : missingRanges) {
	 			range.setInUse(false);
	 		}
	 	}
        
	 	downloadStatus.start();
	 	
        while (missingRanges.size() != 0) {
        	ExecutorService executor = Executors.newFixedThreadPool(numberOfWorkers);
        	
	        List<Callable<Object>> task = new ArrayList<Callable<Object>>();
	        Range range;
	        // Find an unused range to give to a worker
	        for (int i = 0; i < numberOfWorkers; i++) {
	        	range = downloadableMetadata.getMissingRange();
        		if (range == null) break;
        		task.add(Executors.callable(new HTTPRangeGetter(url, range, queue, tokenBucket)));
	        }
	        try {
				executor.invokeAll(task);
				// check if there's Internet connection 
				final URLConnection conn = url.openConnection();
		        conn.connect();
			} catch (InterruptedException e) {
				IdcDm.downloadStopped.set(true);
				System.err.println("Executor of workers: InterruptedException occured");
				executor.shutdown();
				break;
			} catch (IOException e) {
				IdcDm.downloadStopped.set(true);
				System.err.println("Internet Connection lost");
				executor.shutdown();
				break;
			} finally{
				executor.shutdown();
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
            System.err.println("FileWriter: InterruptedException occured");
            IdcDm.endDownload();
        }
        
        tokenBucket.terminate();
        
        // wait until the rateLimiter has finished
        try {
			rateLimiter.join();
		} catch (InterruptedException e) {
			System.err.println("RateLimiter: InterruptedException occured");
			IdcDm.endDownload();
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
    
    public static void endDownload() {
    	System.err.println("Download Failed.");
        System.exit(0);
    }
}
