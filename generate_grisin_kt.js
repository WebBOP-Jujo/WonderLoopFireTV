const fs = require('fs');
const cover = fs.readFileSync('C:/adb/apk/R23/grisin_cover.base64', 'utf8');
const pol = fs.readFileSync('C:/adb/apk/R23/grisin_polaroid.base64', 'utf8');

const kt = 'package org.wonderloop.app.ui\n\n' +
  'object DemoGrisinAssets {\n' +
  '    const val COVER_BASE64 = "' + cover + '"\n' +
  '    const val POLAROID_BASE64 = "' + pol + '"\n' +
  '}\n';

fs.writeFileSync('composeApp/src/commonMain/kotlin/org/wonderloop/app/ui/DemoGrisinAssets.kt', kt);
console.log('Generated DemoGrisinAssets.kt successfully!');
