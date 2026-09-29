package hu.movieclub;
import hu.movieclub.core.*;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
@SpringBootApplication
public class MovieClubApplication {
    public static void main(String[] args) { SpringApplication.run(MovieClubApplication.class,args); }
    @Bean RecommendationService recommendationService(ClubStore store) { return new RecommendationService(store); }
}

