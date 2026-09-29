package hu.movieclub.web;
import hu.movieclub.core.*;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfiguration {
    @Bean PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }
    @Bean UserDetailsService users(ClubStore store) {
        return name -> {
            var u=store.account(name).orElseThrow(() -> new UsernameNotFoundException("Ismeretlen felhasználó."));
            return User.withUsername(u.username()).password(u.passwordHash()).roles(u.role()).build();
        };
    }
    @Bean SecurityFilterChain security(HttpSecurity http) throws Exception {
        return http.authorizeHttpRequests(a -> a
                .requestMatchers("/","/login","/app.js","/style.css","/favicon.svg","/tmdb.svg","/fonts/**","/error","/api/session","/api/register").permitAll()
                .requestMatchers(org.springframework.http.HttpMethod.GET,"/api/movies","/api/movies/*").permitAll()
                .requestMatchers("/api/moderator/**").hasRole("MODERATOR")
                .anyRequest().authenticated())
            .formLogin(f -> f.loginPage("/login").loginProcessingUrl("/api/login")
                .successHandler((req,res,auth) -> res.setStatus(204))
                .failureHandler((req,res,ex) -> {res.setStatus(401);res.setContentType("application/json;charset=UTF-8");res.getWriter().write("{\"message\":\"Hibás felhasználónév vagy jelszó.\"}");}))
            .logout(l -> l.logoutUrl("/api/logout").logoutSuccessHandler((req,res,auth) -> res.setStatus(204)))
            .exceptionHandling(e -> e.authenticationEntryPoint((req,res,ex) -> {
                res.setStatus(401);res.setContentType("application/json;charset=UTF-8");res.getWriter().write("{\"message\":\"Ehhez jelentkezz be.\"}");
            }).accessDeniedHandler((req,res,ex) -> {
                res.setStatus(403);res.setContentType("application/json;charset=UTF-8");res.getWriter().write("{\"message\":\"Nincs jogosultságod, vagy lejárt a munkamenet. Frissítsd az oldalt.\"}");
            })).build();
    }
}
