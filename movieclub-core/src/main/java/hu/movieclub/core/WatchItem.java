package hu.movieclub.core;
public record WatchItem(Long movieId, Status status) {
    public enum Status { PLANNED, WATCHED }
}

