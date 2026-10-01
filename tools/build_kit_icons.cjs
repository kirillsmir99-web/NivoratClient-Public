const fs = require('fs');
const path = require('path');
const sharp = require('C:/Users/Administrator/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules/sharp');
const root = path.resolve(__dirname, '..');
const source = path.join(root, 'tools/assets/kit-icons');
const names = ['layout-grid','flask-conical','end-crystal','water-bucket','shield','hammer','diamond-sword','plain-helmet','axe','flask-round'];

(async () => {
  const overlays = [];
  for (let i = 0; i < names.length; i++) {
    const svg = fs.readFileSync(path.join(source, names[i] + '.svg'), 'utf8').replaceAll('currentColor', '#ffffff');
    const input = await sharp(Buffer.from(svg)).resize(40, 40).png().toBuffer();
    overlays.push({input, left: i * 48 + 4, top: 4});
  }
  const output = path.join(root, 'src/main/resources/assets/activity/textures/gui/kit_icons.png');
  fs.mkdirSync(path.dirname(output), {recursive: true});
  await sharp({create: {width: 480, height: 48, channels: 4, background: {r:0,g:0,b:0,alpha:0}}})
    .composite(overlays).png().toFile(output);
  console.log('Compiled 10 kit icons into one 480x48 atlas.');
})().catch(error => { console.error(error.message); process.exitCode = 1; });
