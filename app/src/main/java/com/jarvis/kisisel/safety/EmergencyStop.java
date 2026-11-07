package com.jarvis.kisisel.safety;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicLong;

/** Process-local synchronous cancellation bus. Every long-running component
 * registers its own cancellation callback. Does not restart anything. */
public final class EmergencyStop {
    private static final AtomicLong generation=new AtomicLong();
    private static final Set<Runnable> listeners=new CopyOnWriteArraySet<>();
    private EmergencyStop(){}
    public static long generation(){return generation.get();}
    public static void register(Runnable listener){
        if(listener!=null)listeners.add(listener);
    }
    public static void unregister(Runnable listener){
        if(listener!=null)listeners.remove(listener);
    }
    public static long stop(){
        long value=generation.incrementAndGet();
        for(Runnable listener:listeners){
            try{listener.run();}catch(RuntimeException ignored){
                // One failed client must not prevent other clients cancelling.
            }
        }
        return value;
    }
    public static boolean isCancelled(long ticket){return generation.get()!=ticket;}
}
