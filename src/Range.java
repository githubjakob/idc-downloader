import java.io.Serializable;

/**
 * Describes a simple range, with a start, an end, and a length
 */
class Range implements Serializable {
    
	private static final long serialVersionUID = 1L;

	private Long start; // 0 based

    private Long end;

    private Long length;

    private Boolean inUse; // true, when rangeGetter is download this range, false otherwise

    Range(Long start, Long end) {
        this.start = start;
        this.end = end;
        this.length = end - start + 1;
        this.inUse = false;
    }

    Long getStart() {
        return this.start;
    }

    Long getEnd() {
        return this.end;
    }

    Long getLength() {
        return this.length;
    }

    Boolean getInUse() {
    	return this.inUse;
    }
    
    void setInUse(Boolean bool) {
    	this.inUse = bool;
    }
    
    void setStart(Long start) {
    	this.length = this.end - start + 1;
    	this.start = start;
    }
}
