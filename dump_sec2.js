const fs = require('fs');
let html = fs.readFileSync('C:/adb/apk/R23/wonderloop-firetv.html', 'utf8');

const style = html.substring(html.indexOf('<style>'), html.indexOf('</style>'));
const sec2Html = html.substring(html.indexOf('<div class="lb">2 · Pantalla principal'), html.indexOf('<div class="lb">3 · Escanear con el móvil'));
console.log("=== SEC2 HTML ===");
console.log(sec2Html.replace(/data:image\/[^;]+;base64,[^"')]+/g, '[IMG]'));

console.log("=== ALL STYLES ===");
// find classes mentioned in sec2
const classes = ['tv', 'top', 'logo', 'chip2', 'pd', 'av', 'hero', 'hl', 'cta', 'hr', 'steps', 'tile', 'ico', 'dr', 'draw', 'phn', 'scl', 'sp', 'ar', 'idea', 'band', 'grp', 'cards', 'cont', 'cv', 'tx', 'tag', 'meta', 'prog', 'btn', 'new', 'plus', 'cnt', 'pol', 'newf', 'foc'];

const styleLines = style.split('}');
styleLines.forEach(block => {
  if (classes.some(c => block.includes('.' + c))) {
    console.log(block.trim() + '}');
  }
});
