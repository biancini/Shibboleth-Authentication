package it.garr.shibboleth.idp;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Thread safe object map, which clears objects after a specified time. The objects are stored in the
 * underlying hashMap.  
 * @author rongkan
 *
 */
public final class TimeLimitedCacheMap {
        
    private final ConcurrentHashMap<String, HashMap<String, String>> objectMap = new ConcurrentHashMap<String, HashMap<String, String>>(10);
    private final ConcurrentHashMap<String, Long> timeMap = new ConcurrentHashMap<String, Long>();
    /* I need a shared lock, readwrite lock is an excellent candidate.
     * evcition is run with writeLock, put/remove with readLock 
     */
    private final ReentrantReadWriteLock accessLock = new ReentrantReadWriteLock();
    //private final ReentrantLock accessLock = new ReentrantLock();
    private final Runnable evictor = new Runnable() {
        /* evictor thread removes data, and changes map state. This is
         * in conflict with put() and remove(). So we need sync for these operations  
         * 
         * In case you are wondering why evictor needs sync (it just calls remove() right?)
         * eviction is a compound action that spans more than a single remove(). It enters
         * into a conflict with put()
         * 
         * evictor: start looping ------------------------------> keyset is stale, evictor removes the recent put & armagedon comes 
         * Thrd1:--------------------->put(same key, new object)
         * 
         */
        @Override
        public void run() {
            // avoid runs on empty maps
            if(timeMap.isEmpty()) Thread.yield();
            
            long currentTime = System.nanoTime();
            accessLock.writeLock().lock();
            Set<String> keys = new HashSet<String>(timeMap.keySet());
            accessLock.writeLock().unlock();
            /* First attempt to detect & mark stale entries, but don't delete
             * The hash map may contain 1000s of objects dont' block it. The returned
             * Set returned may be stale, implying:
             * 1. contains keys for objects which are removed by user, using remove() (not a problem)
             * 2. contains keys for objects which are updated by user, using put() [a big problem]
             */
            Set<String> markedForRemoval = new HashSet<String>(10);
            for (String key : keys) {
                long lastTime = timeMap.get(key);
                if (lastTime == 0) continue;
                
                long interval = currentTime - lastTime;
                long elapsedTime = TimeUnit.NANOSECONDS.convert(interval, expiryTimeUnit);
                if (elapsedTime > expiryTime) markedForRemoval.add(key);
            }
            
            /* Actual removal call, which runs on the objects marked earlier.
             * Assumption: marked objects.size() < hashmap.size()
             * Do not delete blindly, check for staleness before calling remove
             */
            accessLock.writeLock().lock();
            for (String key : markedForRemoval) {
                long lastTime = timeMap.get(key);
                if(lastTime == 0){
                    continue;
                }
                long interval = currentTime - lastTime;
                long elapsedTime = TimeUnit.NANOSECONDS.convert(interval, expiryTimeUnit);
                if(elapsedTime > expiryTime){
                    remove(key);
                }
            }
            accessLock.writeLock().unlock();
        }
    };
    
    private final ScheduledExecutorService timer = Executors.newSingleThreadScheduledExecutor(new MyThreadFactory(true));
    private final class MyThreadFactory implements ThreadFactory {
        private boolean isDaemon = false;
        
        public MyThreadFactory(boolean daemon) {
            isDaemon = daemon;
        }

        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r);
            t.setDaemon(isDaemon);
            return t;
        }        
        
    };
    private final long expiryTime;
    private final TimeUnit expiryTimeUnit;
    
    /**
     * Users can play around with evictionDelay and expiryTime.
     * 1. Large evictionDelay => less frequent checks, hence chances of finding expired Objects are more
     * 2. Lean evictionDelay => aggressive checks, and hence more sync overhead with put() and remove()
     * 3. Large expiryTime => increases the time object stays in object map and less chance of cache miss (cache miss is bad)
     * 4. Lean expiryTime => by itself does not force object to be removed aggressively, needs lean eviction to be configured 
     * 
     * In case you are wondering, above picture is not complete. 
     * Another key aspect is "Arrival Periodicty", or the rate at which put() is called.
     * 
     * Ideally: expiryTime == arrival periodicity + 1 [read '+1' as slightly greater]
     *             evictionDelay == expiryTime + 1
     * 
     * For random arrival times, which is a more common scenario, use the following pointers 
     * 1. eviction Delay > expiry Time
     *         Here user needs to think of the impact of stale hit (define how stale is stale!)
     * 2. eviction Delay < arrival time
     *         This has higher chances of cache miss and accidental treatment as failure     * 
     * 3. eviction Delay < expiry Time
     *         Unwanted eviction run(s) resulting in sync overhead on map
     * 4. eviction Delay > arrival Time
     *         Unwanted eviction run(s) resulting in sync overhead on map
     *  
     * @param initialDelay, time after which scheduler starts
     * @param evictionDelay, periodicity with which eviction is carried out  
     * @param expiryTime, age of the object, exceeding which the object is  to be removed
     * @param unit
     */
    public TimeLimitedCacheMap(long initialDelay, long evictionDelay, long expiryTime, TimeUnit unit) {
        timer.scheduleWithFixedDelay(evictor, initialDelay, evictionDelay, unit);
        this.expiryTime = expiryTime;
        this.expiryTimeUnit = unit;
    }

    public void put(String key, HashMap<String, String> value) {        
        accessLock.readLock().lock();
        Long nanoTime = System.nanoTime();
        timeMap.put(key, nanoTime);
        objectMap.put(key, value);
        accessLock.readLock().unlock();        
    }
    
    public boolean containsKey(String key) {        
        accessLock.readLock().lock();
        //accessLock.lock();
        boolean containsKey = objectMap.containsKey(key);
        //accessLock.unlock();
        accessLock.readLock().unlock();
        return containsKey;
    }
    
    public HashMap<String, String> get(String key) {        
        accessLock.readLock().lock();
        //accessLock.lock();
        HashMap<String, String> value = objectMap.get(key);
        //accessLock.unlock();
        accessLock.readLock().unlock();
        return value;
    }

    public HashMap<String, String> remove(String key) {        
        accessLock.readLock().lock();
        //accessLock.lock();
        HashMap<String, String> value = objectMap.remove(key);
        timeMap.remove(key);
        //accessLock.unlock();
        accessLock.readLock().unlock();
        return value;
    }
    
    public Map<String, Object> getClonedMap(){
        accessLock.writeLock().lock();
        HashMap<String, Object> mapClone = new HashMap<String, Object>(objectMap);
        accessLock.writeLock().unlock();
        return Collections.unmodifiableMap(mapClone);
    }

}