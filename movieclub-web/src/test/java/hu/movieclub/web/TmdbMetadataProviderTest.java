package hu.movieclub.web;
import hu.movieclub.infrastructure.TmdbMetadataProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import static org.assertj.core.api.Assertions.*;

class TmdbMetadataProviderTest {
    HttpServer server; String base;
    @BeforeEach void start() throws Exception {
        server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        base="http://127.0.0.1:"+server.getAddress().getPort();
        server.start();
    }
    @AfterEach void stop() {server.stop(0);}
    @Test void fetchMapsMetadataAndAuthenticates() {
        AtomicReference<String> auth=new AtomicReference<>();
        server.createContext("/movie/42",exchange->{
            auth.set(exchange.getRequestHeaders().getFirst("Authorization"));
            byte[] data="{\"id\":42,\"title\":\"Tesztfilm\",\"release_date\":\"2024-02-01\",\"overview\":\"Leírás\",\"genres\":[{\"id\":878,\"name\":\"Science Fiction\"}]}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200,data.length);try(var out=exchange.getResponseBody()){out.write(data);}
        });
        var movie=new TmdbMetadataProvider("test-token",base,new ObjectMapper()).fetch(42);
        assertThat(movie.externalId()).isEqualTo(42);assertThat(movie.id()).isNull();
        assertThat(movie.genres()).containsExactly("Sci-fi");assertThat(movie.year()).isEqualTo(2024);
        assertThat(auth.get()).isEqualTo("Bearer test-token");
    }
    @Test void remoteFailureDoesNotExposeCredentials() {
        server.createContext("/search/movie",exchange->{exchange.sendResponseHeaders(401,-1);exchange.close();});
        assertThatThrownBy(()->new TmdbMetadataProvider("secret-token",base,new ObjectMapper()).search("film"))
            .isInstanceOf(IllegalStateException.class).hasMessageNotContaining("secret-token");
    }
    @Test void missingReleaseDateIsHandledAndQueryIsEncoded() {
        server.createContext("/search/movie",exchange->{
            byte[] data="{\"results\":[{\"id\":42,\"title\":\"Film\",\"release_date\":\"\"}]}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200,data.length);try(var out=exchange.getResponseBody()){out.write(data);}
        });
        assertThat(new TmdbMetadataProvider("test",base,new ObjectMapper()).search("Árvíz & film")).singleElement().satisfies(m->assertThat(m.year()).isZero());
    }
}

