---
marp: true
size: 16:9
paginate: true
title: MC-cli
footer: MC-cli · DAI PW1 · HEIG-VD
style: |
  @import url('https://fonts.googleapis.com/css2?family=Fraunces:wght@500;600&family=Inter:wght@400;500;600&family=JetBrains+Mono:wght@400;500&display=swap');
  :root {
    --bg: #f4f1ea;
    --ink: #1f2b22;
    --muted: #5d6b5f;
    --moss: #3f5e3c;
    --fern: #7d9a5f;
    --bark: #8a6a4a;
    --card: #e8e3d6;
  }
  section {
    background: var(--bg);
    color: var(--ink);
    font-family: 'Inter', system-ui, sans-serif;
    font-size: 24px;
    padding: 56px 64px 64px;
    justify-content: flex-start;
  }
  h1, h2 {
    font-family: 'Fraunces', Georgia, serif;
    color: var(--moss);
    font-weight: 600;
    margin: 0 0 6px;
  }
  h1 { font-size: 54px; }
  h2 { font-size: 40px; }
  h3 {
    font-size: 19px;
    text-transform: uppercase;
    letter-spacing: .08em;
    color: var(--bark);
    margin: 14px 0 6px;
  }
  p, li { line-height: 1.4; }
  ul { padding-left: 1.1em; margin: 4px 0; }
  li::marker { color: var(--fern); }
  strong { color: var(--moss); }
  code {
    font-family: 'JetBrains Mono', monospace;
    background: var(--card);
    color: var(--ink);
    border-radius: 4px;
    padding: 1px 6px;
    font-size: .88em;
  }
  pre {
    background: #24332a;
    border-radius: 8px;
    padding: 14px 18px;
    margin: 6px 0;
  }
  pre code {
    background: none;
    color: #e6eadf;
    padding: 0;
    font-size: 17px;
    line-height: 1.5;
  }
  .lead { color: var(--muted); font-size: 22px; margin: 0 0 18px; }
  .cols { display: grid; grid-template-columns: 1fr 1fr; gap: 36px; align-items: start; }
  .cols.wide { grid-template-columns: 1.15fr .85fr; }
  .gallery { display: grid; grid-template-columns: 1fr 1fr; gap: 10px; }
  .gallery img, .cols img { width: 100%; border-radius: 6px; display: block; }
  .cap { font-size: 15px; color: var(--muted); margin-top: 4px; }
  .stats { display: flex; gap: 14px; margin: 10px 0 14px; }
  .stat { background: var(--card); border-left: 4px solid var(--fern); border-radius: 6px; padding: 8px 14px; flex: 1; }
  .stat b { display: block; font-family: 'Fraunces', serif; font-size: 34px; color: var(--moss); }
  .stat span { font-size: 15px; color: var(--muted); }
  footer { color: var(--muted); font-size: 14px; }
  section::after { color: var(--muted); font-size: 14px; }
  pre .hljs-built_in, pre .hljs-keyword { color: #b9d39a; }
  pre .hljs-comment { color: #8fa38f; font-style: normal; }
  pre .hljs-string, pre .hljs-number { color: #e2c98f; }
  .stat { padding: 6px 14px; }
  .stat b { font-size: 30px; line-height: 1.1; }
  .tight li { font-size: 21px; margin: 2px 0; }
  .mosaic-pair { display: grid; grid-template-columns: 1fr 1fr; gap: 8px; }
  .cmd { display: grid; grid-template-columns: 1fr 520px; gap: 28px; align-items: center; padding: 12px 0; border-top: 1px solid #d8d1c0; }
  .cmd:first-of-type { border-top: none; }
  .cmd h3 { margin: 0 0 4px; font-family: 'JetBrains Mono', monospace; text-transform: none; letter-spacing: 0; font-size: 24px; color: var(--moss); }
  .cmd p { margin: 0 0 6px; font-size: 20px; }
  .cmd code { font-size: 16px; }
  .ba { display: grid; grid-template-columns: 1fr 34px 1fr; align-items: center; gap: 8px; }
  .ba img { width: 100%; height: 132px; object-fit: contain; display: block; }
  .ba .arrow { color: var(--fern); font-size: 28px; text-align: center; }
  .ba .lbl { font-size: 13px; color: var(--muted); text-align: center; margin-top: 2px; }
  .flow { display: flex; align-items: center; gap: 10px; margin: 6px 0 22px; font-size: 18px; }
  .flow span.step { background: var(--card); border-radius: 999px; padding: 6px 14px; }
  .flow span.to { color: var(--fern); }
  .cards { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }
  .card { background: var(--card); border-left: 4px solid var(--fern); border-radius: 8px; padding: 12px 18px; }
  .card h3 { margin: 0 0 4px; }
  .card p { margin: 0; font-size: 19px; }

---

# MC-cli : trois commandes

<div class="cmd">
<div>

### split
<p>Découpe une image en une grille de tuiles, nommées <code>&lt;ligne&gt;_&lt;col&gt;_tile.bmp</code>.</p>

`mc-cli split forest.bmp tiles/ -r 2 -c 3`

</div>
<div class="ba">
<div><img src="img/forest.jpg"><div class="lbl">forest.bmp</div></div>
<div class="arrow">→</div>
<div><img src="img/split-tiles.jpg"><div class="lbl">6 tuiles 320×320</div></div>
</div>
</div>

<div class="cmd">
<div>

### merge
<p>Recolle des images : grille lue dans les noms, ou en ligne, colonne, grille <code>-r -c</code>, ou en boucle <code>-R</code>.</p>

`mc-cli merge 0_0_tile.bmp 1_2_tile.bmp -R 4 -o strip.bmp`

</div>
<div class="ba">
<div><img src="img/merge-before.jpg"><div class="lbl">2 tuiles</div></div>
<div class="arrow">→</div>
<div><img src="img/strip.jpg"><div class="lbl">A B A B · 1280×320</div></div>
</div>
</div>

<div class="cmd">
<div>

### mosaic
<p>Remplit chaque case de la grille avec la couleur moyenne de ses pixels.</p>

`mc-cli mosaic flowers.bmp -r 16 -c 20 -o mosaic.bmp`

</div>
<div class="ba">
<div><img src="img/flowers.jpg"><div class="lbl">flowers.bmp</div></div>
<div class="arrow">→</div>
<div><img src="img/flowers-mosaic.png"><div class="lbl">16×20 cases</div></div>
</div>
</div>

---

## Comment on a travaillé

<div class="flow">
<span class="step">Issue</span><span class="to">→</span>
<span class="step">Branche</span><span class="to">→</span>
<span class="step">PR vers <code>dev</code></span><span class="to">→</span>
<span class="step">Relecture de l'autre</span><span class="to">→</span>
<span class="step">Merge</span><span class="to">→</span>
<span class="step"><code>main</code> au rendu</span>
</div>

<div class="cards">
<div class="card">

### Issues
<p>Chaque tâche ou bug devient une issue avec une liste de cases à cocher. On voit ce qui reste et qui fait quoi.</p>

</div>
<div class="card">

### Pull requests
<p>24 PR vers <code>dev</code>, chacune relue par l'autre avant le merge. Commits au format Conventional Commits.</p>

</div>
<div class="card">

### Tests
<p>79 tests JUnit 5 sur de petites images connues pixel par pixel. <code>./mvnw test</code> doit passer avant chaque merge.</p>

</div>
<div class="card">

### IA
<p>Claude Code et Gemini : messages git, débogage, une partie du code, explications. Tout relu, testé et validé en PR.</p>

</div>
</div>
