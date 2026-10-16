const fs = require('fs');
let html = fs.readFileSync('C:/adb/apk/R23/wonderloop-firetv.html', 'utf8');
const p = html.indexOf('<div class="lb">2 · Pantalla principal');
const sub = html.substring(p, p + 5000);
const nt = sub.indexOf('<ul class="nt">');
console.log(sub.substring(nt, sub.indexOf('</ul>', nt) + 5));
