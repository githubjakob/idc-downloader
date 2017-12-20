import java.io.*;
import java.net.Socket;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Only gets the header of the file we want to download
 */
public class HttpHeadGetter {

    String header = "";

    long fileSize = -1;

    int responseStatus = -1;

    public long getFileSize() {
        return this.fileSize;
    }

    public HttpHeadGetter(URL url) {

        Socket socket = null;
        try {
            socket = new Socket(url.getHost(), 80);

        BufferedOutputStream out = new BufferedOutputStream(socket.getOutputStream());
        BufferedInputStream inputStream = new BufferedInputStream(socket.getInputStream());

        final String get = "GET " + url.getPath() + " HTTP/1.1\r\n" +
                "Host: " + url.getHost() + "\r\n\r\n";

        out.write(get.getBytes());
        out.flush();


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


        //extract some usefull information from the header

        Pattern statusPattern = Pattern.compile("HTTP\\/1.1\\s(.*)\\sOK");
        Matcher statusMatcher = statusPattern.matcher(responseHeader);

        if (statusMatcher.find()) {
            this.responseStatus = Integer.valueOf(statusMatcher.group(1));
        }

        Pattern contentLengthPattern = Pattern.compile("Content-Length:\\s(.*)\\r\\n");
        Matcher contentLengthMatcher = contentLengthPattern.matcher(responseHeader);

        if (contentLengthMatcher.find()) {
            this.fileSize = Long.valueOf(contentLengthMatcher.group(1));
        }

        //System.out.println(responseHeader);

        } catch (IOException e) {
            e.printStackTrace();
        }


    }

}
