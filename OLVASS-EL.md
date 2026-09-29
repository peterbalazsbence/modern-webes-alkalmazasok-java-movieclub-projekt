# MovieClub – indítás egyszerűen

## 1. Elindítás Windows alatt

1. Ha ZIP-et kaptál, **először csomagold ki az egész mappát** egy írható helyre, például a Dokumentumok közé.
2. Kattints kétszer az **INDITAS.cmd** fájlra.
3. Várd meg az indulást. A böngésző automatikusan megnyílik: **http://localhost:8080**.
4. Az indítóablak maradjon nyitva. Leállítás: **Ctrl+C** az indítóablakban, vagy dupla kattintás a **LEALLITAS.cmd** fájlra.

A GitHub-repó a forráskódot tartalmazza, az előre fordított JAR-t és a hordozható Java-környezetet nem. Az INDITAS.cmd első futáskor letölti a hordozható Java 21-et (~200 MB), ellenőrzi a letöltés SHA-256 lenyomatát, letölti a Maven-függőségeket, lefuttatja a teszteket, majd elkészíti és elindítja a programot. Ehhez internetkapcsolat és néhány perc szükséges. Az eszközök a projekt `.runtime` mappájába kerülnek, a rendszer Java-beállításai nem változnak. Külön Maven-, Docker- vagy adatbázis-telepítés nem szükséges.

### Bemutatófiókok

| Szerepkör | Felhasználónév | Jelszó |
|---|---|---|
| Tag | `tag` | `MovieClub123!` |
| Moderátor | `moderator` | `Moderator123!` |

A regisztrációval saját tagfiókot is létrehozhatsz. A további demófelhasználók (`anna`, `mark`) értékelései fiktív bemutatóadatok. A regisztráció nem ad moderátori jogot.

## 2. Ha nem indul

### A 8080-as port foglalt

Ne állíts le ismeretlen alkalmazást. Nyiss terminált a MovieClub mappájában, és futtasd:

```powershell
.\INDITAS.cmd -Port 8081
```

Ezután a cím http://localhost:8081.

### A Java letöltése nem sikerül

Az első indításhoz internet és hozzáférés szükséges az Adoptium/GitHub letöltési oldalaihoz. Ellenőrizd a kapcsolatot, majd indítsd újra. Az indító a hiányzó Java-előkészítést újra megpróbálja.

### Nem nyílik meg automatikusan a böngésző

Nyisd meg kézzel a http://localhost:8080 címet. Az indítóablakban a `Started MovieClubApplication` sor jelzi a sikeres indulást.

### Java 8 van a gépemen

Az indító saját Java 21-et használ, ezért nem kell eltávolítani a Java 8-at. Fejlesztőkörnyezetben a projekt JDK-jának Java 21-et válassz.

### Bezártam az ablakot

Futtasd ismét az INDITAS.cmd fájlt. Az adatbázis megmarad.

## 3. Hol vannak az adatok?

A projekt **data/movieclub.mv.db** fájljában. A Liquibase az első induláskor létrehozza a szükséges táblákat. Újraindításkor az adatok megmaradnak.

Biztonsági másolathoz előbb állítsd le az alkalmazást, majd másold ki a `data` mappát. Ha teljesen tiszta bemutatót szeretnél, leállítás után nevezd át a `data` mappát például `data-mentes` névre; a következő indulás új adatbázist és demóadatokat hoz létre. Ez nem törli a félretett másolatot.

## 4. Külső filmes API bekapcsolása

A **TMDB-import** a feladat külső integrációja. A moderátor cím alapján kereshet, és egy kattintással a helyi katalógusba importálhatja a kiválasztott film adatait.

1. Hozz létre saját fiókot a [TMDB oldalán](https://www.themoviedb.org/).
2. A fiók API-beállításainál igényelj hozzáférést.
3. Másold ki az **API Read Access Token** értékét. Bearer tokent használunk, nem a rövid API Key mezőt.
4. Állítsd le a MovieClubot.
5. A projekt mappájában nyiss PowerShellt, és add meg:

```powershell
$env:TMDB_TOKEN = 'IDE_KERUL_A_SAJAT_READ_ACCESS_TOKEN'
.\INDITAS.cmd
```

6. Jelentkezz be moderátorként, és a **Moderáció → Filmek importálása** részen keress egy filmet.

A beállítás erre a terminálmunkamenetre vonatkozik. A tokent ne írd a forráskódba, ne add be a projektcsomagban és ne töltsd fel Gitbe. A keresési találat önmagában még nem ajánlható film: csak az importálás után kerül a saját katalógusba.

API-kulcsot a repó nem tartalmaz. Az éles TMDB-hozzáférés nem volt kipróbálható saját token nélkül; az integráció leképezését és hibakezelését helyi tesztszerver ellenőrizte.

A felület saját tipografikus borítókat használ, nem hivatalos filmplakátokat. A TMDB-ből cím, év, leírás és műfajok importálhatók.

## 5. Fordítás és tesztelés

- **TESZTELES.cmd**: teljes újrafordítás és az automatikus tesztek futtatása.
- **UJRAFORDITAS.cmd**: szintén teljes ellenőrzött fordítás, majd az indítható JAR frissítése.
- Forráskód-változtatás után futtasd az UJRAFORDITAS.cmd fájlt, majd indítsd újra az alkalmazást.
- Az első fordítás internetet igényel a Maven és a függőségek letöltéséhez. A későbbi fordítások a helyi gyorsítótárat használják.

Fejlesztőként, beállított Java 21 mellett:

```powershell
.\mvnw.cmd clean verify
java -jar .\movieclub-web\target\movieclub-web-1.0.0.jar
```

Ez a Maven-parancs a modul `target` mappájába fordít. A gyökérben lévő `movieclub.jar` fájlt a kényelmi UJRAFORDITAS.cmd frissíti.

IntelliJ IDEA/Eclipse alatt a **gyökér pom.xml** fájlt importáld Maven-projektként. A futtatandó főosztály: `hu.movieclub.MovieClubApplication`. A munkamappa a projekt gyökere legyen, hogy ugyanazt a fájladatbázist használd, mint a parancsfájlos indítás.

## 6. Helyi bemutatóra előkészített beállítások

Az alkalmazás alapértelmezetten csak a saját gépről érhető el (127.0.0.1), a demóadatok pedig bekapcsoltak. Nyilvános üzemeltetés nincs előkészítve. A `MOVIECLUB_DEMO=false` az új demóadatok betöltését tiltja le; a már létrejött bemutatófiókokat nem törli.

További leírás:
- **DOKUMENTACIO.md** – felépítés, adatmodell, végpontok, tantárgyi kapcsolódás.
- **BEMUTATO.md** – lépések egy 8–10 perces bemutatóhoz.
- **ELLENORZES.md** – ellenőrzési eredmények és ismert korlátok.
