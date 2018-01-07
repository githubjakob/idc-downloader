import java.util.List;

/**
 * Created by jakob on 07.01.18.
 */
public class DownloadStatus implements Runnable  {

    private final DownloadableMetadata downloadableMetadata;

    DownloadStatus(DownloadableMetadata downloadableMetadata) {
        this.downloadableMetadata = downloadableMetadata;
    }


    @Override
    public void run() {
        while (true) {
            if (downloadableMetadata.bytesDownloaded >= downloadableMetadata.getFileSize()) {
                break;
            }

            System.out.println("Percent downloaded: " + downloadableMetadata.bytesDownloaded * 100.0f / downloadableMetadata.getFileSize());

            try {
                Thread.sleep(2000L);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }
    }
}
