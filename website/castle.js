/* ============================================================
   IRONHOLD — "The Keep" scroll-built WebGL castle
   Renders dark_castle_v3.isc (a real in-game Ironhold structure)
   with three.js. Scroll progress (window.__keepProgress, set by
   main.js) raises the build cutaway and orbits the camera.
   Parser + block colours adapted from tools/isc_viewer.
   ============================================================ */
import * as THREE from "three";

const section = document.querySelector(".keep-section");
const canvas = document.getElementById("keepCanvas");
const hudShown = document.getElementById("keepBlocks");
const hudTotal = document.getElementById("keepTotal");

const RULES = [
  ["soul_torch", 0x4fd8d8], ["soul_lantern", 0x4fd8d8], ["soul_fire", 0x4fd8d8],
  ["soul_sand", 0x4a3b2e],
  ["wall_torch", 0xffb347], ["torch", 0xffb347],
  ["lantern", 0xffa033],
  ["fire", 0xff7722],
  ["red_candle", 0xc03030], ["black_candle", 0x262626], ["candle", 0xd8d0b8],
  ["chain", 0x6a6a72],
  ["gilded_blackstone", 0x8a6a22],
  ["cracked_polished_blackstone_bricks", 0x241f29],
  ["polished_blackstone_bricks", 0x2a242f],
  ["polished_blackstone", 0x322b38], ["chiseled_polished_blackstone", 0x3a3342],
  ["blackstone", 0x1f1a24],
  ["cracked_deepslate_bricks", 0x383840],
  ["deepslate_bricks", 0x40404a],
  ["deepslate_tile", 0x30303a],
  ["polished_deepslate", 0x4a4a55],
  ["cobbled_deepslate", 0x3a3a44],
  ["chiseled_deepslate", 0x46465a],
  ["crying_obsidian", 0x3a1a6a],
  ["obsidian", 0x16101f],
  ["dark_oak_log", 0x2e1d0c], ["dark_oak_plank", 0x42301a],
  ["dark_oak_fence", 0x42301a], ["dark_oak_stairs", 0x42301a],
  ["dark_oak_slab", 0x42301a], ["dark_oak_door", 0x3a2812],
  ["dark_oak_trapdoor", 0x3a2812], ["dark_oak_pressure", 0x42301a],
  ["bookshelf", 0x6a4a24],
  ["iron_bars", 0x9aa0a8], ["iron_block", 0xd8d8de],
  ["gold_block", 0xeac94e],
  ["tinted_glass", 0x241a3a],
  ["glass_pane", 0x8a93a8],
  ["black_banner", 0x101014], ["red_banner", 0x8a1418],
  ["black_bed", 0x18181c],
  ["red_carpet", 0x8a1418], ["black_carpet", 0x141418], ["gray_carpet", 0x4a4a52],
  ["water", 0x2f4fd0],
  ["cobweb", 0xd8d8dc],
  ["sculk_vein", 0x0e4a58], ["sculk", 0x0b3340],
  ["netherrack", 0x6b2c2c], ["magma", 0x8a4422],
  ["hay_block", 0xc8a832],
  ["wither_skeleton_skull", 0x2a2a2e], ["skeleton_skull", 0xd8d8c8], ["skeleton_wall_skull", 0xd8d8c8],
  ["wither_rose", 0x222018], ["dead_bush", 0x7a5a30],
  ["coarse_dirt", 0x5a4030],
  ["anvil", 0x4a4a50], ["grindstone", 0x6a6a70],
  ["smithing_table", 0x3a3026], ["fletching_table", 0xb8a37a], ["cartography_table", 0x6a4a32],
  ["enchanting_table", 0x9a2a4a], ["brewing_stand", 0x8a8a60],
  ["lectern", 0x7a5a30],
  ["chest", 0x7a5526], ["barrel", 0x5a3f22],
  ["smoker", 0x4a4640], ["blast_furnace", 0x55555c], ["furnace", 0x55555c],
  ["cauldron", 0x3a3a40], ["composter", 0x4a3a22],
  ["ladder", 0x6a5232], ["tripwire_hook", 0x9a9a72],
];
const DEFAULT_COLOR = 0x8a8a92;
const TRANSLUCENT = ["glass", "water", "cobweb"];
const EMISSIVE = ["torch", "lantern", "fire", "candle", "glow"];

