# MovieClub

A MovieClub a **Modern webes alkalmazások (Java)** tárgyhoz készített filmklub-alkalmazás. A tagok filmeket kereshetnek, értékelhetnek, nézési listát vezethetnek és személyre szabott ajánlásokat kaphatnak. A moderátor kezeli a filmkatalógust és törölheti a kifogásolható értékeléseket.

## A feladat fő szabályai

- Egy felhasználó egy filmet egyszer értékelhet. Az ismételt mentés a korábbi értékelést módosítja; az adatbázis egyedi kulcsa is kizárja a duplikációt.
- Az ajánló csak a helyi katalógusban szereplő filmekből választ. A már értékelt és megnézett filmeket kihagyja.
- A 4–5 csillagos értékelések műfajai és a közösségi pontszámok alapján rangsorol. Az ajánlásokhoz rövid indoklás tartozik.

## Megvalósítás

A projekt Java 21-et és hárommodulos Mavent használ: a **core** tartalmazza az üzleti modellt és az ajánlót, az **infrastructure** az adatbázis- és TMDB-kapcsolatot, a **web** pedig a Spring Boot alkalmazást és a felületet. A fő technológiák: Spring MVC, Spring Security, Spring Data JPA, Liquibase, H2 és Thymeleaf. Az alkalmazás beépített Tomcat szerverrel fut; nincs külön frontend-fordítás.

A külső integráció a TMDB filmes metaadat-API-ja. Moderátorként film kereshető és importálható, ha a `TMDB_TOKEN` be van állítva. Az importált film ezután a helyi katalógus része. Az ajánlások indoklását a program állítja elő, futás közben nem használ LLM-et.

## Indítás

Windows alatt a repó letöltése vagy klónozása után indítsd el az **INDITAS.cmd** fájlt, majd nyisd meg a [http://localhost:8080](http://localhost:8080) címet. Első alkalommal az indító letölti a hordozható Java 21-et és a Maven-függőségeket, lefuttatja a teszteket, majd elindítja az alkalmazást. Ehhez internetkapcsolat szükséges; külön Java-, Maven- vagy adatbázis-telepítés nem kell.

| Szerepkör | Felhasználónév | Jelszó |
|---|---|---|
| Tag | `tag` | `MovieClub123!` |
| Moderátor | `moderator` | `Moderator123!` |

Az adatok a projekt `data` mappájában maradnak meg. Leállítás: **LEALLITAS.cmd**. Az indítás további részletei és a TMDB-token beállítása az [indítási útmutatóban](OLVASS-EL.md) szerepelnek.

## Ellenőrzés és korlátok

A projektben 27 JUnit 5, Mockito, Spring-integrációs és HTTP-komponens teszt van. A teljes tesztcsomag a **TESZTELES.cmd** fájllal vagy beállított Java 21 mellett a `.\mvnw.cmd clean verify` paranccsal futtatható. A [tesztelési jegyzőkönyv](ELLENORZES.md) tartalmazza a kipróbált eseteket.

A helyi filmkatalógus, az értékelések, a listák és az ajánlások TMDB-token nélkül működnek. Az éles TMDB-import saját API-token nélkül nem volt kipróbálható; a kapcsolódó kódot helyi HTTP-tesztszerverrel ellenőriztem. A projekt helyi egyetemi bemutatóra készült, nyilvános üzemeltetésre nincs előkészítve.

Részletesebb leírás: [architektúra és adatmodell](DOKUMENTACIO.md) · [bemutatási vázlat](BEMUTATO.md).
