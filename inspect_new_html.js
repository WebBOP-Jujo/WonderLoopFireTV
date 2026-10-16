const fs = require('fs');
const html = fs.readFileSync('C:/adb/apk/R23/WonderLoopFireTV.html', 'utf8');

const regex = /<div class="lb">([\s\S]*?)<\/div>/g;
let match;
while ((match = regex.exec(html)) !== null) {
    console.log("LB:", match[1].trim());
}
