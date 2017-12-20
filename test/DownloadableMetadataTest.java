import org.junit.Assert;
import org.junit.Test;

import java.util.List;

/**
 * Created by jakob on 18.12.17.
 */
public class DownloadableMetadataTest {

    @Test
    public void getSimpleMissingRage() {
        DownloadableMetadata downloadableMetadata = new DownloadableMetadata(null, 100);

        downloadableMetadata.addRange(new Range(0L, 19L));
        downloadableMetadata.addRange(new Range(80L, 99L));

        Range result = downloadableMetadata.getMissingRanges().get(0);

        Assert.assertEquals(result, new Range(20L,79L));
    }

    @Test
    public void getMissingRanges_downloadNotYetStarted() {
        DownloadableMetadata downloadableMetadata = new DownloadableMetadata(null, 100);

        Range result = downloadableMetadata.getMissingRanges().get(0);

        Assert.assertEquals(result, new Range(0L,99L));
    }

    @Test
    public void getSimpleMissingRage_2() {
        DownloadableMetadata downloadableMetadata = new DownloadableMetadata(null, 100);

        downloadableMetadata.addRange(new Range(0L, 19L));
        downloadableMetadata.addRange(new Range(80L, 85L));

        // now this should be range from 86-99
        Range result = downloadableMetadata.getMissingRanges().get(0);

        // add it
        downloadableMetadata.addRange(result);

        Range result2 = downloadableMetadata.getMissingRanges().get(0);

        Assert.assertEquals(result, new Range(20L,79L));

    }

    @Test
    public void getSimpleMissingRage_noMissingRange() {
        DownloadableMetadata downloadableMetadata = new DownloadableMetadata(null, 100);

        downloadableMetadata.addRange(new Range(86L, 99L));
        downloadableMetadata.addRange(new Range(20L, 79L));
        downloadableMetadata.addRange(new Range(0L, 19L));
        downloadableMetadata.addRange(new Range(80L, 85L));


        List<Range> result = downloadableMetadata.getMissingRanges();

        Assert.assertTrue(result.isEmpty());
    }

    @Test
    public void getSimpleMissingRage_4() {
        DownloadableMetadata downloadableMetadata = new DownloadableMetadata(null, 100);

        downloadableMetadata.addRange(new Range(90L, 99L));
        downloadableMetadata.addRange(new Range(0L, 19L));
        downloadableMetadata.addRange(new Range(20L, 85L));

        Range result = downloadableMetadata.getMissingRanges().get(0);

        Assert.assertEquals(result, new Range(86L,89L));

    }
}
