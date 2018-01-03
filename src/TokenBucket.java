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

class TokenBucket {

	AtomicLong tokensAvailable;
	AtomicBoolean termination;
	
    TokenBucket() {
    	tokensAvailable = new AtomicLong();
    	termination = new AtomicBoolean();
    }

    synchronized void take(long tokens) {
    	while (tokens > tokensAvailable.get());
    	tokensAvailable.addAndGet(-tokens);
    }

    void terminate() {
    	termination.set(true);
    }

    boolean terminated() {
        return termination.get();
    }

    void set(long tokens) {
    	tokensAvailable.set(tokens);
    }
    
    void add(long tokens) {
    	tokensAvailable.addAndGet(tokens);
    }
    
}
