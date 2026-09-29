package hu.movieclub.infrastructure;
import jakarta.persistence.*;
import java.util.*;
@Entity @Table(name="movies")
public class MovieEntity {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) Long id;
    @Column(nullable=false,length=200) String title;
    @Column(name="release_year",nullable=false) int year;
    @Column(nullable=false,length=4000) String description;
    @Column(name="external_id",unique=true) Long externalId;
    @ElementCollection(fetch=FetchType.EAGER)
    @CollectionTable(name="movie_genres",joinColumns=@JoinColumn(name="movie_id"))
    @Column(name="genre",nullable=false,length=60)
    Set<String> genres = new HashSet<>();
    protected MovieEntity() {}
    public Long getId() { return id; }
    public String getTitle() { return title; }
    public int getYear() { return year; }
    public String getDescription() { return description; }
    public Set<String> getGenres() { return genres; }
    public Long getExternalId() { return externalId; }
}
