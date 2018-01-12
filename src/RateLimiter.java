/**
 * A token bucket based rate-limiter.
 *
 * This class should implement a "soft" rate limiter by adding maxBytesPerSecond tokens to the bucket every second,
 * or a "hard" rate limiter by resetting the bucket to maxBytesPerSecond tokens every second.
 */
public class RateLimiter implements Runnable {
    private final TokenBucket tokenBucket;
    private final Long maxBytesPerSecond;

    public RateLimiter(TokenBucket tokenBucket, Long maxBytesPerSecond) {
        this.tokenBucket = tokenBucket;
        this.maxBytesPerSecond = (maxBytesPerSecond == null) ? Long.MAX_VALUE : maxBytesPerSecond;
    }

    
    @Override
    public void run() {
    	
		while (true) {
			if (tokenBucket.terminated()) break;

			// use soft limiter if maxBytesPerSecond smaller than chunk size
			if (maxBytesPerSecond < HTTPRangeGetter.CHUNK_SIZE) {
				tokenBucket.add(maxBytesPerSecond);
				
			// otherwise use hard limiter
			} else {
				tokenBucket.set(maxBytesPerSecond);
			}
    		try {
				Thread.sleep(1000); // adding/reseting maxBps to token bucket every second
			} catch (InterruptedException e) {
				System.err.println("RateLimiter: InterruptedException occured");
				IdcDm.endDownload();
			}
		}
		//System.out.println("RateLimiter: Exiting.");
    }
}
