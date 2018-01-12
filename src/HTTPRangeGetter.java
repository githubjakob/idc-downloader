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
    static final int CHUNK_SIZE = 4096;
    private static final int CONNECT_TIMEOUT = 500;
    private static final int READ_TIMEOUT = 2000;
    private static final String GET = "GET";
    private final URL url;
    private final Range range;
    private final BlockingQueue<Chunk> outQueue;
    private TokenBucket tokenBucket;

    public HTTPRangeGetter(
            URL url,
            Range range,
            BlockingQueue<Chunk> outQueue,
            TokenBucket tokenBucket) {
        this.url = url;
        this.range = range;
        this.outQueue = outQueue;
        this.tokenBucket = tokenBucket;
    }

    private void downloadRange() throws IOException {

        final HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();

        httpURLConnection.setRequestMethod(GET);
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
        long offset = this.range.getStart();

        while ((bytesRead = inputStream.read(buffer, 0, CHUNK_SIZE)) != -1) {
            tokenBucket.take(CHUNK_SIZE);

            final Chunk chunk = new Chunk(buffer, offset, bytesRead);
            outQueue.add(chunk);
            offset += bytesRead;
            //System.out.println("HTTPRangeGetter: Reading from stream " + bytesRead + ", offset: " + offset);
        }

        inputStream.close();
        httpURLConnection.disconnect();
    }

    @Override
    public void run() {
        try {
            this.downloadRange();
        } catch (IOException e) {
        	// If a worker fails, we have the pool that will start another thread, taking over the range
        	range.setInUse(false);
        }
    }

}
