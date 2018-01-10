import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Describes a file's metadata: URL, file name, size, and which parts already downloaded to disk.
 *
 * The metadata (or at least which parts already downloaded to disk) is constantly stored safely in disk.
 * When constructing a new metadata object, we first check the disk to load existing metadata.
 *
 * CHALLENGE: try to avoid metadata disk footprint of O(n) in the average case
 * HINT: avoid the obvious bitmap solution, and think about ranges...
 */
public class DownloadableMetadata {

    private final String filenameWithoutExtension;

    private String filenameWithExtension;

    private long fileSize;

    ArrayList<Range> toDownloadRanges = new ArrayList<>();

    long bytesDownloaded;

    File thisFile;

    /* Flag for alternating between 0 and 1, the downloaded Ranges are saved in two files (extension meta0/meta1) */
    AtomicBoolean fileCounter = new AtomicBoolean(true);

    DownloadableMetadata(URL url, long fileSize) {
        this.filenameWithExtension = url.getFile().substring(url.getFile().lastIndexOf("/")+ 1, url.getFile().length());
        this.filenameWithoutExtension = this.filenameWithExtension.substring(0, this.filenameWithExtension.lastIndexOf("."));
        this.fileSize = fileSize;
        /* for safety we persist the metadata alternating in two files
        * if one is corrupted during saving, the other is still readable */
        File file0 = new File(this.filenameWithoutExtension + ".meta0");
        File file1 = new File(this.filenameWithoutExtension + ".meta1");

        if (checkFileCanRead(file1)) {
            this.thisFile = file1;
        } else {
            this.thisFile = file0;
        }

        if (thisFile.exists()) {
            System.out.println("DownloadableMetadata: Found a metadata file, continuing download...");
            read(thisFile);
            this.bytesDownloaded = initDownloadStatus();
        }

        
    }

    private long initDownloadStatus() {
        long status = 0;
        for (Range range : this.toDownloadRanges) {
            status += range.getLength();
        }
        
        return this.fileSize - status;
    }

    private void read(File file) {
        try {
            FileInputStream fileInputStream = new FileInputStream(file);
            ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream);
            this.toDownloadRanges = (ArrayList<Range>) objectInputStream.readObject();
            
            fileInputStream.close();
            objectInputStream.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    void addRange(Range range) {
        this.toDownloadRanges.add(range);
    }

    String getFilenameWithExtension() {
        return filenameWithExtension;
    }

    long getFileSize() {
        return fileSize;
    }

    public List<Range> getMissingRanges() {
    	return this.toDownloadRanges;
    }

    public void updateDownloadedRange(long currentPosition, long newPosition) {
	    for (Range range : this.toDownloadRanges) {
	        if (range.getStart() == currentPosition) {
	        	if (range.getEnd() == newPosition - 1) {
	        		// range is unique
	        		this.toDownloadRanges.remove(range);
	        	} else {
		            range.setStart(newPosition);
		            this.bytesDownloaded += newPosition - currentPosition;
	        	}
	        	break;
	        }
		}
        saveToFile();
    }

    private void saveToFile() {
        try {
            File file = new File(this.filenameWithoutExtension + ".meta" + (this.fileCounter.get() ? "0" : "1"));
            FileOutputStream fileOutputStream = new FileOutputStream(file);
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream);
            objectOutputStream.writeObject(this.toDownloadRanges);
            objectOutputStream.close();
            fileOutputStream.close();
            this.fileCounter.set(!this.fileCounter.get());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean checkFileCanRead(File file){
        if (!file.exists())
            return false;
        if (!file.canRead())
            return false;
        try {
            FileReader fileReader = new FileReader(file.getAbsolutePath());
            fileReader.read();
            fileReader.close();
        } catch (Exception e) {
            System.err.println("Exception when checked file can read with message: " + e.getMessage());
            return false;
        }
        return true;
    }

    public void cleanUpMetadata() {
        File file0 = new File(this.filenameWithoutExtension + ".meta0");
        file0.delete();
        File file1 = new File(this.filenameWithoutExtension + ".meta1");
        file1.delete();
    }

    public void deleteRange(Range range) {
        this.toDownloadRanges.remove(range);
    }
}