function colorFor(id) {
  for (const [k, c] of RULES) if (id.includes(k)) return c;
  return DEFAULT_COLOR;
}

function parseIsc(text) {
  const lines = text.split("\n");
  let i = 0;
  if (!lines[i].startsWith("#")) throw new Error("not an ISC file");
  i++;
  const parts = lines[i++].split(/\s+/);
  const sx = parseInt(parts[1]), sy = parseInt(parts[2]), sz = parseInt(parts[3]);
  if (lines[i++].trim() !== "palette") throw new Error("bad palette header");
  const pal = {};
  while (i < lines.length && lines[i].trim() !== "body") {
    const m = lines[i].trim().match(/^(\S+)\s+(\S+)$/);
    if (m) pal[m[1]] = m[2];
    i++;
  }
  i++; // skip 'body'
  const blocks = [];
  let y = -1, z = 0;
  for (; i < lines.length; i++) {
    const ln = lines[i];
    if (ln.startsWith("y=")) { y = parseInt(ln.slice(2)); z = 0; continue; }
    if (!ln.trim()) continue;
    const codes = ln.trim().split(/\s+/);
    for (let x = 0; x < codes.length; x++) {
      const bs = pal[codes[x]];
      if (!bs || bs === "minecraft:air") continue;
      blocks.push({ x, y, z, id: bs });
    }
    z++;
  }
  return { sx, sy, sz, blocks };
}

