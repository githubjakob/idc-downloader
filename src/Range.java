import java.io.Serializable;

/**
 * Describes a simple range, with a start, an end, and a length
 */
class Range implements Serializable {
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
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
        return length;
    }

    /*@Override
    public int compareTo(Object o) {
        Range other = (Range) o;
        return this.getStart().compareTo(other.getStart());
    }

    @Override
    public boolean equals(Object o) {
        Range other = (Range) o;
        return this.getStart().equals(other.getStart()) && this.getEnd().equals(other.getEnd());
    }*/

    public void setEnd(Long end) {
        this.length += end - this.end;
        this.end = end;
    }
    
    public void setStart(Long start) {
    	this.length = this.end - start;
    	this.start = start;
    }
}
