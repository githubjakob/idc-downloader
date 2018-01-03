import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Only gets the header of the file we want to download
 */
public class HttpHeadGetter {

    long fileSize = -1;

    public long getFileSize() {
        return this.fileSize;
    }

    public HttpHeadGetter(URL url) {
        final HttpURLConnection httpURLConnection;
        try {
            httpURLConnection = (HttpURLConnection) url.openConnection();

            httpURLConnection.setRequestMethod("HEAD");

            int responseCode = httpURLConnection.getResponseCode();

            if (responseCode != HttpURLConnection.HTTP_OK) {
                // TODO
            }
            this.fileSize = httpURLConnection.getContentLength();

            httpURLConnection.disconnect();

        } catch (IOException e) {
            e.printStackTrace();

            // TODO
        }
    }

}
