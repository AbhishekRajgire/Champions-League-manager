// Renders the standalone landing mockup to a high-res PNG.
//
// Usage:
//   npm i -D playwright        # one-time (downloads a Chromium build)
//   npx playwright install chromium
//   node tools/screenshot.mjs
//
// Output: landing-page-mockup.png (full page) at 2x device scale.
import { chromium } from 'playwright';
import { fileURLToPath, pathToFileURL } from 'node:url';
import { dirname, resolve } from 'node:path';

const __dirname = dirname(fileURLToPath(import.meta.url));
const htmlPath = resolve(__dirname, '..', 'landing-page-mockup.html');
const outPath = resolve(__dirname, '..', 'landing-page-mockup.png');

const browser = await chromium.launch();
const page = await browser.newPage({
  viewport: { width: 1440, height: 900 },
  deviceScaleFactor: 2, // crisp, retina-quality export
});

await page.goto(pathToFileURL(htmlPath).href, { waitUntil: 'networkidle' });
// Force all scroll-reveal sections visible before capture.
await page.evaluate(() => document.querySelectorAll('.reveal').forEach((el) => el.classList.add('in')));
await page.waitForTimeout(900); // let fonts + transitions settle

await page.screenshot({ path: outPath, fullPage: true });
await browser.close();

console.log('Saved', outPath);
