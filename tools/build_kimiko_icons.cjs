const fs = require('fs');
const path = require('path');
const sharp = require('C:/Users/Administrator/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules/sharp');
const root = path.resolve(__dirname, '..');
const donor = path.join(root, 'src/main/resources/assets/activity/textures/gui/kimiko-icons');
const atlasPath = path.join(root, 'src/main/resources/assets/nivoratclient/textures/gui/nivorat_icons_atlas.png');
const source = fs.readFileSync(path.join(root, 'src/main/java/activity/client/gui/icon/ActivityIcon.java'), 'utf8');
const names = {ANIMATION:'animation',CHECK:'check',CLOSE:'close',COPY:'duplicate',FONT:'language',GLASS:'glass',
  IMPORT:'import',KEYBIND:'bind',PIN:'pinned',PROFILE:'profile',SEARCH:'search',SETTINGS:'settings',SOUND:'sound',
  TRASH:'delete',COMBAT:'modules',DEFENSE:'potions',UTILITY:'utils',CONFIG:'import',ABOUT:'more',
  CHEVRON_DOWN:'chevron-down',CHEVRON_RIGHT:'next',EXPORT:'export',REFRESH:'speed',SAVE:'check'};
(async () => {
  const overlay = [];
  const copied = [];
  for (const match of source.matchAll(/^    (\w+)\((\d+), (\d+), 24, 24,/gm)) {
    const name = names[match[1]];
    if (!name) continue;
    let svgFile = path.join(donor, name + '.svg');
    if (!fs.existsSync(svgFile)) svgFile = path.join(donor, name.replace(/-/g,'_') + '.svg');
    if (!fs.existsSync(svgFile)) continue;
    const svg = fs.readFileSync(svgFile,'utf8').replaceAll('currentColor','#ffffff');
    const input = await sharp(Buffer.from(svg)).resize(24,24).png().toBuffer();
    overlay.push({input, left:Number(match[2]), top:Number(match[3])});
    copied.push({icon:match[1],source:path.basename(svgFile),u:Number(match[2]),v:Number(match[3])});
    const dst = path.join(root,'src/main/resources/assets/activity/textures/gui/kimiko-icons',path.basename(svgFile));
    fs.mkdirSync(path.dirname(dst),{recursive:true});
    if (path.resolve(svgFile) !== path.resolve(dst)) fs.copyFileSync(svgFile,dst);
  }
  // Rebuild the mapped cells from SVG; keep existing unmapped semantic icons.
  const original = await sharp(path.join(root, 'tools/assets/base-icons.png')).raw().ensureAlpha().toBuffer({resolveWithObject:true});
  for (const r of copied) for(let y=r.v;y<r.v+24;y++) for(let x=r.u;x<r.u+24;x++) {
    const offset = (y*original.info.width+x)*4;original.data.fill(0,offset,offset+4);
  }
  const png = await sharp(original.data,{raw:original.info}).composite(overlay).png().toBuffer();
  fs.writeFileSync(atlasPath,png);
  const activityAtlas = path.join(root,'src/main/resources/assets/activity/textures/gui/nivorat_icons_atlas.png');
  if(fs.existsSync(activityAtlas)) fs.writeFileSync(activityAtlas,png);
  fs.writeFileSync(path.join(root,'docs/kimiko-icon-mapping.json'),JSON.stringify(copied,null,2)+'\n');
  // Nine-slice rounded panel texture, compiled once, tinted and reused at runtime.
  const rounded = '<svg xmlns="http://www.w3.org/2000/svg" width="16" height="16"><rect width="16" height="16" rx="7" fill="white"/></svg>';
  const roundedPath = path.join(root,'src/main/resources/assets/activity/textures/gui/rounded_panel.png');
  fs.writeFileSync(roundedPath,await sharp(Buffer.from(rounded)).png().toBuffer());
  const border = '<svg xmlns="http://www.w3.org/2000/svg" width="16" height="16"><rect x="0.5" y="0.5" width="15" height="15" rx="6.5" fill="none" stroke="white"/></svg>';
  fs.writeFileSync(path.join(path.dirname(roundedPath),'rounded_border.png'),await sharp(Buffer.from(border)).png().toBuffer());
  console.log(`Compiled ${copied.length} Kimiko SVG icons into the existing atlas.`);
})();
