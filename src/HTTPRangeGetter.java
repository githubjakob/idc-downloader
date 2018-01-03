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

        final Socket socket = new Socket(url.getHost(), 80);

        final BufferedOutputStream outputStream = new BufferedOutputStream(socket.getOutputStream());
        final BufferedInputStream inputStream = new BufferedInputStream(socket.getInputStream());

        // do a GET and request a range from the file
        outputStream.write(getRequest(url, range).getBytes());
        outputStream.flush();

        // strip header from response
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

        // now get the data
        byte[] buffer = new byte[CHUNK_SIZE];
        int bytesRead;
        long offset = this.range.getStart();

        while(true){
        	tokenBucket.take(CHUNK_SIZE);

            bytesRead = inputStream.read(buffer, 0, CHUNK_SIZE);

            if (bytesRead == -1) break;

            final Chunk chunk = new Chunk(buffer, offset, bytesRead);
            outQueue.add(chunk);
            offset += bytesRead;
            //System.out.println("HTTPRangeGetter: Reading from stream " + bytesRead + ", offset: " + offset);
        }

        outputStream.close();
        inputStream.close();
        socket.close();
    }

    private String getRequest(final URL url, final Range range) {
        return "GET " + url.getPath() + " HTTP/1.1\r\n" +
                "Host: " + url.getHost() + "\r\n" +
                "Range: bytes=" + range.getStart() + "-" + range.getEnd() +"\r\n\r\n";
    }

    @Override
    public void run() {
        try {
            this.downloadRange();
        } catch (IOException e) {
            System.err.println("HTTPRangeGetter: Error ocurred during downloading");
            e.printStackTrace();
            //TODO
        }
    }

}
