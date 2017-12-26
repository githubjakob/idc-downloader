import java.net.URL;
import java.util.*;

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
    private String metadataFilename;
    private String filename;
    private URL url;
    private long fileSize;

    List<Range> downloadedRanges = new ArrayList<>();

    /** Instead of Range objects, I prefer to store the already downloaded ranges in a map
     * the mapping is: first byte -> first free byte / last byte + 1  */
    TreeMap<Long, Long> pointers = new TreeMap<>();

    DownloadableMetadata(URL url, long fileSize) {

        this.url = url;

        // TODO fix this
        this.filename = "movie.avi";

        this.fileSize = fileSize;

        /** terrible hack, mark the lower/upper border of the file size by additional of the ranges
         * lower border is: -1 and upper border is: file size */
        this.addRange(new Range(fileSize, fileSize));
        this.addRange(new Range(-1L, 0L));

    }

    private static String getMetadataName(String filename) {
        return filename + ".metadata";
    }

    private static String getName(String path) {
        return path.substring(path.lastIndexOf('/') + 1, path.length());
    }

    public void addRange(Range range) {
        this.pointers.put(range.getEnd(), range.getStart());
        this.downloadedRanges.add(range);
    }

    String getFilename() {
        return filename;
    }

    long getFileSize() {return fileSize; }

    boolean isCompleted() {
        //TODO
        return false;
    }

    void delete() {
        //TODO
    }

    /**
     * Gets a list of all missing ranges, empty list if no missing ranges
     */
    public List<Range> getMissingRanges() {

        updateDownloadedRanges();

        List<Range> result = new ArrayList<>();

        // sort the downloaded Ranges accoring to the starts
        Collections.sort(this.downloadedRanges);

        for (int i = 0; i < this.downloadedRanges.size() - 1; i++) {

            // for two subsequent Ranges ...
            Range first = this.downloadedRanges.get(i);
            Range second = this.downloadedRanges.get(i+1);

            // ... if start and end are two following whole numbers then there is a missing range
            if (!(first.getEnd().equals(second.getStart() - 1))) {
                result.add(new Range(first.getEnd() + 1, second.getStart() - 1));
            }
        }


        return result;
    }

    private void updateDownloadedRanges() {

        for (Map.Entry<Long, Long> entry : this.pointers.entrySet()) {
            final long beginDownloadedRange = entry.getValue(); // first downloaded byte
            final long endDownloadedRange = entry.getKey() - 1; // last downloaded byte
            for (Range range : this.downloadedRanges) {
                final long rangeStart = range.getStart();
                if (beginDownloadedRange == rangeStart) {
                    range.setEnd(endDownloadedRange); //// update the range with the actual downloaed offset
                    break;
                }
            }

        }
    }

    URL getUrl() {
        return url;
    }

    public void addPointer(long rangeStart) {
        this.pointers.put(rangeStart, rangeStart);
    }

    public void updateDownloadedRange(long currentPosition, long newPosition) {
        /** Rename the key -> value pair, where the key is the end of the range and the value is the beginning*/
        this.pointers.put(newPosition, this.pointers.remove(currentPosition));
        /*System.out.print("Pointers: ");
        for (Map.Entry<Long, Long> entry : this.pointers.entrySet()) {
            System.out.print(entry.getKey() + ", " + entry.getValue() + "\n");
        }
        System.out.println("\n");*/
    }

    public void validateDownload() {
        try { // wait to make sure all threads are terminated
            Thread.currentThread().sleep(20L);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        updateDownloadedRanges();
        List<Range> ranges = getMissingRanges();

        if (ranges.size() == 0) {
            System.out.println("DownloadableMetadata: File is valid.");
        } else {
            System.err.println("DownloadableMetadata: File is not valid.");
        }
    }
}
