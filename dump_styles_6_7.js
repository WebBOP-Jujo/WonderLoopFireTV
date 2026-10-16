const fs = require('fs');
let html = fs.readFileSync('C:/adb/apk/R23/WonderLoopFireTV.html', 'utf8');
const style = html.substring(html.indexOf('<style>'), html.indexOf('</style>'));

const rules = style.split('}');
for (const r of rules) {
  if (r.includes('.hd3') || r.includes('.pnw') || r.includes('.pnl') || r.includes('.pnr') || r.includes('.pro3') || r.includes('.sech') || r.includes('.prow3') || r.includes('.prc') || r.includes('.av3') || r.includes('.addc') || r.includes('.lst') || r.includes('.lr') || r.includes('.npw') || r.includes('.npl') || r.includes('.npr') || r.includes('.prev') || r.includes('.ages') || r.includes('.okb') || r.includes('.avs') || r.includes('.fld') || r.includes('.kb')) {
    console.log(r.trim() + '}');
  }
}
