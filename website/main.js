/* ============================================================
   IRONHOLD landing — choreography, particles, portraits, voices
   ============================================================ */

(() => {
  "use strict";

  const reduceMotion = window.matchMedia("(prefers-reduced-motion: reduce)").matches;
  const finePointer = window.matchMedia("(hover: hover) and (pointer: fine)").matches;
  const clamp = (v, a, b) => Math.min(Math.max(v, a), b);
  // ?snap=0.8 — deterministic state for screenshot tooling: reveals forced
  // in, pinned choreography frozen at the given progress
  const SNAP = new URLSearchParams(location.search).get("snap");
  const SNAP_P = SNAP !== null ? clamp(parseFloat(SNAP) || 0, 0, 1) : null;

  /* ---------- preloader + hero title entrance ---------- */
  const loader = document.getElementById("loader");
  const heroTitle = document.getElementById("heroTitle");
  "IRONHOLD".split("").forEach((ch, i) => {
    const s = document.createElement("span");
    s.className = "lt";
    s.textContent = ch;
    s.style.transitionDelay = i * 0.055 + "s";
    s.style.animationDelay = i * 0.09 + "s";
    heroTitle.appendChild(s);
  });
  const t0 = performance.now();
  if (SNAP_P !== null) {
    loader.classList.add("done");
    heroTitle.classList.add("entered");
  } else {
    // gate on fonts, not window.load — CDN scripts must not hold the curtain
    (document.fonts?.ready || Promise.resolve()).then(() => {
      const wait = Math.max(0, 900 - (performance.now() - t0));
      setTimeout(() => {
        loader.classList.add("done");
        requestAnimationFrame(() => heroTitle.classList.add("entered"));
      }, wait);
    });
    // safety: never trap the page behind the loader
    setTimeout(() => { loader.classList.add("done"); heroTitle.classList.add("entered"); }, 4000);
  }

  /* ---------- custom cursor ---------- */
  const dot = document.getElementById("cursorDot");
  const ring = document.getElementById("cursorRing");
  let cx = -100, cy = -100, rx = -100, ry = -100;
  if (finePointer && !reduceMotion) {
    document.body.classList.add("has-cursor");
    window.addEventListener("pointermove", (e) => {
      cx = e.clientX; cy = e.clientY;
      dot.style.left = cx + "px";
      dot.style.top = cy + "px";
      const t = e.target.closest("a, button, .voice-chip, .npc-rail");
      ring.classList.toggle("on-link", !!t);
    }, { passive: true });
  }

  /* ---------- inertial smooth scroll (desktop wheel only) ---------- */
  let target = window.scrollY, current = window.scrollY, lastSet = window.scrollY;
  const maxScroll = () => document.documentElement.scrollHeight - window.innerHeight;
  if (finePointer && !reduceMotion && SNAP_P === null) {
    window.addEventListener("wheel", (e) => {
      if (e.ctrlKey) return; // pinch zoom
      e.preventDefault();
      const mult = e.deltaMode === 1 ? 16 : 1;
      target = clamp(target + e.deltaY * mult, 0, maxScroll());
    }, { passive: false });
  }
  document.addEventListener("click", (e) => {
    const a = e.target.closest('a[href^="#"]');
    if (!a) return;
    const el = document.querySelector(a.getAttribute("href"));
    if (!el) return;
    e.preventDefault();
    target = clamp(el.getBoundingClientRect().top + window.scrollY, 0, maxScroll());
    if (reduceMotion || !finePointer) window.scrollTo(0, target);
  });

  /* ---------- pinned-section progress helpers ---------- */
  const pinHero = document.querySelector(".pin-hero");
  const pinKeep = document.querySelector(".pin-keep");
  const pinGates = document.querySelector(".pin-gates");
  const heroContent = document.getElementById("heroContent");
  const scrollCue = document.querySelector(".scroll-cue");
  const gatePortal = document.getElementById("gatePortal");
  const portalCore = document.querySelector(".portal-core");
  const meterFill = document.getElementById("gateMeterFill");
  const meterPct = document.getElementById("gateMeterPct");

  function pinProgress(wrap) {
    if (SNAP_P !== null) return SNAP_P;
    const r = wrap.getBoundingClientRect();
    const total = wrap.offsetHeight - window.innerHeight;
    return total > 0 ? clamp(-r.top / total, 0, 1) : 0;
  }

  /* ---------- master frame loop ---------- */
  const progressBar = document.getElementById("progressBar");
  const nav = document.getElementById("nav");
  const layers = [...document.querySelectorAll(".hero [data-depth]")];
  const plxEls = [...document.querySelectorAll("[data-plx]")];
  let mouseX = 0, mouseY = 0, curMX = 0, curMY = 0;
  let lastGatePct = -1;

  window.addEventListener("mousemove", (e) => {
    mouseX = e.clientX / window.innerWidth - 0.5;
    mouseY = e.clientY / window.innerHeight - 0.5;
  }, { passive: true });

  function frame() {
    // smooth scroll integration
    if (Math.abs(window.scrollY - lastSet) > 1.5) {
      // external scroll (touch, keyboard, anchor) — adopt it
      current = target = window.scrollY;
    }
    if (Math.abs(target - current) > 0.4) {
      current += (target - current) * 0.11;
      lastSet = Math.round(current);
      window.scrollTo(0, lastSet);
    }
    const sy = window.scrollY;

    const max = maxScroll();
    progressBar.style.width = (max > 0 ? (sy / max) * 100 : 0) + "%";
    nav.classList.toggle("scrolled", sy > 40);

    // cursor ring easing
    if (finePointer && !reduceMotion) {
      rx += (cx - rx) * 0.18; ry += (cy - ry) * 0.18;
      ring.style.left = rx + "px";
      ring.style.top = ry + "px";
    }

    if (!reduceMotion) {
      curMX += (mouseX - curMX) * 0.06;
      curMY += (mouseY - curMY) * 0.06;

      // hero pin: parallax drift + title zoom-fade
      const hp = pinProgress(pinHero);
      if (hp < 1) {
        for (const el of layers) {
          const depth = parseFloat(el.dataset.depth || 0);
          const m = parseFloat(el.dataset.mouse || 0);
          el.style.transform =
            `translate3d(${(-curMX * m).toFixed(2)}px, ${(sy * depth * 0.55 + -curMY * m).toFixed(2)}px, 0)`;
        }
        heroContent.style.opacity = clamp(1 - hp * 1.6, 0, 1);
        heroContent.style.transform =
          `translate3d(${(-curMX * 14).toFixed(2)}px, ${(-curMY * 14).toFixed(2)}px, 0) scale(${(1 + hp * 0.55).toFixed(4)})`;
        scrollCue.style.opacity = clamp(1 - hp * 4, 0, 1);
      }

      // keep pin → castle.js
      window.__keepProgress = pinProgress(pinKeep);

      // gates pin: rings converge, core ignites, meter charges
      const gp = pinProgress(pinGates);
      gatePortal.style.setProperty("--p", gp.toFixed(4));
      portalCore.style.transform =
        `translate(-50%, -50%) scale(${(0.4 + gp * 0.75).toFixed(4)})`;
      portalCore.style.opacity = (0.35 + gp * 0.65).toFixed(3);
      portalCore.style.filter = `blur(2px) brightness(${1 + gp * 1.3})`;
      const pct = Math.round(gp * 100);
      if (pct !== lastGatePct) {
        lastGatePct = pct;
        meterFill.style.width = pct + "%";
        meterPct.textContent = pct >= 99 ? "OPEN" : pct + "%";
      }

      for (const el of plxEls) {
        const r = el.getBoundingClientRect();
        if (r.bottom < 0 || r.top > window.innerHeight) continue;
        const f = parseFloat(el.dataset.plx);
        const off = (r.top + r.height / 2 - window.innerHeight / 2) * f;
        el.style.transform = `translate3d(0, ${off.toFixed(2)}px, 0)`;
      }
    } else {
      window.__keepProgress = pinProgress(pinKeep);
      const gp = pinProgress(pinGates);
      gatePortal.style.setProperty("--p", gp.toFixed(4));
      meterFill.style.width = Math.round(gp * 100) + "%";
      meterPct.textContent = Math.round(gp * 100) + "%";
    }

    requestAnimationFrame(frame);
  }
  requestAnimationFrame(frame);

  // snap+solo mode: show ONLY the named section at scroll 0 — headless
  // Chromium paints nothing but the body background after programmatic
  // scrolls, so screenshot tooling isolates sections instead of scrolling
  if (SNAP_P !== null) {
    const solo = new URLSearchParams(location.search).get("solo");
    const dest = solo && document.getElementById(solo);
    if (dest) {
      for (const el of [...document.body.children]) {
        if (el.tagName === "SCRIPT" || el.id === "loader") continue;
        if (el === dest || el.contains(dest)) continue;
        if (["progressBar", "cursorDot", "cursorRing", "nav"].includes(el.id)) continue;
        el.style.display = "none";
      }
    }
  }

  /* ---------- starfield ---------- */
  function starfield(canvas) {
    const ctx = canvas.getContext("2d");
    let stars = [];
    function resize() {
      canvas.width = canvas.offsetWidth;
      canvas.height = canvas.offsetHeight;
      stars = Array.from({ length: Math.floor(canvas.width / 6) }, () => ({
        x: Math.random() * canvas.width,
        y: Math.random() * canvas.height * 0.85,
        r: Math.random() * 1.4 + 0.3,
        tw: Math.random() * Math.PI * 2,
        sp: 0.008 + Math.random() * 0.02,
      }));
    }
    resize();
    window.addEventListener("resize", resize);
    (function draw() {
      ctx.clearRect(0, 0, canvas.width, canvas.height);
      for (const s of stars) {
        s.tw += s.sp;
        const a = 0.25 + Math.abs(Math.sin(s.tw)) * 0.75;
        ctx.fillStyle = `rgba(232, 226, 240, ${a})`;
        ctx.fillRect(s.x, s.y, s.r, s.r); // square stars: it's Minecraft
      }
      if (!reduceMotion) requestAnimationFrame(draw);
    })();
  }
  starfield(document.getElementById("stars"));

  /* ---------- ember particles ---------- */
  function embers(canvas, count, palette) {
    const ctx = canvas.getContext("2d");
    let parts = [];
    function resize() {
      canvas.width = canvas.offsetWidth;
      canvas.height = canvas.offsetHeight;
    }
    function spawn(p) {
      p.x = Math.random() * canvas.width;
      p.y = canvas.height + Math.random() * 60;
      p.s = Math.random() * 3 + 1.5;
      p.vy = -(0.25 + Math.random() * 0.8);
      p.vx = (Math.random() - 0.5) * 0.35;
      p.life = 0;
      p.max = 280 + Math.random() * 320;
      p.c = palette[(Math.random() * palette.length) | 0];
      p.wob = Math.random() * Math.PI * 2;
      return p;
    }
    resize();
    window.addEventListener("resize", resize);
    parts = Array.from({ length: count }, () => {
      const p = spawn({});
      p.y = Math.random() * canvas.height;
      p.life = Math.random() * p.max;
      return p;
    });
    (function draw() {
      ctx.clearRect(0, 0, canvas.width, canvas.height);
      for (const p of parts) {
        p.life++;
        if (p.life > p.max || p.y < -20) spawn(p);
        p.wob += 0.02;
        p.x += p.vx + Math.sin(p.wob) * 0.3;
        p.y += p.vy;
        const t = p.life / p.max;
        const a = t < 0.15 ? t / 0.15 : 1 - (t - 0.15) / 0.85;
        ctx.globalAlpha = Math.max(a, 0) * 0.9;
        ctx.fillStyle = p.c;
        ctx.fillRect(p.x | 0, p.y | 0, p.s, p.s);
      }
      ctx.globalAlpha = 1;
      if (!reduceMotion) requestAnimationFrame(draw);
    })();
  }
  embers(document.getElementById("embers"), 70, ["#ff7a18", "#ffc83d", "#ff4d1c", "#ffe9b0"]);
  embers(document.getElementById("footerEmbers"), 45, ["#ff7a18", "#ffc83d", "#ffe9b0"]);

  /* ---------- floating voxel cubes ---------- */
  const field = document.getElementById("voxelField");
  const voxColors = ["#6b4ddb", "#ff7a18", "#34d2ff", "#3e3654", "#9b5cff"];
  for (let i = 0; i < 14; i++) {
    const v = document.createElement("div");
    v.className = "voxel";
    const s = 8 + Math.random() * 26;
    v.style.setProperty("--s", s + "px");
    v.style.setProperty("--vc", voxColors[(Math.random() * voxColors.length) | 0]);
    v.style.setProperty("--dur", (7 + Math.random() * 9).toFixed(1) + "s");
    v.style.width = s + "px";
    v.style.height = s + "px";
    v.style.left = Math.random() * 96 + "%";
    v.style.top = 12 + Math.random() * 70 + "%";
    v.style.animationDelay = (-Math.random() * 10).toFixed(1) + "s";
    v.innerHTML = "<i></i><i></i><i></i>";
    field.appendChild(v);
  }

  /* ---------- scroll reveals ---------- */
  const io = new IntersectionObserver((entries) => {
    for (const e of entries) {
      if (e.isIntersecting) {
        e.target.classList.add("in");
        io.unobserve(e.target);
      }
    }
  }, { threshold: 0.15, rootMargin: "0px 0px -40px 0px" });
  document.querySelectorAll(".reveal").forEach((el, i) => {
    el.style.transitionDelay = (i % 4) * 0.08 + "s";
    if (SNAP_P !== null) { el.classList.add("in"); return; }
    io.observe(el);
  });

  /* ---------- stat counters ---------- */
  const cio = new IntersectionObserver((entries) => {
    for (const e of entries) {
      if (!e.isIntersecting) continue;
      cio.unobserve(e.target);
      const el = e.target;
      const goal = parseInt(el.dataset.count, 10);
      const suffix = el.dataset.suffix || "";
      const s0 = performance.now();
      (function tick(t) {
        const k = Math.min((t - s0) / 1600, 1);
        el.textContent = Math.round(goal * (1 - Math.pow(1 - k, 3))) + suffix;
        if (k < 1) requestAnimationFrame(tick);
      })(s0);
    }
  }, { threshold: 0.6 });
  document.querySelectorAll(".stat-num").forEach((c) => cio.observe(c));

  /* ---------- feature card cursor glow + tilt ---------- */
  document.querySelectorAll(".feature-card").forEach((card) => {
    card.style.setProperty("--glow", card.dataset.glow || "#ff7a18");
    card.addEventListener("pointermove", (e) => {
      const r = card.getBoundingClientRect();
      const x = e.clientX - r.left, y = e.clientY - r.top;
      card.style.setProperty("--mx", x + "px");
      card.style.setProperty("--my", y + "px");
      if (!reduceMotion) {
        const tx = ((y / r.height) - 0.5) * -7;
        const ty = ((x / r.width) - 0.5) * 7;
        card.style.transform = `perspective(700px) rotateX(${tx.toFixed(2)}deg) rotateY(${ty.toFixed(2)}deg) translateY(-4px)`;
      }
    });
    card.addEventListener("pointerleave", () => { card.style.transform = ""; });
  });

  /* ---------- magnetic buttons ---------- */
  if (finePointer && !reduceMotion) {
    document.querySelectorAll(".magnetic").forEach((btn) => {
      btn.addEventListener("pointermove", (e) => {
        const r = btn.getBoundingClientRect();
        const dx = (e.clientX - r.left - r.width / 2) / (r.width / 2);
        const dy = (e.clientY - r.top - r.height / 2) / (r.height / 2);
        btn.style.transform = `translate(${(dx * 7).toFixed(1)}px, ${(dy * 6).toFixed(1)}px)`;
      });
      btn.addEventListener("pointerleave", () => { btn.style.transform = ""; });
    });
  }

  /* ============================================================
     PIXEL PORTRAITS — hand-drawn 14×14 busts, one per NPC
     ============================================================ */
  const PORTRAITS = {
    halric: { // steel helm, violet plume, iron stare
      pal: { h: "#8a93a8", H: "#5a6372", p: "#9b5cff", P: "#6b3fb0", s: "#d8a878", S: "#b8865a", e: "#2a2230", a: "#6a7382", A: "#4a5260", m: "#7a4a3a" },
      rows: [
        "......pp......",
        ".....pPp......",
        "....hhhhhh....",
        "...hhhhhhhh...",
        "...hHHHHHHh...",
        "...hsssssss...",
        "...sesssses...",
        "...sssssssS...",
        "....ssmmsS....",
        ".....ssss.....",
        "....aaaaaa....",
        "...aaAaaAaa...",
        "..aaaaaaaaaa..",
        "..aaAaaaaAaa..",
      ],
    },
    kangarude: { // smug kangaroo
      pal: { f: "#c98e5a", F: "#a06a3c", m: "#e8c89a", n: "#3a2418", e: "#1a1416", w: "#f0ead8" },
      rows: [
        "..f........f..",
        ".fFf......fFf.",
        ".fFf......fFf.",
        ".fFf......fFf.",
        "..ffffffffff..",
        ".ffffffffffff.",
        ".ffewffffweff.",
        ".ffffffffffff.",
        "..ffmmmmmmff..",
        "..fmmnnnnmmf..",
        "...mmmmmmmm...",
        "....ffffff....",
        "...ffffffff...",
        "..ffffffffff..",
      ],
    },
    mira: { // red hair, warm, apron
      pal: { r: "#c4502a", R: "#93351a", s: "#e0b088", S: "#c08a60", e: "#3a2a20", m: "#a05a48", d: "#6a3a2a", w: "#e8e0d0" },
      rows: [
        "....rrrrrr....",
        "...rrrrrrrr...",
        "..rrrrrrrrrr..",
        "..rRssssssRr..",
        "..rRssssssRr..",
        "..rsessssesr..",
        "..rsssssssRr..",
        "..Rssmmss.Rr..",
        "...ssssss.....",
        "....ssss......",
        "...dddddd.....",
        "..ddwwwwdd....",
        "..dwwwwwwd....",
        "..dwwwwwwd....",
      ],
    },
    roselind: { // open helm, red crest, blonde
      pal: { a: "#8a93a8", A: "#5a6372", c: "#aa2a3a", y: "#d8b85a", s: "#d8a878", e: "#2a3040", m: "#8a5040" },
      rows: [
        "......cc......",
        ".....cccc.....",
        "....aaaaaa....",
        "...aaaaaaaa...",
        "...aAyyyyAa...",
        "...ayssssya...",
        "...sesssses...",
        "...ssssssss...",
        "....ssmmss....",
        ".....ssss.....",
        "....aaaaaa....",
        "...aaaAAaaa...",
        "..aaaaaaaaaa..",
        "..aAaaaaaaAa..",
      ],
    },
    tobias: { // soot, huge beard, leather
      pal: { s: "#c89068", S: "#a06a48", B: "#4a3424", b: "#5a4030", e: "#1a1416", l: "#5a3f22", L: "#3a2812" },
      rows: [
        "....ssssss....",
        "...ssssssss...",
        "...sSssssSs...",
        "...sessssses..",
        "...ssssssss...",
        "..BbssssssbB..",
        "..BBBbssbBBB..",
        "..BBBBBBBBBB..",
        "...BBBBBBBB...",
        "....BBBBBB....",
        "....llllll....",
        "...llLllLll...",
        "..llllllllll..",
        "..lLllllllLl..",
      ],
    },
    eilan: { // grey scholar, deep blue robe
      pal: { g: "#b8b8c0", G: "#8a8a96", s: "#d0a880", e: "#2a2a3a", v: "#3a4a6a", V: "#283550", m: "#7a4a3a" },
      rows: [
        "....gggggg....",
        "...gggggggg...",
        "...gGssssGg...",
        "...gssssssg...",
        "...sessssesg..",
        "...ssssssss...",
        "...Gssmmss....",
        "...ggssssgg...",
        "....gggggg....",
        ".....gggg.....",
        "....vvvvvv....",
        "...vvVvvVvv...",
        "..vvvvvvvvvv..",
        "..vVvvvvvvVv..",
      ],
    },
    dunstan: { // mining helm + lamp, big beard
      pal: { h: "#7a5a30", H: "#5a4020", L: "#ffd95a", s: "#c89068", e: "#1a1416", B: "#6a4a2a", l: "#4a3a2a" },
      rows: [
        ".....LLLL.....",
        "....hhLLhh....",
        "...hhhhhhhh...",
        "...hHHHHHHh...",
        "...ssssssss...",
        "...sesssses...",
        "...ssssssss...",
        "..BssssssssB..",
        "..BBssssssBB..",
        "...BBBBBBBB...",
        "....BBBBBB....",
        "....llllll....",
        "...llllllll...",
        "..llllllllll..",
      ],
    },
    bram: { // green cap, yellow feather, grin
      pal: { c: "#4a7a3a", C: "#33582a", F: "#e8d85a", h: "#7a5232", s: "#d8a878", e: "#2a2418", m: "#8a5040", v: "#6a4a2a" },
      rows: [
        "........FF....",
        "....ccccFF....",
        "...ccccccc....",
        "..cCCCCCCCc...",
        "...hssssssh...",
        "...sesssses...",
        "...ssssssss...",
        "...ssmmmmss...",
        "....ssssss....",
        ".....ssss.....",
        "....vvvvvv....",
        "...vvvvvvvv...",
        "..vvvCCvvvvv..",
        "..vvvCCvvvvv..",
      ],
    },
    beren: { // bald, long white beard, weathered
      pal: { s: "#c89878", S: "#a07050", W: "#d8d8cc", w: "#b8b8aa", e: "#2a2420", k: "#5a4a3a" },
      rows: [
        "....ssssss....",
        "...ssssssss...",
        "...sSssssSs...",
        "...sessssses..",
        "...sSssssSs...",
        "...ssssssss...",
        "..WwssssssWw..",
        "..WWWwwwwWWW..",
        "...WWWWWWWW...",
        "....WWWWWW....",
        "....kWWWWk....",
        "...kkWWWWkk...",
        "..kkkWWWWkkk..",
        "..kkkkWWkkkk..",
      ],
    },
    hesta: { // green hood, grey wisps, knowing eyes
      pal: { g: "#4a6a3a", G: "#324a28", h: "#b0b0a8", s: "#c89878", e: "#1a2014", m: "#8a5a48" },
      rows: [
        "....gggggg....",
        "...gggggggg...",
        "..gggggggggg..",
        "..gGGGGGGGGg..",
        "..gGhssssshGg.",
        "..gGsssssssGg.",
        "..gGsessse.Gg.",
        "..gGsssssss...",
        "..gG.ssmms....",
        "..gg..ssss....",
        "..ggggggggg...",
        "..gGgggggGgg..",
        ".gggggggggggg.",
        ".gGgggggggGgg.",
      ],
    },
    pippa: { // messy hair, freckles, patched scarf
      pal: { r: "#7a5a32", R: "#5a4022", s: "#e0b088", e: "#2a2418", f: "#b07848", c: "#aa6a2a", C: "#7a4a1a" },
      rows: [
        "...rrrrrrrr...",
        "..rrRrrrrRrr..",
        "..rrssssssrr..",
        "..Rsssssssr...",
        "...sessssess..",
        "...ssssssss...",
        "...sfssssfs...",
        "....ssmmss....",
        ".....ssss.....",
        "....cccccc....",
        "...ccCccCcc...",
        "....cccccc....",
        "....c....c....",
        "..............",
      ],
    },
    wren: { // white wimple, navy habit, gold trim
      pal: { w: "#f0ead8", W: "#d0c8b0", s: "#d8a878", e: "#3a3020", v: "#2a3a5a", V: "#1c2840", t: "#d8b85a", m: "#9a6a50" },
      rows: [
        "....wwwwww....",
        "...wwwwwwww...",
        "..wwwwwwwwww..",
        "..wWssssssWw..",
        "..wWssssssWw..",
        "..wsessssesw..",
        "..wWssssssWw..",
        "..wWssmmssWw..",
        "...wssssssw...",
        "....wwwwww....",
        "....vvvvvv....",
        "...vvtvvtvv...",
        "..vvvvvvvvvv..",
        "..vVvvttvvVv..",
      ],
    },
    watcher: { // hooded void, glowing eyes
      pal: { k: "#2a2f4a", K: "#1a1e33", D: "#0a0c18", E: "#8fa8ff", g: "#4a5580" },
      rows: [
        ".....kkkk.....",
        "....kkkkkk....",
        "...kkKKKKkk...",
        "...kKDDDDKk...",
        "..kkDDDDDDkk..",
        "..kKDEDDEDKk..",
        "..kKDDDDDDKk..",
        "..kkDDDDDDkk..",
        "...kKDDDDKk...",
        "...kkKKKKkk...",
        "....kkkkkk....",
        "...kkgkkgkk...",
        "..kkkkkkkkkk..",
        "..kKkkkkkkKk..",
      ],
    },
    plague: { // beaked bone mask, glass eyes, dark hood
      pal: { k: "#2a2030", K: "#1a1320", M: "#d8d0c0", m: "#b0a890", E: "#d78cff", v: "#3a2a44" },
      rows: [
        "....kkkkkk....",
        "...kkkkkkkk...",
        "..kkKKKKKKkk..",
        "..kKMMMMMMKk..",
        "..kMMEMMEMMk..",
        "..kKMMMMMMKk..",
        "...kMMMMMMk...",
        "....mMMMMm....",
        ".....mMMm.....",
        "......mm......",
        "....vvvvvv....",
        "...vvkvvkvv...",
        "..vvvvvvvvvv..",
        "..vKvvvvvvKv..",
      ],
    },
  };

  document.querySelectorAll(".npc-card").forEach((card) => {
    const def = PORTRAITS[card.dataset.npc];
    const canvas = card.querySelector(".npc-portrait");
    if (!def || !canvas) return;
    const ctx = canvas.getContext("2d");
    def.rows.forEach((row, y) => {
      for (let x = 0; x < row.length; x++) {
        const c = def.pal[row[x]];
        if (!c) continue;
        ctx.fillStyle = c;
        ctx.fillRect(x, y, 1, 1);
      }
    });
  });

  /* ---------- NPC voices: real ElevenLabs lines from the mod ---------- */
  let activeAudio = null, activeChip = null;
  function stopVoice() {
    if (activeAudio) { activeAudio.pause(); activeAudio = null; }
    if (activeChip) { activeChip.classList.remove("playing"); activeChip = null; }
  }
  document.querySelectorAll(".npc-card[data-voice]").forEach((card) => {
    const chip = card.querySelector(".voice-chip");
    const slug = card.dataset.npc;
    // downgrade gracefully if the mp3 hasn't been generated yet
    fetch(`assets/voices/${slug}.mp3`, { method: "HEAD" }).then((r) => {
      if (!r.ok) throw 0;
    }).catch(() => {
      chip.classList.add("casting");
      chip.querySelector(".voice-label").textContent = "VOICE IN CASTING";
      const p = chip.querySelector(".voice-play"); if (p) p.remove();
      const w = chip.querySelector(".wave"); if (w) w.remove();
      card.removeAttribute("data-voice");
    });
    chip.addEventListener("click", (e) => {
      e.stopPropagation();
      if (!card.hasAttribute("data-voice")) return;
      if (activeChip === chip) { stopVoice(); return; }
      stopVoice();
      const audio = new Audio(`assets/voices/${slug}.mp3`);
      activeAudio = audio; activeChip = chip;
      chip.classList.add("playing");
      audio.play().catch(stopVoice);
      audio.addEventListener("ended", stopVoice);
      audio.addEventListener("error", stopVoice);
    });
  });

  /* ---------- draggable NPC rail ---------- */
  const rail = document.getElementById("npcRail");
  let down = false, startX = 0, startScroll = 0, moved = 0;
  rail.addEventListener("pointerdown", (e) => {
    down = true; moved = 0; startX = e.clientX; startScroll = rail.scrollLeft;
    rail.classList.add("dragging");
  });
  rail.addEventListener("pointermove", (e) => {
    if (!down) return;
    const dx = e.clientX - startX;
    moved = Math.max(moved, Math.abs(dx));
    if (moved > 6) {
      rail.setPointerCapture(e.pointerId);
      rail.scrollLeft = startScroll - dx;
    }
  });
  ["pointerup", "pointercancel"].forEach((ev) =>
    rail.addEventListener(ev, () => { down = false; rail.classList.remove("dragging"); })
  );
  rail.addEventListener("click", (e) => {
    if (moved > 6) { e.stopPropagation(); e.preventDefault(); }
  }, true);
})();
