const fs = require('fs');
let html = fs.readFileSync('C:/adb/apk/R23/WonderLoopFireTV.html', 'utf8');

// Replace base64 data URLs to make reading clean
html = html.replace(/data:image\/[^;]+;base64,[^"')]+/g, '[BASE64_IMAGE]');

const p6 = html.indexOf('<div class="lb">6 · Panel de padres');
const p7 = html.indexOf('<div class="lb">7 · Nuevo perfil');

if (p6 !== -1) {
    const sec6 = p7 !== -1 ? html.substring(p6, p7) : html.substring(p6);
    console.log('================ SECTION 6: Panel de padres ================\n');
    console.log(sec6);
}

if (p7 !== -1) {
    const sec7 = html.substring(p7);
    console.log('\n================ SECTION 7: Nuevo perfil ================\n');
    console.log(sec7);
}
