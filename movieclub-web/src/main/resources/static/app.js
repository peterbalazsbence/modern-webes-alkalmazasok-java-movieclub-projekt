'use strict';
const app=document.querySelector('#app');
let session={},catalog=[],routeVersion=0;
const esc=v=>String(v??'').replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
const genres=()=>[...new Set(catalog.flatMap(f=>f.movie.genres))].sort((a,b)=>a.localeCompare(b,'hu'));
const score=v=>v?Number(v).toFixed(1).replace('.',','):'–';
function toast(message,error=false){const t=document.querySelector('#toast');t.textContent=message;t.className='visible'+(error?' error':'');clearTimeout(toast.timer);toast.timer=setTimeout(()=>t.className='',5500);}
async function api(path,options={}){
 const headers={...options.headers};
 if(options.body && !(options.body instanceof URLSearchParams))headers['Content-Type']='application/json';
 if(options.method && options.method!=='GET')headers[session.csrfHeader||'X-CSRF-TOKEN']=session.csrf;
 const response=await fetch('/api'+path,{credentials:'same-origin',...options,headers});
 if(!response.ok){let data;try{data=await response.json();}catch{}throw new Error(data?.message||'A kérés nem sikerült. Próbáld újra.');}
 return response.status===204?null:response.json();
}
async function refresh(){session=await api('/session');catalog=await api('/movies');navigation();}
function navigation(){
 const active=(location.hash||'#catalog').split('/')[0];
 const links=[['#catalog','Felfedezés'],['#watchlist','Saját listám'],['#recommendations','Neked ajánljuk']];
 if(session.role==='MODERATOR')links.push(['#admin','Moderáció']);
 document.querySelector('#nav').innerHTML=links.map(([href,label])=>'<a class="'+(active===href?'active':'')+'" href="'+href+'">'+label+'</a>').join('');
 document.querySelector('#account').innerHTML=session.username
 ?'<span class="avatar">'+esc(session.username[0].toUpperCase())+'</span><span class="account-name">'+esc(session.username)+'</span><button class="text-button" data-action="logout">Kilépés</button>'
 :'<a class="button compact secondary" href="#login">Bejelentkezés <span aria-hidden="true">↗</span></a>';
}
function cover(movie,extra=''){
 const type=(movie.id||movie.externalId||1)%6;
 return '<div class="cover theme-'+type+' '+extra+'" aria-hidden="true"><div class="cover-orbit"></div><div class="cover-grain"></div><span class="cover-top">MOVIECLUB SELECTION / '+esc(movie.year||'—')+'</span><div class="cover-title">'+esc(movie.title)+'</div><span class="cover-bottom">'+esc([...movie.genres].slice(0,2).join(' / ')||'FILM')+'</span></div>';
}
function card(f,extra=''){
 const m=f.movie||f;
 return '<article class="film-card"><a class="cover-link" href="#film/'+m.id+'" aria-label="'+esc(m.title)+' adatlapja">'+cover(m)+(f.votes?'<span class="rating-badge">★ '+score(f.average)+'</span>':'')+'</a><div class="card-meta"><span>'+esc(m.year||'Ismeretlen év')+'</span><span>'+esc([...m.genres].sort()[0]||'Film')+'</span></div><h3><a href="#film/'+m.id+'">'+esc(m.title)+'</a></h3>'+extra+'</article>';
}
function heading(kicker,title,subtitle=''){return '<div class="page-heading"><p class="eyebrow">'+kicker+'</p><h1>'+title+'</h1>'+(subtitle?'<p class="muted">'+subtitle+'</p>':'')+'</div>';}
function empty(title,text){return '<div class="empty"><span class="empty-icon">◇</span><h2>'+title+'</h2><p>'+text+'</p><a class="button secondary" href="#catalog">Filmek felfedezése ↗</a></div>';}
function loginNeeded(){app.innerHTML=heading('A TE FILMKLUBOD','Lépj be a történetbe.','A saját listád és az ajánlásaid a bejelentkezés után várnak.')+'<a class="button" href="#login">Bejelentkezés ↗</a>'; }
function catalogPage(){
 app.innerHTML='<section class="hero"><div class="hero-copy"><p class="eyebrow"><span class="live-dot"></span> EGY HELY A JÓ FILMEKNEK</p><h1>A következő<br>kedvenced<br><em>itt kezdődik.</em></h1><p>Fedezz fel történeteket, mondd el a véleményed,<br class="desktop"> és találd meg azt a filmet, ami igazán neked szól.</p><div class="actions"><a class="button" href="#recommendations">Mutasd az ajánlásaimat <span>↗</span></a><a class="text-link" href="#films">Böngészek inkább ↓</a></div><div class="hero-stats"><strong>'+catalog.length+'<small>film a klubban</small></strong><span class="stat-line"></span><strong>Te döntesz.<small>A saját listád. A saját ízlésed.</small></strong></div></div><div class="hero-art"><span class="orbit-label">EGY ÚJ TÖRTÉNET VÁR RÁD</span>'+(catalog.slice(0,3).map((f,i)=>'<a aria-label="Film megnyitása" class="hero-poster hp-'+i+'" href="#film/'+f.movie.id+'">'+cover(f.movie)+'</a>').join(''))+'<span class="curated-badge">✦<br><small>Fedezd fel.<br>Éld át.</small></span></div></section><section id="films"><div class="section-heading"><div><p class="eyebrow">A KATALÓGUS</p><h2>Mit nézünk ma?</h2></div><span id="film-count" class="muted">'+catalog.length+' film</span></div><div class="filters"><label class="search"><span aria-hidden="true">⌕</span><input id="search" type="search" maxlength="200" placeholder="Keress egy filmre…" aria-label="Keresés cím alapján"></label><div class="genre-chips" id="genre-chips"><button class="chip selected" data-genre="">Összes</button>'+genres().map(g=>'<button class="chip" data-genre="'+esc(g)+'">'+esc(g)+'</button>').join('')+'</div><label class="sort-label"><span>Rendezés</span><select id="sort"><option value="title">Cím szerint</option><option value="rating">Legjobbra értékelt</option><option value="year">Legújabb először</option></select></label></div><div id="film-grid" class="film-grid"></div></section>';
 updateGrid();
}
function updateGrid(){
 const q=document.querySelector('#search').value.toLocaleLowerCase('hu');
 const genre=document.querySelector('.chip.selected')?.dataset.genre||'';
 const sort=document.querySelector('#sort').value;
 let films=catalog.filter(f=>f.movie.title.toLocaleLowerCase('hu').includes(q)&&(!genre||f.movie.genres.includes(genre)));
 films.sort((a,b)=>sort==='rating'?b.average-a.average:sort==='year'?b.movie.year-a.movie.year:a.movie.title.localeCompare(b.movie.title,'hu'));
 document.querySelector('#film-grid').innerHTML=films.length?films.map(f=>card(f)).join(''):empty('Most nincs találat.','Próbálj másik címet vagy műfajt.');
 document.querySelector('#film-count').textContent=films.length+' film';
}
async function detailsPage(id,version){
 const data=await api('/movies/'+id);
 let watches=session.username?await api('/watchlist'):[];
 if(version!==routeVersion)return;
 const m=data.movie, own=data.ratings.find(r=>r.username===session.username), watch=watches.find(w=>w.movie.id===m.id);
 const avg=data.ratings.length?data.ratings.reduce((s,r)=>s+r.score,0)/data.ratings.length:0;
 app.innerHTML='<a class="back-link" href="#catalog">← Vissza a filmekhez</a><section class="detail"><div>'+cover(m,'detail-cover')+'<p class="art-note">MovieClub grafika · nem hivatalos filmplakát</p></div><div class="detail-copy"><p class="eyebrow">'+esc(m.year||'Ismeretlen év')+' / '+esc([...m.genres].sort().join(' · '))+'</p><h1>'+esc(m.title)+'</h1><div class="detail-rating"><span>★ '+score(avg)+'</span><small>'+data.ratings.length+' értékelés</small></div><p class="synopsis">'+esc(m.description||'Ehhez a filmhez még nincs leírás.')+'</p>'+(session.username?'<div class="panel"><h3>A nézési listád</h3><div class="actions"><button class="button '+(watch?.status==='PLANNED'?'':'secondary')+'" data-action="watch" data-id="'+m.id+'" data-status="PLANNED">＋ Meg szeretném nézni</button><button class="button '+(watch?.status==='WATCHED'?'':'secondary')+'" data-action="watch" data-id="'+m.id+'" data-status="WATCHED">✓ Megnéztem</button>'+(watch?'<button class="text-button" data-action="unwatch" data-id="'+m.id+'">Levétel a listáról</button>':'')+'</div></div><form id="rating-form" data-id="'+m.id+'" class="panel"><h3>'+ (own?'A te értékelésed szerkesztése':'Te mit gondolsz róla?')+'</h3><label>Pontszám<select name="score" required>'+[5,4,3,2,1].map(v=>'<option value="'+v+'" '+(own?.score===v?'selected':'')+'>'+v+' / 5 '+ '★'.repeat(v)+'</option>').join('')+'</select></label><label>Vélemény <span class="muted">(nem kötelező)</span><textarea name="review" maxlength="2000" rows="3" placeholder="Mi fogott meg benne?">'+esc(own?.review||'')+'</textarea></label><p class="form-error" role="alert"></p><button class="button" type="submit">Értékelés mentése ↗</button><p class="form-note">Filmenként egy értékelésed lehet. Mentéskor a meglévő frissül.</p></form>':'<div class="panel"><h3>Oszd meg a véleményed.</h3><p class="muted">Értékelj és építs saját nézési listát.</p><a class="button" href="#login">Bejelentkezés ↗</a></div>')+(session.role==='MODERATOR'?'<a class="text-link" href="#edit/'+m.id+'">Film szerkesztése →</a>':'')+'</div></section><section class="reviews"><div class="section-heading"><h2>A klub véleménye</h2><span class="muted">'+data.ratings.length+' értékelés</span></div>'+(data.ratings.length?data.ratings.map(r=>'<article class="review"><div class="review-top"><strong><span class="avatar">'+esc(r.username[0].toUpperCase())+'</span>'+esc(r.username)+'</strong><span class="stars">'+'★'.repeat(r.score)+'<span class="dim">'+'★'.repeat(5-r.score)+'</span></span></div><p>'+esc(r.review||'Szöveges vélemény nélkül.')+'</p>'+((session.username===r.username||session.role==='MODERATOR')?'<button class="text-button delete-link" data-action="delete-rating" data-id="'+r.id+'">Értékelés törlése</button>':'')+'</article>').join(''):'<p class="muted">Még nincs értékelés. Legyél te az első!</p>')+'</section>';
}
async function watchPage(version){
 if(!session.username)return loginNeeded();
 const items=await api('/watchlist');if(version!==routeVersion)return;
 app.innerHTML=heading('SZEMÉLYES GYŰJTEMÉNY','A listád. A te estéd.','Gyűjtsd egy helyre a következő filmélményeidet.')+(items.length?['PLANNED','WATCHED'].map(status=>{
  const list=items.filter(i=>i.status===status);
  return '<section class="list-section"><div class="section-heading"><h2>'+(status==='PLANNED'?'Még előttem van':'Már láttam')+'</h2><span class="muted">'+list.length+' film</span></div><div class="film-grid">'+(list.length?list.map(i=>card(i,'<div class="watch-actions"><button class="text-button" data-action="watch" data-id="'+i.movie.id+'" data-status="'+(status==='PLANNED'?'WATCHED':'PLANNED')+'">'+(status==='PLANNED'?'✓ Megnéztem':'↶ Még megnézném')+'</button><button class="text-button" data-action="unwatch" data-id="'+i.movie.id+'">Eltávolítás</button></div>')).join(''):'<p class="muted">Ebben a listában még nincs film.</p>')+'</div></section>';
 }).join(''):empty('Minden jó lista egy filmmel kezdődik.','Válassz egy filmet a katalógusból, és add a saját listádhoz.'));
}
async function recommendationsPage(version){
 if(!session.username)return loginNeeded();
 const items=await api('/recommendations');if(version!==routeVersion)return;
 app.innerHTML=heading('AZ ÍZLÉSEDRE HANGOLVA','Neked válogattuk.','A kedvelt műfajaid és a klub értékelései alapján. Csak olyan filmek, amelyeket még nem jelöltél megnézettnek és nem értékeltél.')
 +'<div class="info-strip"><span>✦</span><p><strong>Egyre jobban megismerünk.</strong> A 4 és 5 csillagos értékeléseid segítenek megtalálni a hozzád illő történeteket.</p></div>'
 +(items.length?'<div class="film-grid">'+items.map(i=>card(i,'<p class="recommend-reason">✦ '+esc(i.reason)+'</p>')).join('')+'</div>':empty('Már az egész klubot felfedezted.','Új ajánlásokhoz további filmekre van szükség a katalógusban.'));
}
function authPage(register=false){
 app.innerHTML='<section class="auth-layout"><div>'+heading('ÜDV A KLUBBAN',register?'Kezdj egy új<br>történetet.':'Jó újra<br>itt látni.','A kedvenc filmjeid egy helyen.')+'<div class="auth-art"><span>PLAY<br>YOUR<br><em>STORY.</em></span><i>✳</i></div></div><div class="auth-panel"><h2>'+(register?'Regisztráció':'Bejelentkezés')+'</h2><p class="muted">'+(register?'Hozd létre a saját filmes profilodat.':'Folytasd, ahol legutóbb abbahagytad.')+'</p><form id="'+(register?'register-form':'login-form')+'"><label>Felhasználónév<input name="username" autocomplete="username" required minlength="3" maxlength="40" pattern="[a-zA-Z0-9_]{3,40}" placeholder="pl. filmrajongo"></label>'+(register?'<p class="form-note">3–40 karakter: ékezet nélküli betű, szám vagy aláhúzás.</p>':'')+'<label>Jelszó<input type="password" name="password" autocomplete="'+(register?'new-password':'current-password')+'" required '+(register?'minlength="8" maxlength="60"':'')+' placeholder="'+(register?'Legalább 8 karakter':'A jelszavad')+'"></label><p class="form-error" role="alert"></p><button class="button full" type="submit">'+(register?'Csatlakozom a klubhoz':'Belépés')+' ↗</button></form><p class="auth-switch">'+(register?'Már van fiókod? <a href="#login">Jelentkezz be.</a>':'Még nem vagy tag? <a href="#register">Csatlakozz.</a>')+'</p>'+(session.demo&&!register?'<div class="demo-box"><strong>Kipróbálnád?</strong><p>Tag: <code>tag</code> / <code>MovieClub123!</code><br>Moderátor: <code>moderator</code> / <code>Moderator123!</code></p><small>Helyi bemutatófiókok.</small></div>':'')+'</div></section>';
}
function movieForm(movie={}){
 const editing=!!movie.id;
 app.innerHTML='<a class="back-link" href="#admin">← Moderáció</a>'+heading('KATALÓGUSKEZELÉS',editing?'Film szerkesztése':'Új történet a klubban')+'<form id="movie-form" data-id="'+(movie.id||'')+'" class="panel editor"><label>Cím<input name="title" maxlength="200" required value="'+esc(movie.title||'')+'"></label><div class="form-row"><label>Megjelenési év<input type="number" name="year" min="1888" max="2200" required value="'+(movie.year||2024)+'"></label><label>Műfajok, vesszővel elválasztva<input name="genres" maxlength="600" required placeholder="Sci-fi, Dráma" value="'+esc((movie.genres||[]).join(', '))+'"></label></div><label>Rövid leírás<textarea name="description" rows="6" maxlength="4000" required>'+esc(movie.description||'')+'</textarea></label><p class="form-error" role="alert"></p><div class="actions"><button class="button" type="submit">Film mentése ↗</button><a class="button secondary" href="#admin">Mégse</a></div></form>';
}
function adminPage(){
 app.innerHTML=heading('MODERÁTORI FELÜLET','Tartsd mozgásban a klubot.','Kezeld a filmeket, vagy hozz be új történeteket a TMDB katalógusából.')
 +'<div class="actions admin-toolbar"><a class="button" href="#new">＋ Új film kézzel</a><span class="muted">'+catalog.length+' film a katalógusban</span></div><section class="panel import-panel"><h2>Filmek importálása</h2><p class="muted">Keress a TMDB-ben, majd importáld a kiválasztott filmet a saját katalógusba.</p>'+(session.tmdbConfigured?'<form id="import-search"><div class="inline-form"><input name="q" aria-label="Film címe a TMDB-ben" required maxlength="200" placeholder="Melyik filmet keresed?"><button class="button secondary" type="submit">Keresés ↗</button></div><p class="form-error" role="alert"></p></form>':'<div class="info-strip"><p>Az import még nincs bekapcsolva. Az indítási útmutatóban leírt módon állítsd be a saját <code>TMDB_TOKEN</code> értékedet, majd indítsd újra az alkalmazást.</p></div>')+'<div id="import-results"></div><p class="form-note">This product uses the TMDB API but is not endorsed or certified by TMDB. <a href="https://www.themoviedb.org/" target="_blank" rel="noopener noreferrer">TMDB ↗</a></p></section><div class="table-wrap"><table><thead><tr><th>Film</th><th>Év</th><th>Műfaj</th><th>Műveletek</th></tr></thead><tbody>'+catalog.map(f=>'<tr><td><a href="#film/'+f.movie.id+'">'+esc(f.movie.title)+'</a></td><td>'+esc(f.movie.year||'—')+'</td><td>'+esc([...f.movie.genres].sort().join(', '))+'</td><td><div class="actions"><a class="text-link" href="#edit/'+f.movie.id+'">Szerkesztés</a><button class="text-button delete-link" data-action="delete-movie" data-id="'+f.movie.id+'">Törlés</button></div></td></tr>').join('')+'</tbody></table></div>';
}
async function route(){
 const version=++routeVersion;
 navigation();
 const hash=(location.hash|| (location.pathname==='/login'?'#login':'#catalog')).slice(1);
 const [page,id]=hash.split('/');
 app.innerHTML='<p class="loading">Egy pillanat, betöltünk…</p>';
 try {
  if(['admin','new','edit'].includes(page)&&session.role!=='MODERATOR'){app.innerHTML=heading('HOZZÁFÉRÉS','Ez a moderátorok területe.');return;}
  if(page==='film')await detailsPage(Number(id),version);
  else if(page==='watchlist')await watchPage(version);
  else if(page==='recommendations')await recommendationsPage(version);
  else if(page==='login'||page==='register')authPage(page==='register');
  else if(page==='admin')adminPage();
  else if(page==='new')movieForm();
  else if(page==='edit'){const d=await api('/movies/'+Number(id));if(version===routeVersion)movieForm(d.movie);}
  else if(page==='about')app.innerHTML=heading('A PROJEKTRŐL','MovieClub.','Modern webes alkalmazások (Java) – egyetemi projekt.')+'<section class="panel prose"><h2>Mitől személyes?</h2><p>Az ajánló a 4–5 csillagos értékeléseid műfajait figyeli. A közösségi pontszámokat a szavazatok számával súlyozza. A megnézett és már értékelt filmeket kihagyja, és kizárólag a helyi katalógusból válogat.</p><h2>Filmes adatok és grafikák</h2><p>A kezdőadatok bemutatóadatok; a vélemények fiktívek. A borítók saját tipografikus grafikák, nem hivatalos filmplakátok. A moderátor TMDB-adatokat is importálhat.</p><p><img src="/tmdb.svg" alt="TMDB" width="123" height="16"></p><p>This product uses the TMDB API but is not endorsed or certified by TMDB.</p><a href="https://www.themoviedb.org/" target="_blank" rel="noopener noreferrer">The Movie Database ↗</a></section>';
  else {catalogPage();if(page==='films')document.querySelector('#films').scrollIntoView({behavior:'smooth'});}
 }catch(e){if(version===routeVersion)app.innerHTML=heading('VALAMI KÖZBEJÖTT','Ezt most nem tudtuk betölteni.',esc(e.message))+'<button class="button" data-action="retry">Újrapróbálom</button>';}
 if(page!=='films')window.scrollTo(0,0);
}
async function confirmDelete(text){
 const dialog=document.querySelector('#confirm');document.querySelector('#confirm-text').textContent=text;
 return new Promise(resolve=>{dialog.addEventListener('close',()=>resolve(dialog.returnValue==='ok'),{once:true});dialog.showModal();});
}
document.addEventListener('input',e=>{if(e.target.id==='search')updateGrid();});
document.addEventListener('change',e=>{if(e.target.id==='sort')updateGrid();});
document.addEventListener('click',async e=>{
 const chip=e.target.closest('[data-genre]');
 if(chip){document.querySelectorAll('.chip').forEach(c=>c.classList.remove('selected'));chip.classList.add('selected');updateGrid();return;}
 const b=e.target.closest('[data-action]');if(!b)return;
 const id=b.dataset.id;
 b.disabled=true;
 try{
  switch(b.dataset.action){
   case 'retry': await refresh();await route();break;
   case 'logout': await api('/logout',{method:'POST'});await refresh();location.hash='#catalog';await route();toast('Sikeresen kijelentkeztél.');break;
   case 'watch':await api('/watchlist/'+id,{method:'PUT',body:JSON.stringify({status:b.dataset.status})});await route();toast('A listád frissült.');break;
   case 'unwatch':await api('/watchlist/'+id,{method:'DELETE'});await route();toast('A filmet levettük a listádról.');break;
   case 'delete-rating':
    if(await confirmDelete('Az értékelés és a hozzá tartozó vélemény végleg törlődik.')){await api('/ratings/'+id,{method:'DELETE'});await refresh();await route();toast('Értékelés törölve.');}break;
   case 'delete-movie':
    if(await confirmDelete('A film, az értékelései és a hozzá tartozó nézési listás bejegyzések is törlődnek.')){await api('/moderator/movies/'+id,{method:'DELETE'});await refresh();await route();toast('Film törölve.');}break;
   case 'import':
    await api('/moderator/import/'+id,{method:'POST'});await refresh();b.textContent='✓ A katalógusban';b.dataset.action='';toast('A film bekerült a katalógusba.');break;
  }
 }catch(err){toast(err.message,true);}finally{b.disabled=false;}
});
document.addEventListener('submit',async e=>{
 const form=e.target;if(!form.id)return;e.preventDefault();
 const error=form.querySelector('.form-error'),button=form.querySelector('[type=submit]'),f=Object.fromEntries(new FormData(form));
 if(error)error.textContent='';if(button)button.disabled=true;
 try{
  switch(form.id){
   case 'login-form':
    await api('/login',{method:'POST',body:new URLSearchParams(f)});await refresh();location.hash='#catalog';toast('Üdv a klubban, '+session.username+'!');break;
   case 'register-form':
    await api('/register',{method:'POST',body:JSON.stringify(f)});location.hash='#login';toast('Sikeres regisztráció. Jelentkezz be az új fiókoddal.');break;
   case 'rating-form':
    await api('/movies/'+form.dataset.id+'/rating',{method:'PUT',body:JSON.stringify({score:Number(f.score),review:f.review})});await refresh();await route();toast('Az értékelésed elmentettük.');break;
   case 'movie-form':{
    const body={title:f.title.trim(),year:Number(f.year),description:f.description.trim(),genres:[...new Set(f.genres.split(',').map(g=>g.trim()).filter(Boolean))]};
    const film=await api('/moderator/movies'+(form.dataset.id?'/'+form.dataset.id:''),{method:form.dataset.id?'PUT':'POST',body:JSON.stringify(body)});
    await refresh();location.hash='#film/'+film.id;toast('Film elmentve.');break;}
   case 'import-search':{
    const result=await api('/moderator/import/search?q='+encodeURIComponent(f.q));
    document.querySelector('#import-results').innerHTML=result.length?result.map(m=>'<div class="import-row"><div><strong>'+esc(m.title)+'</strong><p class="muted">'+esc(m.year||'Ismeretlen év')+' · '+esc(m.description.slice(0,160))+'</p></div><button class="button secondary compact" data-action="import" data-id="'+m.externalId+'">＋ Importálás</button></div>').join(''):'<p>Nincs találat. Próbálj másik címet.</p>';break;}
  }
 }catch(err){if(error)error.textContent=err.message;else toast(err.message,true);}finally{if(button)button.disabled=false;}
});
window.addEventListener('hashchange',route);
(async()=>{try{await refresh();await route();}catch(e){app.innerHTML=heading('KAPCSOLÓDÁSI HIBA','Az alkalmazás nem érhető el.',esc(e.message))+'<button class="button" data-action="retry">Újrapróbálom</button>';}})();



