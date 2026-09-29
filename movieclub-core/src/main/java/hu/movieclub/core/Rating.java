package hu.movieclub.core;
public record Rating(Long id, Long userId, Long movieId, int score, String review, String username) {}

