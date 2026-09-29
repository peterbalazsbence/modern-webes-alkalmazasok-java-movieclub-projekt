package hu.movieclub.core;
import java.util.List;
public interface MetadataProvider {
    boolean configured();
    List<Movie> search(String query);
    Movie fetch(long externalId);
}

