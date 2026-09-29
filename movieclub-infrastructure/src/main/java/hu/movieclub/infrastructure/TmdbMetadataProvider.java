package hu.movieclub.infrastructure;
import hu.movieclub.core.*;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.*;
import com.fasterxml.jackson.databind.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class TmdbMetadataProvider implements MetadataProvider {
    private final String token;
    private final ObjectMapper mapper;
    private final HttpClient client=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private final String baseUrl;
    public TmdbMetadataProvider(@Value("${movieclub.tmdb-token:}") String token,
            @Value("${movieclub.tmdb-base-url:https://api.themoviedb.org/3}") String baseUrl,ObjectMapper mapper) {
        this.token=token; this.baseUrl=baseUrl; this.mapper=mapper;
    }
    public boolean configured() { return !token.isBlank(); }
    private JsonNode get(URI uri) {
        if(!configured()) throw new IllegalStateException("A filmes importhoz állítsd be a TMDB_TOKEN környezeti változót.");
        try {
            var request=HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(10))
                .header("Authorization","Bearer "+token).header("Accept","application/json").GET().build();
            var response=client.send(request,HttpResponse.BodyHandlers.ofString());
            if(response.statusCode()!=200) throw new IllegalStateException("A filmes szolgáltatás nem érhető el, vagy a hozzáférési kulcs érvénytelen.");
            return mapper.readTree(response.body());
        } catch(InterruptedException e) {
            Thread.currentThread().interrupt(); throw new IllegalStateException("A filmes lekérdezés megszakadt.");
        } catch(java.io.IOException e) { throw new IllegalStateException("A filmes szolgáltatás nem érhető el. Próbáld újra később."); }
    }
    private Movie map(JsonNode n,boolean details) {
        var date=n.path("release_date").asText("");
        int year=date.length()>=4 ? Integer.parseInt(date.substring(0,4)) : 0;
        Set<String> genres=new TreeSet<>();
        if(details) n.path("genres").forEach(g -> genres.add(translateGenre(g.path("id").asInt(),g.path("name").asText())));
        return new Movie(null,n.path("title").asText(),year,n.path("overview").asText(""),genres,n.path("id").asLong());
    }
    private String translateGenre(int id,String fallback) {
        return switch(id) {
            case 28 -> "Akció"; case 12 -> "Kaland"; case 16 -> "Animáció"; case 35 -> "Vígjáték";
            case 80 -> "Bűnügyi"; case 18 -> "Dráma"; case 14 -> "Fantasy"; case 27 -> "Horror";
            case 10749 -> "Romantikus"; case 878 -> "Sci-fi"; case 53 -> "Thriller"; case 9648 -> "Rejtély";
            default -> fallback;
        };
    }
    public List<Movie> search(String q) {
        var uri=UriComponentsBuilder.fromUriString(baseUrl+"/search/movie").queryParam("query",q)
            .queryParam("language","hu-HU").queryParam("include_adult",false).build().encode().toUri();
        List<Movie> result=new ArrayList<>(); get(uri).path("results").forEach(n -> result.add(map(n,false)));
        return result.stream().limit(12).toList();
    }
    public Movie fetch(long id) {
        if(id<=0) throw new IllegalArgumentException("Érvénytelen filmes azonosító.");
        return map(get(URI.create(baseUrl+"/movie/"+id+"?language=hu-HU")),true);
    }
}

