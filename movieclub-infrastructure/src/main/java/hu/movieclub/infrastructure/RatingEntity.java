package hu.movieclub.infrastructure;
import jakarta.persistence.*;
@Entity @Table(name="ratings",uniqueConstraints=@UniqueConstraint(columnNames={"user_id","movie_id"}))
public class RatingEntity {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) Long id;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="user_id") AccountEntity user;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="movie_id") MovieEntity movie;
    @Column(nullable=false) int score;
    @Column(nullable=false,length=2000) String review;
    protected RatingEntity() {}
}