function init(data) {
  const { sx, sy, sz, blocks } = data;
  blocks.sort((a, b) => a.y - b.y || a.z - b.z || a.x - b.x);

  const renderer = new THREE.WebGLRenderer({ canvas, antialias: true, alpha: true });
  renderer.setPixelRatio(Math.min(devicePixelRatio, 1.75));
  const scene = new THREE.Scene();
  scene.fog = new THREE.FogExp2(0x060509, 0.0038);
  const camera = new THREE.PerspectiveCamera(50, 1, 0.5, 3000);

  scene.add(new THREE.AmbientLight(0xa8a2c8, 1.7));
  scene.add(new THREE.HemisphereLight(0x8a7ddb, 0xff7a18, 0.55));
  const moon = new THREE.DirectionalLight(0xcdd4ff, 2.6);
  moon.position.set(1.5, 2.2, 0.8);
  scene.add(moon);
  const emberLight = new THREE.DirectionalLight(0xff8a3d, 1.1);
  emberLight.position.set(-1.2, 0.4, -1.5);
  scene.add(emberLight);

  // sorted-by-y instancing: mesh.count raises the keep as you scroll
  const opaque = [], trans = [], glows = [];
  for (const b of blocks) {
    (TRANSLUCENT.some((t) => b.id.includes(t)) ? trans : opaque).push(b);
    if (EMISSIVE.some((t) => b.id.includes(t))) glows.push(b);
  }

  const geo = new THREE.BoxGeometry(1, 1, 1);
  function makeMesh(list, transparent) {
    if (!list.length) return null;
    const mat = new THREE.MeshLambertMaterial({ transparent, opacity: transparent ? 0.45 : 1 });
    const mesh = new THREE.InstancedMesh(geo, mat, list.length);
    const M = new THREE.Matrix4();
    const C = new THREE.Color();
    list.forEach((b, i) => {
      M.makeTranslation(b.x - sx / 2, b.y, b.z - sz / 2);
      mesh.setMatrixAt(i, M);
      mesh.setColorAt(i, C.setHex(colorFor(b.id)));
    });
    mesh.instanceMatrix.needsUpdate = true;
    if (mesh.instanceColor) mesh.instanceColor.needsUpdate = true;
    scene.add(mesh);
    return mesh;
  }
  const opaqueMesh = makeMesh(opaque, false);
  const transMesh = makeMesh(trans, true);

  // additive sprites where the torches burn
  let glowPts = null;
  if (glows.length) {
    const pos = new Float32Array(glows.length * 3);
    glows.forEach((b, i) => {
      pos[i * 3] = b.x - sx / 2;
      pos[i * 3 + 1] = b.y + 0.4;
      pos[i * 3 + 2] = b.z - sz / 2;
    });
    const pgeo = new THREE.BufferGeometry();
    pgeo.setAttribute("position", new THREE.BufferAttribute(pos, 3));
    const pmat = new THREE.PointsMaterial({
      color: 0xffa033, size: 3.2, sizeAttenuation: true,
      transparent: true, opacity: 0.85,
      blending: THREE.AdditiveBlending, depthWrite: false,
    });
    glowPts = new THREE.Points(pgeo, pmat);
    scene.add(glowPts);
  }

  // cumulative block counts per y-level → O(1) reveal per frame
  function cumByY(list) {
    const cum = new Array(sy + 1).fill(0);
    let i = 0;
    for (let level = 0; level < sy; level++) {
      while (i < list.length && list[i].y <= level) i++;
      cum[level + 1] = i;
    }
    return cum;
  }
  const cumOpaque = cumByY(opaque);
  const cumTrans = cumByY(trans);
  const cumGlow = cumByY(glows);

  hudTotal.textContent = blocks.length.toLocaleString();

  function resize() {
    const w = canvas.clientWidth, h = canvas.clientHeight;
    if (!w || !h) return;
    renderer.setSize(w, h, false);
    camera.aspect = w / h;
    camera.updateProjectionMatrix();
  }
  resize();
  addEventListener("resize", resize);

  // only render while the section is on screen (always-on in ?snap mode)
  const snapMode = new URLSearchParams(location.search).has("snap");
  let visible = snapMode;
  if (!snapMode) {
    new IntersectionObserver((es) => { visible = es[0].isIntersecting; },
      { rootMargin: "20% 0px" }).observe(section);
  }

  const d = Math.max(sx, sz);
  let lastShown = -1, idle = 0;

  renderer.setAnimationLoop(() => {
    if (!visible) return;
    const p = window.__keepProgress ?? 0;
    idle += 0.0016;

    // build reveal
    const levelF = 1 + p * (sy - 1);
    const level = Math.min(Math.floor(levelF), sy - 1);
    if (opaqueMesh) opaqueMesh.count = cumOpaque[level + 1];
    if (transMesh) transMesh.count = cumTrans[level + 1];
    if (glowPts) glowPts.geometry.setDrawRange(0, cumGlow[level + 1]);

    const shown = cumOpaque[level + 1] + cumTrans[level + 1];
    if (shown !== lastShown) {
      lastShown = shown;
      hudShown.textContent = shown.toLocaleString();
    }

    // orbit: scroll sweeps the camera, idle drift keeps it alive
    const ang = -0.7 + p * 2.6 + idle;
    const radius = d * 1.95 - p * d * 0.55;
    const height = sy * 0.8 + p * sy * 0.5;
    camera.position.set(Math.cos(ang) * radius, height, Math.sin(ang) * radius);
    camera.lookAt(0, sy * 0.3, 0);

    renderer.render(scene, camera);
  });
}

fetch("assets/dark_castle_v3.isc")
  .then((r) => { if (!r.ok) throw new Error("HTTP " + r.status); return r.text(); })
  .then((text) => init(parseIsc(text)))
  .catch((err) => {
    console.warn("Keep viewer unavailable:", err);
    section.classList.add("keep-fallback");
  });
