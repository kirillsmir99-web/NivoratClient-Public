const fs = require('fs');
const path = require('path');
const sharp = require('C:/Users/Administrator/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules/sharp');
const root = path.resolve(__dirname, '..');
const source = path.join(root, 'tools/assets/kit-icons');
const items = [
  'minecraft_nether_star.png',     // 0: ALL (Все)
  'minecraft_splash_potion.png',   // 1: NPOT (НПОТ)
  'minecraft_end_crystal.png',     // 2: CRYSTAL (КПВП)
  'minecraft_golden_apple.png',    // 3: UHC (УХК)
  'minecraft_shield.png',          // 4: SMP (СМП)
  'minecraft_mace.png',            // 5: MACE (Мейсы)
  'minecraft_diamond_sword.png',   // 6: BEAST (Бисты)
  'minecraft_netherite_sword.png', // 7: SWORD (OP)
  'minecraft_diamond_axe.png',     // 8: AXE (Топоры)
  'minecraft_potion.png',          // 9: DPOT (ДПОТ)
  'minecraft_tnt_minecart.png'     // 10: CART (КАРТ)
];

(async () => {
  const contentSize = 64;
  const pad = 4;
  const stride = contentSize + pad * 2;
  const totalW = items.length * stride;
  const totalH = stride;
  const overlays = [];
  for (let i = 0; i < items.length; i++) {
    const isShield = items[i] === 'minecraft_shield.png';
    const input = await sharp(path.join(source, items[i]))
      .resize(contentSize, contentSize, { kernel: isShield ? 'lanczos3' : 'nearest' })
      .png()
      .toBuffer();
    overlays.push({input, left: i * stride + pad, top: pad});
  }
  const output = path.join(root, 'src/main/resources/assets/activity/textures/gui/kit_icons.png');
  fs.mkdirSync(path.dirname(output), {recursive: true});
  await sharp({create: {width: totalW, height: totalH, channels: 4, background: {r:0,g:0,b:0,alpha:0}}})
    .composite(overlays).png().toFile(output);
  console.log(`Compiled ${items.length} vanilla kit icons into one ${totalW}x${totalH} atlas with ${pad}px anti-bleed padding.`);
})().catch(error => { console.error(error.message); process.exitCode = 1; });

