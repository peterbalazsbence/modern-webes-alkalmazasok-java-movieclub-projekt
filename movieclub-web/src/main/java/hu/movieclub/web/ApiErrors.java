package hu.movieclub.web;
import java.util.*;
import org.slf4j.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import jakarta.validation.ConstraintViolationException;

@RestControllerAdvice
public class ApiErrors {
    private static final Logger log=LoggerFactory.getLogger(ApiErrors.class);
    private ResponseEntity<Map<String,String>> error(HttpStatus status,String message) {
        return ResponseEntity.status(status).body(Map.of("message",message));
    }
    @ExceptionHandler(NoSuchElementException.class)
    ResponseEntity<?> missing(NoSuchElementException e) { return error(HttpStatus.NOT_FOUND,e.getMessage()); }
    @ExceptionHandler(SecurityException.class)
    ResponseEntity<?> denied(SecurityException e) { return error(HttpStatus.FORBIDDEN,e.getMessage()); }
    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<?> conflict() { return error(HttpStatus.CONFLICT,"Ez az adat már létezik, vagy ütközik egy másik módosítással. Frissítsd az oldalt."); }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<?> invalid(MethodArgumentNotValidException e) {
        return error(HttpStatus.BAD_REQUEST,e.getBindingResult().getFieldErrors().stream()
            .map(f -> f.getField()+": "+f.getDefaultMessage()).findFirst().orElse("Érvénytelen adatok."));
    }
    @ExceptionHandler({IllegalArgumentException.class,ConstraintViolationException.class,HttpMessageNotReadableException.class,
        org.springframework.web.method.annotation.HandlerMethodValidationException.class,
        org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class})
    ResponseEntity<?> badInput(Exception e) { return error(HttpStatus.BAD_REQUEST,"Ellenőrizd a megadott adatokat és azok hosszát."); }
    @ExceptionHandler(IllegalStateException.class)
    ResponseEntity<?> unavailable(IllegalStateException e) { return error(HttpStatus.SERVICE_UNAVAILABLE,e.getMessage()); }
}

