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

    private Type type = Type.REGULAR;
    private byte[] data;
    private long offset; //from beginning of file, ie. number of startbyte
    private int size_in_bytes; // we need this because the data array is not guaranteed to be fully filled

    public Chunk(byte[] data, long offset, int size_in_bytes) {
        this.data = data != null ? data.clone() : null;
        this.offset = offset;
        this.size_in_bytes = size_in_bytes;
    }

    public byte[] getData() {
        if (data.length != size_in_bytes) {
            byte[] truncatedData = new byte[size_in_bytes];
            System.arraycopy(this.data, 0, truncatedData, 0, size_in_bytes);
            return truncatedData;
        }
        return data;
    }

    public long getOffset() {
        return offset;
    }

    public int getSize_in_bytes() {
        return size_in_bytes;
    }

    public void setAsFinishedMarker() {
        type = Type.FINISHED_MARKER;
    }

    public boolean isFinishedMarker() {
        return this.type.equals(Type.FINISHED_MARKER);
    }

    @Override
    public String toString() {
        return "Chunk offset: " + this.offset + " size: " + this.size_in_bytes + " byte";
    }
}
