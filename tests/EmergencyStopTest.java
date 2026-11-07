import com.jarvis.kisisel.safety.EmergencyStop;
import java.util.concurrent.atomic.AtomicInteger;
public final class EmergencyStopTest {
    static void ok(boolean x){if(!x)throw new AssertionError("EmergencyStopTest");}
    public static void main(String[] args) {
        AtomicInteger counter=new AtomicInteger();
        Runnable one=counter::incrementAndGet;
        Runnable broken=()->{throw new IllegalStateException("test listener exception");};
        Runnable two=()->counter.addAndGet(10);
        EmergencyStop.register(one); EmergencyStop.register(broken); EmergencyStop.register(two);
        long ticket=EmergencyStop.generation();
        EmergencyStop.stop();
        ok(EmergencyStop.isCancelled(ticket));
        ok(counter.get()==11);
        EmergencyStop.unregister(one);EmergencyStop.unregister(broken);EmergencyStop.unregister(two);
        EmergencyStop.stop();ok(counter.get()==11);
        System.out.println("EmergencyStopTest PASS");
    }
}
