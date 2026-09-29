package hu.movieclub.infrastructure;
import jakarta.persistence.*;
import hu.movieclub.core.WatchItem;
@Entity @Table(name="watch_items",uniqueConstraints=@UniqueConstraint(columnNames={"user_id","movie_id"}))
public class WatchEntity {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) Long id;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="user_id") AccountEntity user;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="movie_id") MovieEntity movie;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) WatchItem.Status status;
    protected WatchEntity() {}
}

