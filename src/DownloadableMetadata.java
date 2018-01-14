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

    /* for safety we persist the metadata alternating in two files
    * if one is corrupted during saving, the other is still readable */
    private final File file0;

    private final File file1;

    private String filenameWithExtension;

    private final String filenameWithoutExtension;

    private long fileSize;

    private ArrayList<Range> missingRanges = new ArrayList<>();

    private long bytesDownloaded;

    /* fixed Range size to initialize the ranges for the first download */
    final static long RANGE_SIZE = 491520; //480kb

    /* Flag for alternating between 0 and 1, the downloaded Ranges are saved in two files (extension meta0/meta1) */
    private AtomicBoolean fileCounter = new AtomicBoolean(true);

    public DownloadableMetadata(URL url, long fileSize) {
        this.filenameWithExtension = url.getFile().substring(url.getFile().lastIndexOf("/")+ 1, url.getFile().length());
        this.filenameWithoutExtension = this.filenameWithExtension.substring(0, this.filenameWithExtension.lastIndexOf("."));
        this.fileSize = fileSize;

        this.file0 = new File(this.filenameWithoutExtension + ".meta0");
        this.file1 = new File(this.filenameWithoutExtension + ".meta1");

        if (file0.exists() || file1.exists()) {
            System.err.println("Found a metadata file, continuing download...");
            read(this.file0);
            this.bytesDownloaded = initDownloadStatus();
        }

        if (missingRanges.size() == 0) {
            initMissingRanges();
        }

        setDownloadRangesNotInUse();
    }

    /**
     * mark all ranges as not in use before download starts
     */
    private void setDownloadRangesNotInUse() {
        for (Range range : missingRanges) {
            range.setInUse(false);
        }
    }

    /**
     * A first time download - create the ranges for all workers using the fixed RANGE_SIZE
     */
    private void initMissingRanges() {

        long numRanges = this.fileSize / RANGE_SIZE;
        long remainder = this.fileSize % RANGE_SIZE;

        if (numRanges == 0) { // in case the file size is smaller than the range size
            long rangeStart = 0;
            long rangeEnd = this.fileSize - 1;
            Range workerRange = new Range(rangeStart, rangeEnd);
            this.addRange(workerRange);
        } else {
            for (int i = 0; i < numRanges; i++) {

                long rangeStart = i * RANGE_SIZE;
                long rangeEnd = ((i + 1) * RANGE_SIZE) - 1;

                if (i == numRanges - 1) {
                    rangeEnd += remainder;
                }

                Range workerRange = new Range(rangeStart, rangeEnd);
                this.addRange(workerRange);
            }
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
        } catch (ClassNotFoundException|IOException e) {
           if (file == this.file0) { // try other file
               System.out.println("trying other file");
               read(this.file1);
           }
        	System.err.println("DownloadableMetadata: IOException/ClassNotFoundExcpetion occured reading file");
        	IdcDm.exitWithFailure();
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

    synchronized public List<Range> getMissingRanges() {
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

    synchronized private void saveToFile() {
        File file = new File(this.filenameWithoutExtension + ".meta" + (this.fileCounter.get() ? "0" : "1"));
        ObjectOutputStream objectOutputStream = null;
        try {
            objectOutputStream = new ObjectOutputStream(new FileOutputStream(file));
            objectOutputStream.writeObject(this.missingRanges);
            objectOutputStream.flush();
            this.fileCounter.set(!this.fileCounter.get());
        } catch (FileNotFoundException e) {
            System.err.println("DownloadableMetadata: FileNotFoundException occurred");
            IdcDm.exitWithFailure();
        } catch (IOException e) {
        	System.err.println("DownloadableMetadata: IOException occurred during saving file");
        	IdcDm.exitWithFailure();
        } finally {
            if (objectOutputStream != null) {
                try {
                    objectOutputStream.close();
                } catch (IOException ex) {
                    // ignore
                }
            }

        }
    }

    public void cleanUpMetadata() {
        File file0 = new File(this.filenameWithoutExtension + ".meta0");
        file0.delete();
        File file1 = new File(this.filenameWithoutExtension + ".meta1");
        file1.delete();
    }
}
