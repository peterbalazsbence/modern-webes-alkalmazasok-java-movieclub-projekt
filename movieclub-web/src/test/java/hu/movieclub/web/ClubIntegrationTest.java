package hu.movieclub.web;
import hu.movieclub.core.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.*;

@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:clubtest;DB_CLOSE_DELAY=-1","movieclub.demo=true","movieclub.tmdb-token="})
@AutoConfigureMockMvc @Transactional
class ClubIntegrationTest {
    @Autowired MockMvc mvc; @Autowired ClubStore store; @Autowired JdbcTemplate jdbc; @Autowired PasswordEncoder encoder;
    long film;
    @BeforeEach void film() { film=store.movies().getFirst().id(); }
    @Test void publicCatalogueAndTemplateWork() throws Exception {
        mvc.perform(get("/")).andExpect(status().isOk()).andExpect(content().string(containsString("MovieClub")));
        mvc.perform(get("/api/movies")).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(12));
        mvc.perform(get("/api/session")).andExpect(status().isOk()).andExpect(jsonPath("$.csrf").isNotEmpty()).andExpect(jsonPath("$.passwordHash").doesNotExist());
    }
    @Test void filtersByTitleAndGenre() throws Exception {
        mvc.perform(get("/api/movies").param("q","interstellar").param("genre","Sci-fi"))
            .andExpect(status().isOk()).andExpect(jsonPath("$[0].movie.title").value("Interstellar")).andExpect(jsonPath("$.length()").value(1));
    }
    @Test void unauthenticatedWritesAreRejected() throws Exception {
        mvc.perform(put("/api/movies/"+film+"/rating").with(csrf()).contentType("application/json").content("{\"score\":5,\"review\":\"\"}")).andExpect(status().isUnauthorized());
    }
    @Test void csrfIsRequired() throws Exception {
        mvc.perform(put("/api/movies/"+film+"/rating").with(user("tag")).contentType("application/json").content("{\"score\":5,\"review\":\"\"}")).andExpect(status().isForbidden());
    }
    @Test void repeatedRatingUpdatesOneRow() throws Exception {
        var user=store.account("tag").orElseThrow();
        mvc.perform(put("/api/movies/"+film+"/rating").with(user("tag")).with(csrf()).contentType("application/json").content("{\"score\":4,\"review\":\"Első\"}")).andExpect(status().isOk());
        mvc.perform(put("/api/movies/"+film+"/rating").with(user("tag")).with(csrf()).contentType("application/json").content("{\"score\":2,\"review\":\"Frissített\"}")).andExpect(status().isOk());
        assertThat(store.ratings().stream().filter(r->r.userId().equals(user.id())&&r.movieId()==film).toList())
            .singleElement().satisfies(r->{assertThat(r.score()).isEqualTo(2);assertThat(r.review()).isEqualTo("Frissített");});
    }
    @Test void databaseRejectsDuplicateRatingEvenOutsideApplication() {
        var r=store.ratings().getFirst();
        assertThatThrownBy(()->jdbc.update("INSERT INTO ratings(user_id,movie_id,score,review) VALUES (?,?,?,?)",r.userId(),r.movieId(),5,"duplicate")).isInstanceOf(DataIntegrityViolationException.class);
    }
    @Test void invalidScoreAndUnknownFilmAreRejected() throws Exception {
        mvc.perform(put("/api/movies/"+film+"/rating").with(user("tag")).with(csrf()).contentType("application/json").content("{\"score\":6,\"review\":\"\"}")).andExpect(status().isBadRequest());
        mvc.perform(put("/api/movies/99999/rating").with(user("tag")).with(csrf()).contentType("application/json").content("{\"score\":5,\"review\":\"\"}")).andExpect(status().isNotFound());
    }
    @Test void cannotDeleteAnotherMembersReview() throws Exception {
        var rating=store.ratings().stream().filter(r->r.username().equals("anna")).findFirst().orElseThrow();
        mvc.perform(delete("/api/ratings/"+rating.id()).with(user("tag")).with(csrf())).andExpect(status().isForbidden());
        assertThat(store.ratings()).extracting(Rating::id).contains(rating.id());
    }
    @Test void moderatorCanDeleteReview() throws Exception {
        var rating=store.ratings().getFirst();
        mvc.perform(delete("/api/ratings/"+rating.id()).with(user("moderator").roles("MODERATOR")).with(csrf())).andExpect(status().isNoContent());
        assertThat(store.ratings()).extracting(Rating::id).doesNotContain(rating.id());
    }
    @Test void memberCannotManageCatalogueOrImport() throws Exception {
        mvc.perform(delete("/api/moderator/movies/"+film).with(user("tag")).with(csrf())).andExpect(status().isForbidden());
        mvc.perform(get("/api/moderator/import/search").param("q","Dune").with(user("tag"))).andExpect(status().isForbidden());
    }
    @Test void watchlistIsPrivateAndUpserts() throws Exception {
        var tag=store.account("tag").orElseThrow();var anna=store.account("anna").orElseThrow();
        mvc.perform(put("/api/watchlist/"+film).with(user("tag")).with(csrf()).contentType("application/json").content("{\"status\":\"PLANNED\"}")).andExpect(status().isNoContent());
        mvc.perform(put("/api/watchlist/"+film).with(user("tag")).with(csrf()).contentType("application/json").content("{\"status\":\"WATCHED\"}")).andExpect(status().isNoContent());
        assertThat(store.watchlist(tag.id()).stream().filter(w->w.movieId()==film).toList()).singleElement().satisfies(w->assertThat(w.status()).isEqualTo(WatchItem.Status.WATCHED));
        assertThat(store.watchlist(anna.id())).isEmpty();
        mvc.perform(delete("/api/watchlist/"+film).with(user("tag")).with(csrf())).andExpect(status().isNoContent());
        assertThat(store.watchlist(tag.id())).noneMatch(w->w.movieId()==film);
    }
    @Test void registrationHashesPasswordAndCannotEscalateRole() throws Exception {
        mvc.perform(post("/api/register").with(csrf()).contentType("application/json").content("{\"username\":\"newmember\",\"password\":\"Password123!\"}")).andExpect(status().isCreated());
        var account=store.account("newmember").orElseThrow();
        assertThat(account.role()).isEqualTo("MEMBER");
        assertThat(encoder.matches("Password123!",account.passwordHash())).isTrue();
        mvc.perform(post("/api/register").with(csrf()).contentType("application/json").content("{\"username\":\"hacker\",\"password\":\"Password123!\",\"role\":\"MODERATOR\"}")).andExpect(status().isBadRequest());
        mvc.perform(post("/api/register").with(csrf()).contentType("application/json").content("{\"username\":\"newmember\",\"password\":\"Password123!\"}")).andExpect(status().isConflict());
    }
    @Test void validAndInvalidLogin() throws Exception {
        mvc.perform(post("/api/login").with(csrf()).param("username","tag").param("password","MovieClub123!")).andExpect(status().isNoContent());
        mvc.perform(post("/api/login").with(csrf()).param("username","tag").param("password","wrong")).andExpect(status().isUnauthorized());
    }
    @Test void deletionCascadesAndMissingFilmReturns404() throws Exception {
        mvc.perform(delete("/api/moderator/movies/"+film).with(user("moderator").roles("MODERATOR")).with(csrf())).andExpect(status().isNoContent());
        mvc.perform(get("/api/movies/"+film)).andExpect(status().isNotFound());
        assertThat(store.ratings()).noneMatch(r->r.movieId()==film);
    }
    @Test void recommendationEndpointExcludesRatedAndWatched() throws Exception {
        var account=store.account("tag").orElseThrow();
        store.saveWatch(account.id(),film,WatchItem.Status.WATCHED);
        mvc.perform(get("/api/recommendations").with(user("tag"))).andExpect(status().isOk())
            .andExpect(jsonPath("$[*].movie.id",not(hasItem((int)film))));
    }
    @Test void missingExternalTokenProducesHelpfulError() throws Exception {
        mvc.perform(get("/api/moderator/import/search").param("q","Dune").with(user("moderator").roles("MODERATOR")))
            .andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.message",containsString("TMDB_TOKEN")));
    }
    @Test void moderatorCanCreateAndEditValidatedMovie() throws Exception {
        String body="{\"title\":\"Tesztfilm\",\"year\":2024,\"description\":\"Teszt leírás\",\"genres\":[\"Dráma\"]}";
        var response=mvc.perform(post("/api/moderator/movies").with(user("moderator").roles("MODERATOR")).with(csrf())
            .contentType("application/json").content(body)).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long id=new com.fasterxml.jackson.databind.ObjectMapper().readTree(response).get("id").asLong();
        mvc.perform(put("/api/moderator/movies/"+id).with(user("moderator").roles("MODERATOR")).with(csrf())
            .contentType("application/json").content(body.replace("Tesztfilm","Új cím")))
            .andExpect(status().isOk()).andExpect(jsonPath("$.title").value("Új cím"));
        assertThat(store.movie(id).orElseThrow().title()).isEqualTo("Új cím");
        mvc.perform(post("/api/moderator/movies").with(user("moderator").roles("MODERATOR")).with(csrf())
            .contentType("application/json").content(body.replace("2024","1000"))).andExpect(status().isBadRequest());
    }
    @Test void invalidWatchStatusAndOversizedReviewAreRejected() throws Exception {
        mvc.perform(put("/api/watchlist/"+film).with(user("tag")).with(csrf()).contentType("application/json").content("{\"status\":\"INVALID\"}")).andExpect(status().isBadRequest());
        mvc.perform(put("/api/movies/"+film+"/rating").with(user("tag")).with(csrf()).contentType("application/json").content("{\"score\":5,\"review\":\""+ "x".repeat(2001)+"\"}")).andExpect(status().isBadRequest());
    }
}
