import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class IdcDm {

    public static final AtomicBoolean DOWNLOADING = new AtomicBoolean(true);

    /**
     * Receive arguments from the command-line, provide some feedback and start the download.
     *
     * @param args command-line arguments
     */
    public static void main(String[] args) {
        /*int numberOfWorkers = 1;
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
            System.err.printf(" limited to %d Bps", maxBytesPerSecond);
        System.err.printf("...\n");

        DownloadURL(url, numberOfWorkers, maxBytesPerSecond);*/

        DownloadURL(null, 0, 0L);
    }

    /**
     * Initiate the file's metadata, and iterate over missing ranges. For each:
     * 1. Setup the Queue, TokenBucket, DownloadableMetadata, FileWriter, RateLimiter, and a pool of HTTPRangeGetters
     * 2. Join the HTTPRangeGetters, send finish marker to the Queue and terminate the TokenBucket
     * 3. Join the FileWriter and RateLimiter
     *
     * Finally, print "Download succeeded/failed" and delete the metadata as needed.
     *
     * @param url URL to download
     * @param numberOfWorkers number of concurrent connections
     * @param maxBytesPerSecond limit on download bytes-per-second
     */
    private static void DownloadURL(URL url, int numberOfWorkers, Long maxBytesPerSecond) {

        // TODO remove
        numberOfWorkers = 2;


        try {
            //url = new URL("http://www.engr.colostate.edu/me/facil/dynamics/files/drop.avi");
            url = new URL("http://ia600303.us.archive.org/19/items/Mario1_500/Mario1_500.avi");
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        // setup singletons
        BlockingQueue queue = new ArrayBlockingQueue<Chunk>(1000, true);

        // get the filesize
        HttpHeadGetter httpHeadGetter = new HttpHeadGetter(url);
        long size = httpHeadGetter.getFileSize();

        DownloadableMetadata downloadableMetadata = new DownloadableMetadata(url, size);

        Thread fileWriter = new Thread(new FileWriter(downloadableMetadata, queue));
        fileWriter.start();

        final List<Range> missingRanges = downloadableMetadata.getMissingRanges();
        int workersPerMissingRange = numberOfWorkers / missingRanges.size();

        // iterate over the missing ranges

        List<Thread> downloadThreads = new ArrayList<>();

        for (int n = 0; n < missingRanges.size(); n++) {
            Range missingRange = missingRanges.get(n);

            long workerRangeLength = missingRange.getLength() / 2;

            // start workers for every missing range
            for (int i = 0; i < workersPerMissingRange; i++) {

                long rangeStart = missingRange.getStart() + i * workerRangeLength;
                long rangeEnd = missingRange.getStart() + (i+1) * workerRangeLength - 1;

                Range workerRange = new Range(rangeStart, rangeEnd);

                //downloadableMetadata.addPointer(rangeStart);
                downloadableMetadata.addRange(new Range(rangeStart, rangeStart));

                Thread downloadThread = new Thread(new HTTPRangeGetter(
                        url,
                        workerRange,
                        queue,
                        null));
                downloadThreads.add(downloadThread);
                downloadThread.start();
            }
        }

        // wait until all threads have completed
        for (Thread thread : downloadThreads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        // validate download
        downloadableMetadata.validateDownload();
        System.out.println("done");

    }
}
