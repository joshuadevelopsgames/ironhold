/* eslint-disable */
/**
 * Ironhold Bridge — a Blockbench plugin that exposes the editor over a
 * localhost HTTP server, so an external process (the bb-mcp Python server →
 * Claude Code) can drive Blockbench programmatically.
 *
 * Install:
 *   1. Copy this file (or symlink) into Blockbench's plugin directory:
 *        macOS:   ~/Library/Application Support/Blockbench/plugins/
 *        Linux:   ~/.config/Blockbench/plugins/
 *        Windows: %AppData%\Blockbench\plugins\
 *   2. Restart Blockbench. The bridge auto-starts on launch and listens on
 *      127.0.0.1:31173. Toggle from File → Tools → Ironhold Bridge.
 *
 * Wire protocol (POST /rpc):
 *   request:  { "method": "<name>", "params": { ... } }
 *   response: { "result": <any> }   |   { "error": "<message>" }
 */
(function () {
    'use strict';

    const PORT = 31173;
    const HOST = '127.0.0.1';

    // Lazy-load Node modules. Blockbench is Electron, so `require` works.
    const http = require('http');
    const fs = require('fs');
    const path = require('path');

    let server = null;

    // --- Helpers ------------------------------------------------------------

    function requireProject() {
        if (!Project) throw new Error('no open project — call open_project first');
        return Project;
    }

    function findElement(uuidOrName) {
        // Cubes and Groups are both keyed by uuid in Blockbench's outliner registry.
        if (typeof uuidOrName !== 'string') {
            throw new Error('uuid_or_name must be a string');
        }
        const all = [].concat(Cube.all || [], Group.all || []);
        return all.find(e => e.uuid === uuidOrName)
            || all.find(e => e.name === uuidOrName)
            || null;
    }

    function findTexture(uuidOrIndex) {
        if (typeof uuidOrIndex === 'number') {
            return textures[uuidOrIndex] || null;
        }
        return textures.find(t => t.uuid === uuidOrIndex)
            || textures.find(t => t.name === uuidOrIndex)
            || null;
    }

    function serializeCube(c) {
        return {
            type: 'cube',
            uuid: c.uuid,
            name: c.name,
            origin: c.from,
            size: [c.to[0] - c.from[0], c.to[1] - c.from[1], c.to[2] - c.from[2]],
            from: c.from,
            to: c.to,
            pivot: c.origin,
            rotation: c.rotation,
            uv_offset: c.uv_offset,
            mirror_uv: !!c.mirror_uv,
            visibility: c.visibility !== false,
            parent: c.parent && c.parent !== 'root' ? c.parent.uuid : null
        };
    }

    function serializeGroup(g) {
        return {
            type: 'group',
            uuid: g.uuid,
            name: g.name,
            pivot: g.origin,
            rotation: g.rotation,
            children: g.children.map(child => child instanceof Cube ? serializeCube(child) : serializeGroup(child)),
            parent: g.parent && g.parent !== 'root' ? g.parent.uuid : null
        };
    }

    // --- Handlers -----------------------------------------------------------

    const handlers = {

        // Connectivity / inspection -----------------------------------------

        ping() {
            return { ok: true, version: '0.1.0', blockbench: Blockbench.version };
        },

        status() {
            const p = Project;
            return {
                project_open: !!p,
                project_path: p ? p.save_path || null : null,
                project_name: p ? p.name : null,
                dirty: p ? !p.saved : false,
                cube_count: Cube.all.length,
                group_count: Group.all.length,
                texture_count: textures.length,
                selected_uuids: Outliner.selected.map(e => e.uuid)
            };
        },

        // Project ops --------------------------------------------------------

        open_project({ path: filepath }) {
            return new Promise((resolve, reject) => {
                if (!filepath) return reject(new Error('path is required'));
                if (!fs.existsSync(filepath)) {
                    return reject(new Error(`file not found: ${filepath}`));
                }
                Blockbench.read([filepath], { errorbox: false }, (files) => {
                    if (!files.length) return reject(new Error('Blockbench could not read file'));
                    resolve({ opened: filepath, project_name: Project ? Project.name : null });
                });
            });
        },

        save_project() {
            const p = requireProject();
            if (!p.save_path) {
                throw new Error('project has no save_path; call save_project_as first');
            }
            BarItems.save_project.click();
            return { saved: true, path: p.save_path };
        },

        save_project_as({ path: filepath }) {
            const p = requireProject();
            if (!filepath) throw new Error('path is required');
            p.save_path = filepath;
            p.export_path = filepath;
            BarItems.save_project.click();
            return { saved: true, path: filepath };
        },

        export_geo_json({ path: filepath }) {
            requireProject();
            if (!filepath) throw new Error('path is required');
            // Blockbench writes the geo via the Bedrock entity codec.
            const codec = Codecs.bedrock;
            if (!codec) throw new Error('Bedrock codec not loaded');
            const content = codec.compile();
            fs.writeFileSync(filepath, content);
            return { saved: true, path: filepath };
        },

        // Outliner / cube / group ops ----------------------------------------

        list_outliner() {
            requireProject();
            return Outliner.root.map(node =>
                node instanceof Cube ? serializeCube(node) : serializeGroup(node)
            );
        },

        get_element({ uuid_or_name }) {
            const e = findElement(uuid_or_name);
            if (!e) throw new Error(`element not found: ${uuid_or_name}`);
            return e instanceof Cube ? serializeCube(e) : serializeGroup(e);
        },

        add_cube({ name, parent, origin, size, uv, mirror, rotation, pivot }) {
            requireProject();
            if (!Array.isArray(origin) || origin.length !== 3) {
                throw new Error('origin must be [x,y,z]');
            }
            if (!Array.isArray(size) || size.length !== 3) {
                throw new Error('size must be [w,h,d]');
            }
            const cube = new Cube({
                name: name || 'cube',
                from: origin.slice(),
                to: [origin[0] + size[0], origin[1] + size[1], origin[2] + size[2]],
                uv_offset: uv || [0, 0],
                mirror_uv: !!mirror,
                rotation: rotation || [0, 0, 0],
                origin: pivot || origin.slice()
            }).init();
            if (parent) {
                const parentEl = findElement(parent);
                if (parentEl) cube.addTo(parentEl);
            }
            Canvas.updateAll();
            return serializeCube(cube);
        },

        update_cube({ uuid_or_name, origin, size, uv, mirror, rotation, pivot, name }) {
            const e = findElement(uuid_or_name);
            if (!e || !(e instanceof Cube)) throw new Error(`cube not found: ${uuid_or_name}`);
            if (origin) {
                const newSize = size || [e.to[0] - e.from[0], e.to[1] - e.from[1], e.to[2] - e.from[2]];
                e.from = origin.slice();
                e.to = [origin[0] + newSize[0], origin[1] + newSize[1], origin[2] + newSize[2]];
            } else if (size) {
                e.to = [e.from[0] + size[0], e.from[1] + size[1], e.from[2] + size[2]];
            }
            if (uv) e.uv_offset = uv.slice();
            if (typeof mirror === 'boolean') e.mirror_uv = mirror;
            if (rotation) e.rotation = rotation.slice();
            if (pivot) e.origin = pivot.slice();
            if (name) e.name = name;
            Canvas.updateView({ elements: [e], element_aspects: { transform: true, geometry: true, uv: true } });
            return serializeCube(e);
        },

        delete_element({ uuid_or_name }) {
            const e = findElement(uuid_or_name);
            if (!e) throw new Error(`element not found: ${uuid_or_name}`);
            e.remove();
            Canvas.updateAll();
            return { deleted: true, uuid: e.uuid };
        },

        add_group({ name, parent, pivot, rotation }) {
            requireProject();
            const g = new Group({
                name: name || 'bone',
                origin: pivot || [0, 0, 0],
                rotation: rotation || [0, 0, 0]
            }).init();
            if (parent) {
                const parentEl = findElement(parent);
                if (parentEl) g.addTo(parentEl);
            }
            Canvas.updateAll();
            return serializeGroup(g);
        },

        // Texture ops --------------------------------------------------------

        list_textures() {
            return textures.map((t, i) => ({
                uuid: t.uuid,
                index: i,
                name: t.name,
                path: t.path || null,
                width: t.width,
                height: t.height
            }));
        },

        load_texture_from_file({ path: filepath, name }) {
            requireProject();
            if (!filepath) throw new Error('path is required');
            const tex = new Texture({ name: name || path.basename(filepath) })
                .fromPath(filepath)
                .add();
            return { uuid: tex.uuid, name: tex.name, index: textures.indexOf(tex) };
        },

        set_texture_pixel({ texture, x, y, rgba }) {
            const tex = findTexture(texture);
            if (!tex) throw new Error(`texture not found: ${texture}`);
            if (!Array.isArray(rgba) || rgba.length !== 4) throw new Error('rgba must be [r,g,b,a] 0-255');
            tex.edit(canvas => {
                const ctx = canvas.getContext('2d');
                const data = ctx.createImageData(1, 1);
                data.data[0] = rgba[0]; data.data[1] = rgba[1]; data.data[2] = rgba[2]; data.data[3] = rgba[3];
                ctx.putImageData(data, x, y);
            });
            return { ok: true };
        },

        set_texture_rect({ texture, x, y, width, height, rgba }) {
            const tex = findTexture(texture);
            if (!tex) throw new Error(`texture not found: ${texture}`);
            tex.edit(canvas => {
                const ctx = canvas.getContext('2d');
                ctx.fillStyle = `rgba(${rgba[0]},${rgba[1]},${rgba[2]},${(rgba[3] || 255) / 255})`;
                ctx.fillRect(x, y, width, height);
            });
            return { ok: true };
        },

        save_texture({ texture, path: filepath }) {
            const tex = findTexture(texture);
            if (!tex) throw new Error(`texture not found: ${texture}`);
            if (!filepath) throw new Error('path is required');
            const dataUrl = tex.canvas.toDataURL('image/png');
            const buf = Buffer.from(dataUrl.split(',')[1], 'base64');
            fs.writeFileSync(filepath, buf);
            return { saved: true, path: filepath };
        },

        // Visual verification ------------------------------------------------

        screenshot({ path: filepath, view, width, height }) {
            return new Promise((resolve, reject) => {
                requireProject();
                if (!filepath) return reject(new Error('path is required'));
                // Snap the active preview camera to the requested view first.
                if (view && Preview.selected) {
                    const map = { front: 'angle_north', back: 'angle_south', left: 'angle_west',
                                  right: 'angle_east', top: 'angle_top', bottom: 'angle_bottom' };
                    const action = BarItems[map[view]];
                    if (action) action.click();
                }
                Screencam.screenshotPreview({
                    width: width || 512,
                    height: height || 512,
                    show_dialog: false,
                    crop: false,
                    callback: (dataUrl) => {
                        try {
                            const buf = Buffer.from(dataUrl.split(',')[1], 'base64');
                            fs.writeFileSync(filepath, buf);
                            resolve({ saved: true, path: filepath });
                        } catch (e) {
                            reject(e);
                        }
                    }
                });
            });
        }
    };

    // --- HTTP server --------------------------------------------------------

    function startBridge() {
        if (server) return;
        server = http.createServer((req, res) => {
            res.setHeader('Access-Control-Allow-Origin', 'http://127.0.0.1');
            if (req.method !== 'POST' || req.url !== '/rpc') {
                res.writeHead(404, { 'Content-Type': 'application/json' });
                res.end(JSON.stringify({ error: 'POST /rpc only' }));
                return;
            }
            let body = '';
            req.on('data', chunk => { body += chunk; });
            req.on('end', async () => {
                let envelope;
                try {
                    envelope = JSON.parse(body);
                } catch (e) {
                    res.writeHead(400); res.end(JSON.stringify({ error: 'invalid JSON' })); return;
                }
                const { method, params } = envelope;
                const fn = handlers[method];
                if (!fn) {
                    res.writeHead(404);
                    res.end(JSON.stringify({ error: `unknown method: ${method}` }));
                    return;
                }
                try {
                    const result = await Promise.resolve(fn(params || {}));
                    res.writeHead(200, { 'Content-Type': 'application/json' });
                    res.end(JSON.stringify({ result }));
                } catch (e) {
                    console.error('[ironhold-bridge]', method, e);
                    res.writeHead(500, { 'Content-Type': 'application/json' });
                    res.end(JSON.stringify({ error: String((e && e.message) || e) }));
                }
            });
        });
        server.on('error', (err) => {
            console.error('[ironhold-bridge] server error:', err);
            Blockbench.showQuickMessage(`Ironhold Bridge: ${err.code || err.message}`, 4000);
            server = null;
        });
        server.listen(PORT, HOST, () => {
            console.log(`[ironhold-bridge] listening on http://${HOST}:${PORT}/rpc`);
        });
    }

    function stopBridge() {
        if (!server) return;
        server.close();
        server = null;
        console.log('[ironhold-bridge] stopped');
    }

    // --- Plugin registration ------------------------------------------------

    let toggleAction;

    Plugin.register('ironhold_bridge', {
        title: 'Ironhold Bridge',
        author: 'Ironhold',
        icon: 'cable',
        description: 'Localhost HTTP bridge for AI-driven Blockbench editing.',
        version: '0.1.0',
        variant: 'desktop',
        min_version: '4.0.0',
        onload() {
            toggleAction = new Action('ironhold_bridge_toggle', {
                name: 'Ironhold Bridge: Toggle',
                description: `Start/stop the localhost bridge on ${HOST}:${PORT}`,
                icon: 'cable',
                category: 'tools',
                click() {
                    if (server) {
                        stopBridge();
                        Blockbench.showQuickMessage('Ironhold Bridge stopped', 2000);
                    } else {
                        startBridge();
                        Blockbench.showQuickMessage(`Ironhold Bridge running on ${HOST}:${PORT}`, 2000);
                    }
                }
            });
            MenuBar.addAction(toggleAction, 'tools');
            startBridge();
        },
        onunload() {
            stopBridge();
            if (toggleAction) toggleAction.delete();
        }
    });
})();
