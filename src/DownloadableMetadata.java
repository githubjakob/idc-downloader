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

    ArrayList<Range> downloadedRanges = new ArrayList<>();

    long bytesDownloaded;

    File thisFile;

    AtomicBoolean fileCounter = new AtomicBoolean(true);

    DownloadableMetadata(URL url, long fileSize) {
        this.filenameWithExtension = url.getFile().substring(url.getFile().lastIndexOf("/")+ 1, url.getFile().length());
        this.filenameWithoutExtension = this.filenameWithExtension.substring(0, this.filenameWithExtension.lastIndexOf(".") - 1);

        this.fileSize = fileSize;

        this.bytesDownloaded = 0;

        /** TODO terrible hack, mark the lower/upper border of the file size by additional of the ranges
         * lower border is: -1 and upper border is: file size */
        this.addRange(new Range(fileSize+1, Long.MAX_VALUE));
        this.addRange(new Range(Long.MIN_VALUE, -1L));

        /* for safety we persist the metadata alternating in two files
        * if one is corrupted during saving, the other is still readable */
        File file0 = new File(this.filenameWithoutExtension + ".meta0");
        File file1 = new File(this.filenameWithoutExtension + ".meta1");

        if (checkFileCanRead(file1)) {
            this.thisFile = file1;
            System.err.println("DownloadableMetadata: Metadata file1 is corrupted, reading file0...");
        } else {
            this.thisFile = file0;
        }

        if (thisFile.exists()) {
            System.out.println("DownloadableMetadata: Found a metadata file, continuing download...");
            read(thisFile);
        }
    }

    private void read(File file) {
        try {
            FileInputStream fileInputStream = new FileInputStream(file);
            ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream);
            this.downloadedRanges = (ArrayList<Range>) objectInputStream.readObject();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    private static String getMetadataName(String filename) {
        return filename + ".metadata";
    }

    void addRange(Range range) {
        this.downloadedRanges.add(range);
    }

    String getFilenameWithExtension() {
        return filenameWithExtension;
    }

    long getFileSize() {
        return fileSize;
    }

    boolean isCompleted() {
        return false;
    }

    void delete() {
        //TODO
    }

    /**
     * Get a list of all missing ranges, empty list if no missing ranges
     */
    public List<Range> getMissingRanges() {
        List<Range> result = new ArrayList<>();

        // sort the downloaded Ranges accoring to the starts
        Collections.sort(this.downloadedRanges);

        for (int i = 0; i < this.downloadedRanges.size() - 1; i++) {

            // for two subsequent Ranges ...
            Range first = this.downloadedRanges.get(i);
            Range second = this.downloadedRanges.get(i+1);

            // ... if start and end are not the same numbers then there is a missing range
            // TODO this "&&" is not so good
            if (!(first.getEnd().equals(second.getStart() - 1)) && !(first.getEnd().equals(second.getStart()))) {
                result.add(new Range(first.getEnd() + 1, second.getStart() - 1));
            }
        }

        return result;
    }

    public void updateDownloadedRange(long currentPosition, long newPosition) {
        for (Range range : this.downloadedRanges) {
            if (range.getEnd() == currentPosition) {
                range.setEnd(newPosition);
                this.bytesDownloaded += newPosition - currentPosition;
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
            objectOutputStream.writeObject(this.downloadedRanges);
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
}
