import java.io.Serializable;

/**
 * Describes a simple range, with a start, an end, and a length
 */
class Range implements Comparable, Serializable {
    private Long start;
    private Long end;
    private Long length;

    public Range(Long start, Long end) {
        this.start = start;
        this.end = end;
        this.length = end - start;
    }

    void incrementEnd(long offset) {
        this.end += offset;
    }

    Long getStart() {
        return start;
    }

    Long getEnd() {
        return end;
    }

    Long getLength() {
        return end - start + 1;
    }

    @Override
    public int compareTo(Object o) {
        Range other = (Range) o;
        return this.getStart().compareTo(other.getStart());
    }

    @Override
    public boolean equals(Object o) {
        Range other = (Range) o;
        return this.getStart().equals(other.getStart()) && this.getEnd().equals(other.getEnd());
    }

    public void setEnd(Long end) {
        this.length += end - this.end;
        this.end = end;
    }
}
