package hu.movieclub.infrastructure;
import hu.movieclub.core.*;
import java.util.*;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository @Transactional(readOnly=true)
public class JpaClubStore implements ClubStore {
    private final MovieRepository movies;
    private final AccountRepository accounts;
    private final RatingRepository ratings;
    private final WatchRepository watches;
    public JpaClubStore(MovieRepository m,AccountRepository a,RatingRepository r,WatchRepository w) {
        movies=m; accounts=a; ratings=r; watches=w;
    }
    private Movie map(MovieEntity e) { return new Movie(e.getId(),e.getTitle(),e.getYear(),e.getDescription(),e.getGenres(),e.getExternalId()); }
    private Account map(AccountEntity e) { return new Account(e.getId(),e.getUsername(),e.getPasswordHash(),e.getRole()); }
    private Rating map(RatingEntity e) { return new Rating(e.id,e.user.getId(),e.movie.getId(),e.score,e.review,e.user.getUsername()); }
    private MovieEntity requireMovie(long id) { return movies.findById(id).orElseThrow(() -> new NoSuchElementException("A film nem található.")); }
    public List<Movie> movies() { return movies.findAll().stream().map(this::map).sorted(Comparator.comparing(Movie::title)).toList(); }
    public Optional<Movie> movie(long id) { return movies.findById(id).map(this::map); }
    public Optional<Movie> byExternalId(long id) { return movies.findByExternalId(id).map(this::map); }
    @Transactional public Movie saveMovie(Movie m) {
        var e=m.id()==null ? new MovieEntity() : requireMovie(m.id());
        e.title=m.title(); e.year=m.year(); e.description=m.description(); e.genres=new HashSet<>(m.genres()); e.externalId=m.externalId();
        return map(movies.saveAndFlush(e));
    }
    @Transactional public void deleteMovie(long id) { movies.delete(requireMovie(id)); movies.flush(); }
    public Optional<Account> account(String name) { return accounts.findByUsername(name).map(this::map); }
    @Transactional public Account createAccount(String name,String hash,String role) {
        var e=new AccountEntity(); e.username=name; e.passwordHash=hash; e.role=role; return map(accounts.saveAndFlush(e));
    }
    public List<Rating> ratings() { return ratings.findAll().stream().map(this::map).toList(); }
    @Transactional public Rating saveRating(long userId,long movieId,int score,String review) {
        if(score<1 || score>5) throw new IllegalArgumentException("A pontszám 1 és 5 közötti lehet.");
        var movie=requireMovie(movieId);
        var e=ratings.findByUserIdAndMovieId(userId,movieId).orElseGet(RatingEntity::new);
        e.user=accounts.getReferenceById(userId); e.movie=movie; e.score=score; e.review=review;
        return map(ratings.saveAndFlush(e));
    }
    @Transactional public void deleteRating(long id,long actor,boolean moderator) {
        var e=ratings.findById(id).orElseThrow(() -> new NoSuchElementException("Az értékelés nem található."));
        if(!moderator && e.user.getId()!=actor) throw new SecurityException("Csak a saját értékelésed törölheted.");
        ratings.delete(e);
    }
    public List<WatchItem> watchlist(long id) { return watches.findByUserId(id).stream().map(e -> new WatchItem(e.movie.getId(),e.status)).toList(); }
    @Transactional public void saveWatch(long user,long movie,WatchItem.Status status) {
        var film=requireMovie(movie);
        var e=watches.findByUserIdAndMovieId(user,movie).orElseGet(WatchEntity::new);
        e.user=accounts.getReferenceById(user); e.movie=film; e.status=status; watches.saveAndFlush(e);
    }
    @Transactional public void deleteWatch(long user,long movie) { watches.findByUserIdAndMovieId(user,movie).ifPresent(watches::delete); }
}
