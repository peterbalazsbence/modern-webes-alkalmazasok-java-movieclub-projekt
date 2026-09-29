package hu.movieclub.core;
import java.util.*;
import java.util.stream.Collectors;

/** Deterministic content-based recommendation; no external service can add candidates. */
public final class RecommendationService {
    private final ClubStore store;
    public RecommendationService(ClubStore store) { this.store = store; }
    public record Recommendation(Movie movie, double score, String reason) {}
    public List<Recommendation> recommend(long userId) {
        var ratings = store.ratings();
        Set<Long> excluded = ratings.stream().filter(r -> r.userId() == userId)
                .map(Rating::movieId).collect(Collectors.toSet());
        store.watchlist(userId).stream().filter(w -> w.status() == WatchItem.Status.WATCHED)
                .map(WatchItem::movieId).forEach(excluded::add);
        var movies = store.movies();
        Map<Long,Movie> catalogue = movies.stream().collect(Collectors.toMap(Movie::id, m -> m));
        Map<String,Integer> preferences = new HashMap<>();
        ratings.stream().filter(r -> r.userId() == userId && r.score() >= 4).forEach(r -> {
            var movie = catalogue.get(r.movieId());
            if (movie != null) movie.genres().forEach(g -> preferences.merge(g, r.score()-3, Integer::sum));
        });
        return movies.stream().filter(m -> !excluded.contains(m.id())).map(m -> {
            var votes = ratings.stream().filter(r -> r.movieId().equals(m.id())).toList();
            // Bayesian smoothing prevents a single five-star vote dominating the list.
            double popularity = (votes.stream().mapToInt(Rating::score).sum() + 3.0 * 5) / (votes.size()+5);
            int affinity = m.genres().stream().mapToInt(g -> preferences.getOrDefault(g,0)).sum();
            String matched = m.genres().stream().filter(preferences::containsKey).sorted().collect(Collectors.joining(", "));
            String reason = matched.isEmpty()
                ? "Közösségi értékelések alapján. Értékelj több filmet a személyesebb ajánlásokhoz!"
                : "Kedvelt műfajaid alapján: " + matched + ".";
            return new Recommendation(m, affinity * 2 + popularity, reason);
        }).sorted(Comparator.comparingDouble(Recommendation::score).reversed()
                .thenComparing(r -> r.movie().title()).thenComparing(r -> r.movie().id()))
                .limit(12).toList();
    }
}

