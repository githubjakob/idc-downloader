/**
 * A Token Bucket (https://en.wikipedia.org/wiki/Token_bucket)
 *
 * This thread-safe bucket should support the following methods:
 *
 * - take(n): remove n tokens from the bucket (blocks until n tokens are available and taken)
 * - set(n): set the bucket to contain n tokens (to allow "hard" rate limiting)
 * - add(n): add n tokens to the bucket (to allow "soft" rate limiting)
 * - terminate(): mark the bucket as terminated (used to communicate between threads)
 * - terminated(): return true if the bucket is terminated, false otherwise
 *
 */

import java.util.concurrent.atomic.*;

public class TokenBucket {

	private AtomicLong tokensAvailable;
	private AtomicBoolean termination;
	
    TokenBucket() {
    	tokensAvailable = new AtomicLong();
    	termination = new AtomicBoolean();
    }

    public synchronized void take(long tokens) {
    	while (tokens > tokensAvailable.get());
    	tokensAvailable.addAndGet(-tokens);
    }

    public void terminate() {
    	termination.set(true);
    }

    public boolean terminated() {
        return termination.get();
    }

    public void set(long tokens) {
    	tokensAvailable.set(tokens);
    }
    
    public void add(long tokens) {
    	tokensAvailable.addAndGet(tokens);
    }
    
}
