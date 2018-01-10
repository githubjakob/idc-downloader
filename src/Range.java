import java.io.Serializable;

/**
 * Describes a simple range, with a start, an end, and a length
 */
class Range implements Serializable {
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private Long start; // 0 based
    private Long end;
    private Long length;

    public Range(Long start, Long end) {
        this.start = start;
        this.end = end;
        this.length = end - start + 1;
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
    
    public void setStart(Long start) {
    	this.length = this.end - start + 1;
    	this.start = start;
    }
}
