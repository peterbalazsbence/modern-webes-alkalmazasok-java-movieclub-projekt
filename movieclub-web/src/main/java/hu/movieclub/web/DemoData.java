package hu.movieclub.web;
import hu.movieclub.core.*;
import java.util.*;
import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component @ConditionalOnProperty(name="movieclub.demo",havingValue="true")
public class DemoData implements ApplicationRunner {
    private final ClubStore store; private final PasswordEncoder encoder;
    public DemoData(ClubStore s,PasswordEncoder e) {store=s;encoder=e;}
    @Override @Transactional public void run(ApplicationArguments args) {
        // Only the first startup seeds data: deleted demo films stay deleted.
        if(store.account("moderator").isPresent()) return;
        store.createAccount("moderator",encoder.encode("Moderator123!"),"MODERATOR");
        var member=store.createAccount("tag",encoder.encode("MovieClub123!"),"MEMBER");
        var anna=store.createAccount("anna",encoder.encode("MovieClub123!"),"MEMBER");
        var mark=store.createAccount("mark",encoder.encode("MovieClub123!"),"MEMBER");
        String[][] data={
            {"Interstellar","2014","Egy expedíció a csillagok között keresi az emberiség jövőjét, miközben az idő és a családi kötelékek próbára teszik az utazókat.","Sci-fi,Kaland,Dráma"},
            {"Eredet","2010","Egy különleges csapat álmokon keresztül próbál elültetni egy gondolatot. A küldetés során a valóság határai egyre bizonytalanabbá válnak.","Sci-fi,Thriller"},
            {"A Grand Budapest Hotel","2014","Egy legendás portás és fiatal pártfogoltja egy eltűnt festmény körüli kalandba keveredik egy különös európai szállodában.","Vígjáték,Kaland"},
            {"Érkezés","2016","Egy nyelvész ismeretlen látogatók üzenetét próbálja megfejteni, és közben az időről alkotott elképzelése is megváltozik.","Sci-fi,Dráma"},
            {"Whiplash","2014","Egy fiatal dobos és könyörtelen tanára kapcsolatában összecsap az ambíció, a fegyelem és a siker ára.","Dráma"},
            {"Dűne","2021","Egy fiatal örökös sorsa összefonódik egy sivatagbolygó népével és a birodalom legértékesebb erőforrásával.","Sci-fi,Kaland"},
            {"Lelki ismeretek","2020","Egy zenetanár váratlan utazáson fedezi fel, mitől válik értékessé egy hétköznapi pillanat.","Animáció,Dráma"},
            {"Élősködők","2019","Két család élete találkozik egy elegáns házban, ahol a társadalmi különbségek lassan feszültséggé alakulnak.","Thriller,Dráma"},
            {"A Gyűrűk Ura: A Gyűrű Szövetsége","2001","Egy hobbit és társai veszélyes útra indulnak, hogy megakadályozzák egy hatalmas erejű gyűrű rossz kezekbe kerülését.","Fantasy,Kaland"},
            {"La La Land","2016","Egy színésznő és egy jazz-zongorista Los Angelesben próbálja összeegyeztetni az álmait és a kapcsolatát.","Romantikus,Dráma"},
            {"A sötét lovag","2008","Gotham védelmezőjének egy kiszámíthatatlan ellenféllel szemben kell megvizsgálnia saját határait.","Akció,Bűnügyi,Thriller"},
            {"WALL·E","2008","Egy magányos takarítórobot találkozása egy látogatóval az egész emberiség jövőjét megváltoztathatja.","Animáció,Sci-fi"}
        };
        List<Movie> movies=new ArrayList<>();
        for(var d:data) movies.add(store.saveMovie(new Movie(null,d[0],Integer.parseInt(d[1]),d[2],Set.of(d[3].split(",")),null)));
        for(int i=0;i<movies.size();i++) {
            store.saveRating(anna.id(),movies.get(i).id(),i%3==0?5:4,"Demóvélemény: emlékezetes történet, érdemes megnézni.");
            store.saveRating(mark.id(),movies.get(i).id(),i%4==0?3:5,"Demóvélemény: remek hangulat és izgalmas karakterek.");
        }
        store.saveRating(member.id(),movies.get(0).id(),5,"Nagyon tetszett a történet és a zene.");
        store.saveRating(member.id(),movies.get(1).id(),4,"Ötletes és látványos.");
        store.saveWatch(member.id(),movies.get(0).id(),WatchItem.Status.WATCHED);
        store.saveWatch(member.id(),movies.get(5).id(),WatchItem.Status.PLANNED);
    }
}

