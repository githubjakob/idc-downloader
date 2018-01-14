/**
 * Runnable to print out the percentage of already downloaded bytes
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
    	// Prints out the percentage with no repetition of percentages
        while (!IdcDm.downloadStopped.get()) {
            if (downloadableMetadata.getBytesDownloaded() >= downloadableMetadata.getFileSize()) {
                break;
            }
            
            long percent = downloadableMetadata.getBytesDownloaded() * 100 / downloadableMetadata.getFileSize();
            
            if (percent != percentage) {
            	this.percentage = percent;
            	System.err.println("Downloaded " + percent + "%");
            }
        }
    }
}
