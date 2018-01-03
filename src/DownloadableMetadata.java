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

    private long fileSize;

    List<Range> downloadedRanges = new ArrayList<>();

    DownloadableMetadata(URL url, long fileSize) {
        this.filename = url.getFile().substring(url.getFile().lastIndexOf("/")+ 1, url.getFile().length());

        this.fileSize = fileSize;

        /** TODO terrible hack, mark the lower/upper border of the file size by additional of the ranges
         * lower border is: -1 and upper border is: file size */
        this.addRange(new Range(fileSize+1, Long.MAX_VALUE));
        this.addRange(new Range(Long.MIN_VALUE, -1L));
    }

    private static String getMetadataName(String filename) {
        return filename + ".metadata";
    }

    void addRange(Range range) {
        this.downloadedRanges.add(range);
    }

    String getFilename() {
        return filename;
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
                break;
            }
        }
    }
}
