const fs = require('fs');
let html = fs.readFileSync('C:/adb/apk/R23/wonderloop-firetv.html', 'utf8');
const style = html.substring(html.indexOf('<style>'), html.indexOf('</style>'));

const rules = style.split('}');
for (const r of rules) {
  const clean = r.replace(/data:image\/[^;]+;base64,[^"')]+/g, '[IMG]').trim();
  if (clean.includes('.band') || clean.includes('.grp') || clean.includes('.cards') || clean.includes('.cont') || clean.includes('.cv') || clean.includes('.tx') || clean.includes('.tag') || clean.includes('.meta') || clean.includes('.prog') || clean.includes('.btn') || clean.includes('.new') || clean.includes('.plus') || clean.includes('.cnt') || clean.includes('.pol') || clean.includes('.newf') || clean.includes('.idea') || clean.includes('.hero') || clean.includes('.steps') || clean.includes('.tile') || clean.includes('.ico') || clean.includes('.ar') || clean.includes('.phn') || clean.includes('.scl')) {
    console.log(clean + '}');
  }
}
