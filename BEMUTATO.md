# MovieClub – bemutatási forgatókönyv

## 0. Előkészület

Indítsd el az INDITAS.cmd fájllal. Legyen megnyitva a projekt a fejlesztőkörnyezetben. Az API-import bemutatásához előre állítsd be a saját TMDB_TOKEN értékét. Az értékelések fiktív demóadatok.

## 1. A probléma – 30 másodperc

„A MovieClub egy filmklub-alkalmazás. A tag filmeket böngészhet, értékelhet, saját nézési listát vezethet, és az ízléséhez illő ajánlásokat kaphat. A moderátor kezeli a katalógust.”

Mutasd a keresőt, műfaji szűrőt és a rendezést.

## 2. Az MVP – 2 perc

1. Jelentkezz be a tag / MovieClub123! fiókkal.
2. Nyiss meg egy filmadatlapot.
3. Jelöld: „Meg szeretném nézni”.
4. A Saját listám oldalon mutasd meg, majd állítsd „Megnéztem” állapotra.
5. Adj 4 vagy 5 csillagot és egy rövid véleményt.
6. Mentsd újra módosított pontszámmal.

Magyarázat: „Ugyanahhoz a felhasználóhoz és filmhez csak egy értékelés tartozik. Az ismételt mentés azt módosítja. Az adatbázis egyedi kulcsa párhuzamos kéréseknél is megakadályozza a duplikációt.”

## 3. A komplex funkció – 2 perc

Nyisd meg a Neked ajánljuk oldalt.

„A magasra értékelt filmjeim műfajaiból pontokat számolunk. Az ajánló ezt kombinálja a közösségi értékelések simított átlagával. A már értékelt és megnézett filmeket kizárja. Csak a helyi adatbázis filmjeit használja.”

Mutasd meg a műfaji indoklást. Ha új fiókkal próbálod, magyarázd el a közösségi kiinduló ajánlást.

## 4. Moderáció és külső API – 2 perc

1. Lépj ki, és lépj be moderator / Moderator123! fiókkal.
2. A Moderáció oldalon adj hozzá egy tesztfilmet.
3. Szerkeszd a leírását.
4. Ha van API-token: keress egy filmet a TMDB-ben, és importáld.
5. Mutasd meg az importált film helyi adatlapját.
6. Szükség esetén törölj egy fiktív demóvéleményt a megerősítő ablak használatával.

„A keresési találat még külső adat; csak az importálás után kerülhet az ajánlások közé.”

Ha nincs token, mondd ki: az éles import nincs beállítva, a HTTP-integrációt automatikus helyi tesztek igazolják.

## 5. Felépítés és tesztek – 2 perc

Mutasd meg:
- a három Maven-modult;
- a RecommendationService konstruktort és ClubStore interfészt;
- a Liquibase schema.sql fájljában az uq_rating kulcsot;
- a RecommendationServiceTest egy tesztjét;
- a ClubIntegrationTest jogosultsági tesztjét.

A TESZTELES.cmd futtatásával mutasd a tesztek eredményét. A teljes tesztcsomag 27 teszt.

## Lehetséges kérdések

**Miért H2?**  
Fájlba ment, újraindításkor megmarad, külön adatbázisszerver nélkül egyszerűen kipróbálható.

**Miért nem Reactive?**  
A rendszer kis méretű, a perzisztencia blokkoló JPA-t használ. Itt az összetettebb reaktív felépítés nem adna érdemi előnyt.

**Miért nem JWT?**  
A felület és a szerver egy alkalmazás. A session-alapú belépés illeszkedik ehhez, CSRF-védelemmel.

**Miért külön modul az ajánló?**  
A webes és adatbáziskörnyezettől függetlenül tesztelhető.

**Miért kell adatbázis-korlát is?**  
Két egyidejű kérés alkalmazásszintű ellenőrzése önmagában még létrehozhatna duplikációt.

**Az ajánló AI?**  
Nem. Determinisztikus, tartalomalapú ajánló. A feladat külső integrációs követelményét a TMDB API teljesíti. A fejlesztéshez használtunk AI-t.

**Hol van a JSP?**  
A megbeszélt változat Spring MVC-t és Thymeleaf-oldalvázat használ. JSP nem szerepel benne; ha az oktató kötelezővé teszi, a nézetréteget módosítani kell.
