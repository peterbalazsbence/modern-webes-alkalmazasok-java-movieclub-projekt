package hu.movieclub.core;
import java.util.Set;
public record Movie(Long id, String title, int year, String description, Set<String> genres, Long externalId) {
    public Movie { genres = Set.copyOf(genres); }
}
