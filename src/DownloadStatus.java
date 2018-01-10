/**
 * Created by jakob on 07.01.18.
 */
public class DownloadStatus implements Runnable  {

    private final DownloadableMetadata downloadableMetadata;
    
    private long percentage;
    
    DownloadStatus(DownloadableMetadata downloadableMetadata) {
        this.downloadableMetadata = downloadableMetadata;
        this.percentage = -1;
    }


    @Override
    public void run() {
        while (!IdcDm.downloadStopped.get()) {
            if (downloadableMetadata.bytesDownloaded >= downloadableMetadata.getFileSize()) {
                break;
            }
            
            long percent = downloadableMetadata.bytesDownloaded * 100 / downloadableMetadata.getFileSize();
            
            if (percent != percentage) {
            	this.percentage = percent;
            	System.out.println("Downloaded " + percent + "%");
            }
        }
    }
}
