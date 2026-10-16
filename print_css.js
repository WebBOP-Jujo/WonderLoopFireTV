const fs = require('fs');
let html = fs.readFileSync('C:/adb/apk/R23/wonderloop-firetv.html', 'utf8');
const style = html.substring(html.indexOf('<style>'), html.indexOf('</style>'));

const rules = style.split('}');
for (const r of rules) {
  if (r.includes('.cont') || r.includes('.band') || r.includes('.grp') || r.includes('.cards') || r.includes('.new') || r.includes('.pol') || r.includes('.idea') || r.includes('.hero') || r.includes('.steps') || r.includes('.tile') || r.includes('.ico')) {
    console.log(r.trim() + '}');
  }
}
