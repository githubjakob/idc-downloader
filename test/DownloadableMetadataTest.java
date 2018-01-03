import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

/**
 * Created by jakob on 18.12.17.
 */
public class DownloadableMetadataTest {

    DownloadableMetadata downloadableMetadata;

    @Before
    public void init() {
        try {
            downloadableMetadata = new DownloadableMetadata(new URL("http://www.test.com/test.avi"), 100);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void getSimpleMissingRage() {
        downloadableMetadata.addRange(new Range(0L, 19L));
        downloadableMetadata.addRange(new Range(80L, 99L));

        Range result = downloadableMetadata.getMissingRanges().get(0);

        Assert.assertEquals(result, new Range(20L,79L));
    }

    @Test
    public void getMissingRanges_downloadNotYetStarted() {
        Range result = downloadableMetadata.getMissingRanges().get(0);

        Assert.assertEquals(result, new Range(0L,99L));
    }

    @Test
    public void getSimpleMissingRage_2() {
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
        downloadableMetadata.addRange(new Range(86L, 99L));
        downloadableMetadata.addRange(new Range(20L, 79L));
        downloadableMetadata.addRange(new Range(0L, 19L));
        downloadableMetadata.addRange(new Range(80L, 85L));

        List<Range> result = downloadableMetadata.getMissingRanges();

        Assert.assertTrue(result.isEmpty());
    }

    @Test
    public void getSimpleMissingRage_4() {
        downloadableMetadata.addRange(new Range(90L, 99L));
        downloadableMetadata.addRange(new Range(0L, 19L));
        downloadableMetadata.addRange(new Range(20L, 85L));

        Range result = downloadableMetadata.getMissingRanges().get(0);

        Assert.assertEquals(result, new Range(86L,89L));

    }
}
