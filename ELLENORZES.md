# Ellenőrzési eredmények

Dátum: 2026. szeptember 15.

## Automatikus tesztek

A végleges Windows-indító fordítási módja sikeresen futott:

`powershell.exe -NoProfile -ExecutionPolicy Bypass -File start.ps1 -BuildOnly`

Ez a projekt saját Maven Wrapperével `clean verify` ellenőrzést végez, majd frissíti az indítható movieclub.jar fájlt.

| Tesztcsomag | Tesztek | Hibák | Kihagyott |
|---|---:|---:|---:|
| RecommendationServiceTest | 6 | 0 | 0 |
| ClubIntegrationTest | 18 | 0 | 0 |
| TmdbMetadataProviderTest | 3 | 0 | 0 |
| **Összesen** | **27** | **0** | **0** |

A fordítás mindhárom modulra sikeres. A tesztek Java 21, Maven 3.9.11 és Windows alatt futottak.

## Böngészőben ellenőrizve

- A főoldal és a magyar feliratok megjelennek.
- A cím szerinti kereső Interstellar keresésre egy találatot ad.
- A tag és a moderátor bemutatófiókkal is sikeres a belépés és a kilépés.
- A saját értékelés pontszáma szerkeszthető; a film értékeléseinek száma nem nő az ismételt mentéstől.
- A nézési listán a PLANNED és WATCHED állapot váltható.
- A megnézettnek jelölt Dűne eltűnik az ajánlásokból.
- A moderátori filmszerkesztő betölti az adatokat és sikeresen ment.
- A hiányzó TMDB-token állapota érthetően megjelenik.
- A katalógus és a fejléc asztali, valamint 390 × 844 méretű böngészőnézetben is ellenőrizve.
- A javított mobilnézetnél a dokumentum szélessége és görgetési szélessége megegyezik, nincs oldalirányú kilógás.
- Újraindítás után a listabejegyzések megmaradtak, a Liquibase nem hozta létre újra a sémát.
- A start.ps1 a hordozható Java-környezetből elindította a programot.
- A stop.ps1 felismerte és leállította ennek a projektnek a Java-példányát.

## Fontos határok

- Éles TMDB-token nem állt rendelkezésre. A valódi szolgáltatással történő importot a saját token beállítása után még ki kell próbálni. A HTTP-kérés, az adatleképezés és a hibaágak helyi tesztszerverrel ellenőrizve.
- Az indító Java-letöltésének URL-jét és checksum-ellenőrzését az előkészítés során használtuk. A végleges start.ps1 indítását már előkészített Java-mappával teszteltük; egy teljesen üres gépen történő futtatás nem történt.
- A tesztelt indítás a -NoBrowser kapcsolót használta, a programlapot a tesztböngészőben nyitottuk meg. A rendszer alapértelmezett böngészőjének automatikus megnyitását külön nem ellenőriztük.
- A borítók saját grafikák. Nincs hivatalos plakátletöltés vagy LLM-hívás.
- Helyi egyetemi bemutatóhoz készült. Nincs jelszó-visszaállítás, e-mail-hitelesítés, üzemeltetési naplókezelés, terhelésteszt vagy nyilvános telepítés.
- JSP, saját servlet, időzített feladat és Spring Reactive nincs benne. Az oktató külön előírása esetén ezekről egyeztetni kell.

## A ZIP tartalma

Forráskód, tesztek, Maven Wrapper, dokumentáció, Windows-parancsfájlok és az előre fordított movieclub.jar. A .runtime, target és data mappák nem részei a ZIP-nek. Másik gépen az indító külön tölti le a hordozható Java-környezetet, és friss demóadatbázist hoz létre.
