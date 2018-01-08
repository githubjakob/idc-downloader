import java.io.File;
import java.io.FileNotFoundException;
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

    FileWriter(DownloadableMetadata downloadableMetadata, BlockingQueue<Chunk> chunkQueue) {
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
        System.out.println("FileWriter: Found finished marker in queue, closing file.");
        downloadFile.setLength(this.downloadableMetadata.getFileSize());
        downloadFile.close();
    }

    @Override
    public void run() {
        try {
            this.writeChunks();
        } catch (IOException e) {
            e.printStackTrace();
            //TODO
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
