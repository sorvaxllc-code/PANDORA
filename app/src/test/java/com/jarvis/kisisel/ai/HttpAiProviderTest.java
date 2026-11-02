package com.jarvis.kisisel.ai;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import org.json.*;
import org.junit.*;
import static org.junit.Assert.*;
public class HttpAiProviderTest {
    private HttpServer server;private String reply,request,authorization;private int status,posts;
    @Before public void start()throws Exception{
        status=200;posts=0;reply="{\"choices\":[{\"message\":{\"content\":\"Merhaba\"},\"finish_reason\":\"stop\"}]}";
        server=HttpServer.create(new InetSocketAddress("127.0.0.1",8642),0);
        server.createContext("/v1/models",e->{byte[] b="{\"data\":[{\"id\":\"hermes-agent\"}]}".getBytes(StandardCharsets.UTF_8);e.sendResponseHeaders(200,b.length);e.getResponseBody().write(b);e.close();});
        server.createContext("/v1/chat/completions",e->{posts++;request=new String(e.getRequestBody().readAllBytes(),StandardCharsets.UTF_8);authorization=e.getRequestHeaders().getFirst("Authorization");
            byte[] b=reply.getBytes(StandardCharsets.UTF_8);e.getResponseHeaders().set("Location","http://127.0.0.1:8642/redirect-target");e.sendResponseHeaders(status,b.length);e.getResponseBody().write(b);e.close();});server.start();
    }
    @After public void close(){if(server!=null)server.stop(0);}
    private HttpAiProvider provider(){return new HttpAiProvider("Hermes","http://127.0.0.1:8642/v1/chat/completions","hermes-agent","unit-test-key",2000);}
    private JSONArray message()throws Exception{return new JSONArray().put(new JSONObject().put("role","user").put("content","Merhaba"));}
    @Test public void authenticatedTurkishReply()throws Exception{
        assertEquals("Merhaba",provider().answer(message()));assertEquals("Bearer unit-test-key",authorization);assertEquals("hermes-agent",new JSONObject(request).getString("model"));assertEquals(1,posts);
    }
    @Test public void imageContentIsPreserved()throws Exception{
        JSONArray parts=new JSONArray().put(new JSONObject().put("type","text").put("text","Bu ne demek?"))
            .put(new JSONObject().put("type","image_url").put("image_url",new JSONObject().put("url","data:image/jpeg;base64,dGVzdA==")));
        provider().answer(new JSONArray().put(new JSONObject().put("role","user").put("content",parts)));
        assertEquals("data:image/jpeg;base64,dGVzdA==",new JSONObject(request).getJSONArray("messages").getJSONObject(0).getJSONArray("content").getJSONObject(1).getJSONObject("image_url").getString("url"));
    }
    @Test public void nullIsNeverShownAsAnAnswer()throws Exception{reply="{\"choices\":[{\"message\":{\"content\":null}}]}";assertThrows(Exception.class,()->provider().answer(message()));}
    @Test public void serverFailureIsNotSuccess()throws Exception{reply="{\"choices\":[{\"message\":{\"content\":\"diagnostic\"},\"finish_reason\":\"error\"}]}";assertThrows(Exception.class,()->provider().answer(message()));}
    @Test public void redirectsAreNotFollowed()throws Exception{status=302;assertThrows(Exception.class,()->provider().answer(message()));assertEquals(1,posts);}
    @Test public void cancelledTurnIsNeverSent()throws Exception{HttpAiProvider.CancelToken token=new HttpAiProvider.CancelToken();token.cancel();assertThrows(Exception.class,()->provider().answer(message(),token));assertEquals(0,posts);}
    @Test public void clippedResultIsLabelled()throws Exception{reply="{\"choices\":[{\"message\":{\"content\":\"Yanıt\"},\"finish_reason\":\"length\"}]}";assertTrue(provider().answer(message()).contains("kesildi"));}
}
