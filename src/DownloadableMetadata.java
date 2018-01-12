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

    private ArrayList<Range> missingRanges = new ArrayList<>();

    private long bytesDownloaded;

    private File metaFile;

    /* Flag for alternating between 0 and 1, the downloaded Ranges are saved in two files (extension meta0/meta1) */
    private AtomicBoolean fileCounter = new AtomicBoolean(true);

    public DownloadableMetadata(URL url, long fileSize) {
        this.filenameWithExtension = url.getFile().substring(url.getFile().lastIndexOf("/")+ 1, url.getFile().length());
        this.filenameWithoutExtension = this.filenameWithExtension.substring(0, this.filenameWithExtension.lastIndexOf("."));
        this.fileSize = fileSize;
        /* for safety we persist the metadata alternating in two files
        * if one is corrupted during saving, the other is still readable */
        File file0 = new File(this.filenameWithoutExtension + ".meta0");
        File file1 = new File(this.filenameWithoutExtension + ".meta1");

        if (checkFileCanRead(file1)) {
            this.metaFile = file1;
        } else if (checkFileCanRead(file0)){
            this.metaFile = file0;
        }

        if (metaFile != null && metaFile.exists()) {
            System.err.println("Found a metadata file, continuing download...");
            read(metaFile);
            this.bytesDownloaded = initDownloadStatus();
        } 
    }

    private long initDownloadStatus() {
        long status = 0;
        for (Range range : this.missingRanges) {
            status += range.getLength();
        }
        
        return this.fileSize - status;
    }

    public long getBytesDownloaded() {
    	return this.bytesDownloaded;
    }
    
    @SuppressWarnings("unchecked") // due to typeCheck warning when casting object to ArrayList
	private void read(File file) {
        try {
            FileInputStream fileInputStream = new FileInputStream(file);
            ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream);
            this.missingRanges = (ArrayList<Range>) objectInputStream.readObject();
            fileInputStream.close();
            objectInputStream.close();
        } catch (FileNotFoundException e) {
            System.err.println("DownloadableMetadata: FileNotFoundException Occured");
            IdcDm.endDownload();
        } catch (IOException e) {
        	System.err.println("DownloadableMetadata: IOException Occured");
        	IdcDm.endDownload();
        } catch (ClassNotFoundException e) {
        	System.err.println("DownloadableMetadata: ClassNotFoundException Occured");
        	IdcDm.endDownload();
        } 
    }

    void addRange(Range range) {
        this.missingRanges.add(range);
    }

    String getFilenameWithExtension() {
        return filenameWithExtension;
    }

    long getFileSize() {
        return fileSize;
    }

    public List<Range> getMissingRanges() {
    	return this.missingRanges;
    }
    
    synchronized public Range getMissingRange() {
    	
    	for (Range range : this.missingRanges) {
    		if (!range.getInUse()) {
    			range.setInUse(true);
    			return range;
    		}
    	}
    	return null;
    }

    synchronized public void updateDownloadedRange(long currentPosition, long newPosition) {
	    for (Range range : this.missingRanges) {
	        if (range.getStart() == currentPosition) {
	        	if (range.getEnd() == newPosition - 1) {
	        		// range is unique
	        		this.missingRanges.remove(range);
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
            objectOutputStream.writeObject(this.missingRanges);
            objectOutputStream.flush();
            objectOutputStream.close();
            fileOutputStream.flush();
            fileOutputStream.close();
            
            this.fileCounter.set(!this.fileCounter.get());
        } catch (FileNotFoundException e) {
            System.err.println("DownloadableMetadata: FileNotFoundException Occured");
            IdcDm.endDownload();
        } catch (IOException e) {
        	System.err.println("DownloadableMetadata: IOException Occured");
        	IdcDm.endDownload();
        }
    }

    public boolean checkFileCanRead(File file){
        if (!file.exists())
            return false;
        if (!file.canRead())
            return false;
        try {
            FileReader fileReader = new FileReader(file.getAbsolutePath());
            if (fileReader.read() == -1) {
            	fileReader.close();
            	return false;
            }
            // fileReader.read();
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
        this.missingRanges.remove(range);
    }
}
