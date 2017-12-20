/**
 * A chunk of data file
 *
 * Contains an offset, bytes of data, and size
 */
class Chunk {
    private byte[] data;
    private long offset; //from beginning of file, ie. number of startbyte
    private int size_in_bytes; // we need this because the data array is not guaranteed to be fully filled

    Chunk(byte[] data, long offset, int size_in_bytes) {
        this.data = data != null ? data.clone() : null;
        this.offset = offset;
        this.size_in_bytes = size_in_bytes;
    }

    byte[] getData() {
        return data;
    }

    long getOffset() {
        return offset;
    }

    int getSize_in_bytes() {
        return size_in_bytes;
    }

    @Override
    public String toString() {
        return "Chunk offset: " + this.offset + " size: " + this.size_in_bytes + " byte";
    }
}
