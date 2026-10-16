const fs = require('fs');
let html = fs.readFileSync('C:/adb/apk/R23/wonderloop-firetv.html', 'utf8');

const sec2 = html.substring(html.indexOf('<div class="lb">2 · Pantalla principal'), html.indexOf('<div class="lb">3 · Escanear con el móvil'));
console.log('=== SEC 2 HTML ===\n', sec2);

// Let's also find all css rules for .hero, .band, .grp, .cont, .pol, .new, .newf, .steps, .tile, etc.
const style = html.substring(html.indexOf('<style>'), html.indexOf('</style>'));
console.log('\n=== RELEVANT STYLES ===\n');
const lines = style.split('\n');
lines.forEach(l => {
  if (l.match(/\.(top|logo|chip2|av|hero|hl|hr|steps|tile|ico|phn|scl|ar|idea|band|grp|cards|cont|cv|tx|tag|meta|prog|btn|new|newf|pol)/)) {
    console.log(l);
  }
});
