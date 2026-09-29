package hu.movieclub.core;
import java.util.*;
public interface ClubStore {
    List<Movie> movies();
    Optional<Movie> movie(long id);
    Movie saveMovie(Movie movie);
    Optional<Movie> byExternalId(long id);
    void deleteMovie(long id);
    Optional<Account> account(String username);
    Account createAccount(String username, String hash, String role);
    List<Rating> ratings();
    Rating saveRating(long userId, long movieId, int score, String review);
    void deleteRating(long ratingId, long actorId, boolean moderator);
    List<WatchItem> watchlist(long userId);
    void saveWatch(long userId, long movieId, WatchItem.Status status);
    void deleteWatch(long userId, long movieId);
}

