import java.io.Serializable;

/**
 * Describes a simple range, with a start, an end, and a length
 */
class Range implements Serializable {
    
	private static final long serialVersionUID = 1L;
	private Long start; // 0 based
    private Long end;
    private Long length;
    private Boolean inUse;

    public Range(Long start, Long end) {
        this.start = start;
        this.end = end;
        this.length = end - start + 1;
        this.inUse = false;
    }

    public void incrementEnd(long offset) {
        this.end += offset;
    }

    public Long getStart() {
        return this.start;
    }

    public Long getEnd() {
        return this.end;
    }

    public Long getLength() {
        return this.length;
    }
    
    public Boolean getInUse() {
    	return this.inUse;
    }
    
    public void setInUse(Boolean bool) {
    	this.inUse = bool;
    }
    
    public void setStart(Long start) {
    	this.length = this.end - start + 1;
    	this.start = start;
    }
}
