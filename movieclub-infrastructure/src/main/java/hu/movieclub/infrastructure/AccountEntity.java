package hu.movieclub.infrastructure;
import jakarta.persistence.*;
@Entity @Table(name="accounts")
public class AccountEntity {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) Long id;
    @Column(nullable=false,unique=true,length=40) String username;
    @Column(name="password_hash",nullable=false,length=100) String passwordHash;
    @Column(nullable=false,length=20) String role;
    protected AccountEntity() {}
    public Long getId() { return id; }
    public String getUsername() { return username; }
    public String getPasswordHash() { return passwordHash; }
    public String getRole() { return role; }
}
