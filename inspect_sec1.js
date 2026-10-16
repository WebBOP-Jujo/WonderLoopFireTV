const fs = require('fs');
let html = fs.readFileSync('C:/adb/apk/R23/wonderloop-firetv.html', 'utf8');
html = html.replace(/data:image\/[^;]+;base64,[^"')]+/g, '[BASE64_IMAGE]');

const sec1 = html.substring(html.indexOf('<div class="lb">1 · Leer cuento'), html.indexOf('<div class="lb">2 · Pantalla principal'));
console.log(sec1);
