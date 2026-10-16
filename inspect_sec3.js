const fs = require('fs');
let html = fs.readFileSync('C:/adb/apk/R23/wonderloop-firetv.html', 'utf8');
html = html.replace(/data:image\/[^;]+;base64,[^"')]+/g, '[BASE64_IMAGE]');

const sec3 = html.substring(html.indexOf('<div class="lb">3 · Escanear con el móvil'), html.indexOf('<div class="lb">4 · Control parental'));
console.log(sec3);
