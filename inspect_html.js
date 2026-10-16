const fs = require('fs');
let html = fs.readFileSync('C:/adb/apk/R23/wonderloop-firetv.html', 'utf8');

// Replace large base64 data URLs
html = html.replace(/data:image\/[^;]+;base64,[^"')]+/g, '[BASE64_IMAGE]');

// Split by .blk or find each section
const parts = html.split('<div class="blk">');
console.log('Total blk sections:', parts.length - 1);

parts.slice(1).forEach((part, i) => {
    const endIdx = part.indexOf('<ul class="nt">');
    const content = endIdx !== -1 ? part.substring(0, endIdx) : part.substring(0, 1000);
    console.log(`\n================ SECTION ${i+1} ================\n`);
    console.log(content.trim());
});
