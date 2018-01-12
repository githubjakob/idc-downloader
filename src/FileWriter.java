import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.concurrent.BlockingQueue;

/**
 * This class takes chunks from the queue, writes them to disk and updates the file's metadata.
 *
 * NOTE: make sure that the file interface you choose writes every update to the file's content or metadata
 *       synchronously to the underlying storage device.
 */
public class FileWriter implements Runnable {

    private final BlockingQueue<Chunk> chunkQueue;
    private DownloadableMetadata downloadableMetadata;

    public FileWriter(DownloadableMetadata downloadableMetadata, BlockingQueue<Chunk> chunkQueue) {
        this.chunkQueue = chunkQueue;
        this.downloadableMetadata = downloadableMetadata;
    }

    private void writeChunks() throws IOException, InterruptedException {
        File file = new File(downloadableMetadata.getFilenameWithExtension());
        RandomAccessFile downloadFile = new RandomAccessFile(file, "rw");

        while (true) {
            final Chunk chunk = chunkQueue.take();

            if (chunk.isFinishedMarker()) break;

            long pointerBefore = chunk.getOffset();

            downloadFile.seek(pointerBefore); // set the pointer to the end of the last data chunk
            downloadFile.write(chunk.getData());

            long pointerAfter = downloadFile.getFilePointer();
            downloadableMetadata.updateDownloadedRange(pointerBefore, pointerAfter);
        }
        //System.out.println("FileWriter: Found finished marker in queue, closing file.");
        downloadFile.close();
    }

    @Override
    public void run() {
        try {
            this.writeChunks();
        } catch (IOException e) {
            System.err.println("FileWriter: IoException occurred.");
            IdcDm.endDownload();
        } catch (InterruptedException e) {
            System.err.println("FileWriter: InterruptedException occurred.");
            IdcDm.endDownload();
        }
    }
}
