import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.BlockingQueue;

/**
 * A runnable class which downloads a given url.
 * It reads CHUNK_SIZE at a time and writs it into a BlockingQueue.
 * It supports downloading a range of data, and limiting the download rate using a token bucket.
 */
public class HTTPRangeGetter implements Runnable {

    static final int CHUNK_SIZE = 4096; // we read in chunks of 4kb from the stream and write it to the file

    private static final int CONNECT_TIMEOUT = 500; // timeout until establishing a connection will fail in ms

    private static final int READ_TIMEOUT = 2000; // timemout during reading from a stream in ms

    private static final String HTTP_GET = "GET"; // get method for the http request

    private final URL url;

    private final BlockingQueue<Chunk> outQueue;

    private final DownloadableMetadata downloadableMetadata;

    private TokenBucket tokenBucket;

    public HTTPRangeGetter(
            URL url,
            BlockingQueue<Chunk> outQueue,
            TokenBucket tokenBucket,
            DownloadableMetadata downloadableMetadata) {
        this.url = url;
        this.outQueue = outQueue;
        this.tokenBucket = tokenBucket;
        this.downloadableMetadata = downloadableMetadata;
    }

    private void downloadRange(Range range) throws IOException {
        final HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();

        httpURLConnection.setRequestMethod(HTTP_GET);
        httpURLConnection.setRequestProperty("Range", "bytes=" + range.getStart() + "-" + range.getEnd());
        httpURLConnection.setConnectTimeout(CONNECT_TIMEOUT);
        httpURLConnection.setReadTimeout(READ_TIMEOUT);

        int responseCode = httpURLConnection.getResponseCode();

        if (responseCode != HttpURLConnection.HTTP_PARTIAL) {
            System.err.println("HTTPRangeGetter: Unexpected HTTP Status Response.");
            range.setInUse(false);
            return;
        }

        final BufferedInputStream inputStream = new BufferedInputStream(httpURLConnection.getInputStream());

        byte[] buffer = new byte[CHUNK_SIZE];
        int bytesRead;
        long offset = range.getStart();

        while ((bytesRead = inputStream.read(buffer, 0, CHUNK_SIZE)) != -1) {
            tokenBucket.take(CHUNK_SIZE);
            final Chunk chunk = new Chunk(buffer, offset, bytesRead);
            outQueue.add(chunk);
            offset += bytesRead;
        }

        inputStream.close();
        httpURLConnection.disconnect();
    }

    @Override
    public void run() {
        Range range;
        // while there are still missing Ranges
        while ((range = downloadableMetadata.getMissingRange()) != null) {
            try {
                this.downloadRange(range);
            } catch (IOException e) {
                System.err.println("HTTPRangeGetter: IoException");
                // downloader will fail safely
            }
        }
    }


}
