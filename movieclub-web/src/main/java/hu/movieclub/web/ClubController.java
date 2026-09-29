package hu.movieclub.web;
import hu.movieclub.core.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.security.Principal;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;
import org.springframework.http.HttpStatus;

@RestController @RequestMapping("/api") @Validated
public class ClubController {
    private final ClubStore store;
    private final RecommendationService recommender;
    private final PasswordEncoder encoder;
    private final MetadataProvider metadata;
    private final boolean demo;
    public ClubController(ClubStore s,RecommendationService r,PasswordEncoder e,MetadataProvider m,@Value("${movieclub.demo}") boolean demo) {
        store=s;recommender=r;encoder=e;metadata=m;this.demo=demo;
    }
    public record Registration(@Pattern(regexp="[a-zA-Z0-9_]{3,40}",message="A név 3–40 betű, szám vagy aláhúzás lehet.") @NotNull String username,
        @NotNull @Size(min=8,max=60,message="A jelszó 8–60 karakter legyen.") String password) {}
    public record RatingInput(@Min(1) @Max(5) int score,@NotNull @Size(max=2000) String review) {}
    public record WatchInput(@NotNull WatchItem.Status status) {}
    public record MovieInput(@NotBlank @Size(max=200) String title,@Min(1888) @Max(2200) int year,
        @NotBlank @Size(max=4000) String description,@NotEmpty @Size(max=10) Set<@NotBlank @Size(max=60) String> genres) {}
    private Account user(Principal p) {
        if(p==null) throw new SecurityException("Jelentkezz be.");
        return store.account(p.getName()).orElseThrow();
    }
    private Movie movie(long id) { return store.movie(id).orElseThrow(() -> new NoSuchElementException("A film nem található.")); }
    @GetMapping("/session") Map<String,Object> session(Principal p,CsrfToken csrf) {
        Map<String,Object> result=new HashMap<>();
        result.put("username",p==null ? null : p.getName());
        result.put("role",p==null ? null : user(p).role());
        result.put("csrf",csrf.getToken());result.put("csrfHeader",csrf.getHeaderName());
        result.put("demo",demo); result.put("tmdbConfigured",metadata.configured()); return result;
    }
    @PostMapping("/register") @ResponseStatus(HttpStatus.CREATED)
    Map<String,String> register(@Valid @RequestBody Registration body) {
        if(body.password().getBytes(java.nio.charset.StandardCharsets.UTF_8).length>72)
            throw new IllegalArgumentException("A jelszó legfeljebb 72 UTF-8 bájt lehet; használj rövidebb jelszót.");
        store.createAccount(body.username(),encoder.encode(body.password()),"MEMBER");
        return Map.of("message","Sikeres regisztráció. Most már bejelentkezhetsz.");
    }
    public record FilmView(Movie movie,double average,long votes) {}
    @GetMapping("/movies") List<FilmView> movies(@RequestParam(defaultValue="") @Size(max=200) String q,
            @RequestParam(defaultValue="") @Size(max=60) String genre) {
        var ratings=store.ratings();
        return store.movies().stream().filter(m -> m.title().toLowerCase(Locale.ROOT).contains(q.toLowerCase(Locale.ROOT)))
            .filter(m -> genre.isBlank() || m.genres().contains(genre)).map(m -> {
                var votes=ratings.stream().filter(r -> r.movieId().equals(m.id())).toList();
                return new FilmView(m,votes.stream().mapToInt(Rating::score).average().orElse(0),votes.size());
            }).toList();
    }
    @GetMapping("/movies/{id}") Map<String,Object> details(@PathVariable long id) {
        return Map.of("movie",movie(id),"ratings",store.ratings().stream().filter(r -> r.movieId()==id).toList());
    }
    @PutMapping("/movies/{id}/rating") Rating rate(@PathVariable long id,@Valid @RequestBody RatingInput body,Principal p) {
        return store.saveRating(user(p).id(),id,body.score(),body.review());
    }
    @DeleteMapping("/ratings/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    void removeRating(@PathVariable long id,Principal p) {
        var u=user(p);store.deleteRating(id,u.id(),u.role().equals("MODERATOR"));
    }
    public record WatchView(Movie movie,WatchItem.Status status) {}
    @GetMapping("/watchlist") List<WatchView> watchlist(Principal p) {
        return store.watchlist(user(p).id()).stream().map(w -> new WatchView(movie(w.movieId()),w.status())).toList();
    }
    @PutMapping("/watchlist/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    void watch(@PathVariable long id,@Valid @RequestBody WatchInput body,Principal p) { store.saveWatch(user(p).id(),id,body.status()); }
    @DeleteMapping("/watchlist/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    void unwatch(@PathVariable long id,Principal p) { store.deleteWatch(user(p).id(),id); }
    @GetMapping("/recommendations") List<RecommendationService.Recommendation> recommendations(Principal p) {
        return recommender.recommend(user(p).id());
    }
    @PostMapping("/moderator/movies") @ResponseStatus(HttpStatus.CREATED)
    Movie add(@Valid @RequestBody MovieInput b) { return store.saveMovie(new Movie(null,b.title().strip(),b.year(),b.description(),b.genres(),null)); }
    @PutMapping("/moderator/movies/{id}") Movie edit(@PathVariable long id,@Valid @RequestBody MovieInput b) {
        return store.saveMovie(new Movie(id,b.title().strip(),b.year(),b.description(),b.genres(),movie(id).externalId()));
    }
    @DeleteMapping("/moderator/movies/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    void deleteMovie(@PathVariable long id) { store.deleteMovie(id); }
    @GetMapping("/moderator/import/search") List<Movie> search(@RequestParam @NotBlank @Size(max=200) String q) { return metadata.search(q); }
    @PostMapping("/moderator/import/{id}") Movie importMovie(@PathVariable @Positive long id) {
        return store.byExternalId(id).orElseGet(() -> store.saveMovie(metadata.fetch(id)));
    }
}

