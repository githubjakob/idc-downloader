/**
 * A chunk of data file
 *
 * Contains an offset, bytes of data, and size
 */
class Chunk {

    private enum Type {

        FINISHED_MARKER,

        REGULAR
    }

    /* A chunk can either be a regular chunk or a finished marker to signal the filewriter it can terminate */
    private Type type = Type.REGULAR;

    private byte[] data;

    private long offset; //from beginning of file, ie. number of startbyte

    private int size_in_bytes; // we need this because the data array is not guaranteed to be fully filled

    Chunk(byte[] data, long offset, int size_in_bytes) {
        this.data = data != null ? data.clone() : null;
        this.offset = offset;
        this.size_in_bytes = size_in_bytes;
    }

    byte[] getData() {
        if (data.length != size_in_bytes) { // truncate the data in case it is not fully filled
            byte[] truncatedData = new byte[size_in_bytes];
            System.arraycopy(this.data, 0, truncatedData, 0, size_in_bytes);
            return truncatedData;
        }
        return data;
    }

    long getOffset() {
        return offset;
    }

    void setAsFinishedMarker() {
        type = Type.FINISHED_MARKER;
    }

    boolean isFinishedMarker() {
        return this.type.equals(Type.FINISHED_MARKER);
    }

    @Override
    public String toString() {
        return "Chunk offset: " + this.offset + " size: " + this.size_in_bytes + " byte";
    }
}
