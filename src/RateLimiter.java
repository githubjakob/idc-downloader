/**
 * A token bucket based rate-limiter.
 *
 * This class should implement a "soft" rate limiter by adding maxBytesPerSecond tokens to the bucket every second,
 * or a "hard" rate limiter by resetting the bucket to maxBytesPerSecond tokens every second.
 */
public class RateLimiter implements Runnable {
    private final TokenBucket tokenBucket;
    private final Long maxBytesPerSecond;

    RateLimiter(TokenBucket tokenBucket, Long maxBytesPerSecond) {
        this.tokenBucket = tokenBucket;
        this.maxBytesPerSecond = (maxBytesPerSecond == null) ? Long.MAX_VALUE : maxBytesPerSecond;
    }

    // I AM NOT SURE WHAT SHOULD BE THE CONDITION TO STOP THIS THREAD
    
    @Override
    public void run() {
        //TODO
    	
    	// I ASSUMED THAT WHETHER OR NOT maxBytesPerSecond IS NULL DECIDES IF IT'S SOFT OR HARD
    	
		while (true) {
			if (tokenBucket.terminated()) return;
			// use soft limiter if maxBytesPerSecond smaller than chunk size
			if (maxBytesPerSecond < HTTPRangeGetter.CHUNK_SIZE) {
				tokenBucket.add(maxBytesPerSecond);
				
			// otherwise use hard limiter
			} else {
				tokenBucket.set(maxBytesPerSecond);
			}
    		try {
				Thread.sleep(1000); // adding maxBps to token bucket every second
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}	
		}
    	
    
    }
}
