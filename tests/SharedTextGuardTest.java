import com.jarvis.kisisel.SharedTextGuard;

public class SharedTextGuardTest {
    public static void main(String[] args) {
        if (!SharedTextGuard.onlyLink("https://x.com/example/status/123"))
            throw new AssertionError("Link-only must not be treated as read content");
        if (!SharedTextGuard.onlyLink("Şuna bak: https://x.com/example/status/123"))
            throw new AssertionError("Link label does not include article text");
        if (SharedTextGuard.onlyLink("Paylaşılan haberin ayrıntılı açıklama metni burada mevcut ve incelenebilir. https://x.com/example/status/123"))
            throw new AssertionError("Substantial shared text can be analyzed");
        if (SharedTextGuard.onlyLink("Bu bir açıklamadır."))
            throw new AssertionError("Plain text is not URL-only");
        if (SharedTextGuard.safeText("x".repeat(7000)).length()!=6000)
            throw new AssertionError("Size limit");
        System.out.println("SharedTextGuard passed");
    }
}
