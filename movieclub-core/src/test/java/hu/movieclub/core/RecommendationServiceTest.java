package hu.movieclub.core;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecommendationServiceTest {
    @Mock ClubStore store;
    RecommendationService service;
    Movie sciFi=new Movie(1L,"Sci-fi kedvenc",2020,"",Set.of("Sci-fi"),null);
    Movie next=new Movie(2L,"Következő sci-fi",2021,"",Set.of("Sci-fi"),null);
    Movie drama=new Movie(3L,"Dráma",2022,"",Set.of("Dráma"),null);
    @BeforeEach void setup() {service=new RecommendationService(store);}
    @Test void favorsGenresFromHighRatings() {
        when(store.movies()).thenReturn(List.of(sciFi,next,drama));
        when(store.ratings()).thenReturn(List.of(new Rating(1L,7L,1L,5,"","tag")));
        when(store.watchlist(7)).thenReturn(List.of());
        var result=service.recommend(7);
        assertThat(result).extracting(r->r.movie().id()).containsExactly(2L,3L);
        assertThat(result.getFirst().reason()).contains("Sci-fi");
    }
    @Test void excludesRatedAndWatchedButKeepsPlanned() {
        when(store.movies()).thenReturn(List.of(sciFi,next,drama));
        when(store.ratings()).thenReturn(List.of(new Rating(1L,7L,1L,2,"","tag")));
        when(store.watchlist(7)).thenReturn(List.of(new WatchItem(2L,WatchItem.Status.WATCHED),new WatchItem(3L,WatchItem.Status.PLANNED)));
        assertThat(service.recommend(7)).extracting(r->r.movie().id()).containsExactly(3L);
    }
    @Test void recommendsOnlyKnownFilmsEvenWithStaleRating() {
        when(store.movies()).thenReturn(List.of(next));
        when(store.ratings()).thenReturn(List.of(new Rating(1L,7L,999L,5,"","tag")));
        when(store.watchlist(7)).thenReturn(List.of());
        assertThat(service.recommend(7)).extracting(r->r.movie().id()).containsExactly(2L);
    }
    @Test void coldStartUsesSmoothedCommunityRating() {
        when(store.movies()).thenReturn(List.of(sciFi,drama));
        when(store.ratings()).thenReturn(List.of(new Rating(1L,8L,3L,5,"","other")));
        when(store.watchlist(7)).thenReturn(List.of());
        var result=service.recommend(7);
        assertThat(result.getFirst().movie().id()).isEqualTo(3L);
        assertThat(result.getFirst().reason()).contains("Közösségi");
    }
    @Test void ignoresOtherUsersPreferencesAndWatchlist() {
        when(store.movies()).thenReturn(List.of(sciFi,drama));
        when(store.ratings()).thenReturn(List.of(new Rating(1L,8L,1L,5,"","other")));
        when(store.watchlist(7)).thenReturn(List.of());
        assertThat(service.recommend(7)).hasSize(2).allSatisfy(r->assertThat(r.reason()).contains("Közösségi"));
        verify(store).watchlist(7L);
    }
    @Test void emptyCatalogueProducesEmptyRecommendations() {
        when(store.movies()).thenReturn(List.of());when(store.ratings()).thenReturn(List.of());when(store.watchlist(7)).thenReturn(List.of());
        assertThat(service.recommend(7)).isEmpty();
    }
}

