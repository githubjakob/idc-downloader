import java.io.*;
import java.net.Socket;
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
    private final URL url;
    private final Range range;
    private final BlockingQueue<Chunk> outQueue;
    private TokenBucket tokenBucket;

    HTTPRangeGetter(
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

        Socket socket = new Socket(url.getHost(), 80);

        //socket.setSoTimeout(READ_TIMEOUT);

        BufferedOutputStream out = new BufferedOutputStream(socket.getOutputStream());
        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        BufferedInputStream inputStream = new BufferedInputStream(socket.getInputStream());

        final String get = "GET " + url.getPath() + " HTTP/1.1\r\n" +
                "Host: " + url.getHost() + "\r\n" +
                "Range: bytes=" + this.range.getStart() + "-" + this.range.getEnd() +"\r\n\r\n";

        //System.out.println("HTTPRangeGetter: Requesting");
        //System.out.println(get);

        out.write(get.getBytes());
        out.flush();

        // strip header of response
        String responseHeader = "";
        int nextByte;
        boolean readHeader = true;
        while (readHeader) {
            nextByte = inputStream.read();
            responseHeader = responseHeader + (char) nextByte;
            if (responseHeader.contains("\r\n\r\n")) {
                readHeader = false;
            }
        }

        //System.out.println("HTTPRangeGetter: Got response for query");
        //System.out.println(responseHeader);

        // now get the data
        byte[] buffer = new byte[CHUNK_SIZE];
        int bytesRead;
        long offset = this.range.getStart();

        while(true){
            bytesRead = inputStream.read(buffer, 0, CHUNK_SIZE);

            if (bytesRead == -1) {
                break;
            }

            final Chunk chunk = new Chunk(buffer, offset, bytesRead);
            outQueue.add(chunk);
            System.out.println("HTTPRangeGetter: Reading from stream " + bytesRead + ", offset: " + offset);
            offset += bytesRead;
            IdcDm.DOWNLOADING.set(true);
        }

        //System.out.println("HttpRangeGetter completed");
        IdcDm.DOWNLOADING.set(false);

        out.close();
        in.close();
        socket.close();

    }

    @Override
    public void run() {
        try {
            this.downloadRange();
        } catch (IOException e) {
            System.err.println("HTTPRangeGetter: Error ocurred during downloading, failing safely...");
            e.printStackTrace();
            //TODO
        } finally {
            IdcDm.DOWNLOADING.set(false);
        }
    }

}
