/* eslint-disable */
/**
 * Ironhold Bridge — Blockbench plugin (WebSocket client edition).
 *
 * Architecture:
 *   bb-mcp Python server hosts a WebSocket on ws://127.0.0.1:31173
 *   This plugin connects as a CLIENT and receives JSON-RPC commands.
 *   Auto-reconnects if the server isn't running yet or restarts.
 *
 * No Node `require()` calls — everything goes through Blockbench/browser APIs.
 * That avoids the per-plugin module-permission dialogs entirely.
 *
 * Wire protocol:
 *   server → plugin: { "id": <int>, "method": "<name>", "params": {...} }
 *   plugin → server: { "id": <int>, "result": <any> }   |   { "id", "error": "..." }
 *
 * File contents (textures, screenshots) are returned as data URLs; Python writes
 * the bytes to disk so we never need filesystem access from the plugin side.
 */
Plugin.register('ironhold_bridge', {
    title: 'Ironhold Bridge',
    author: 'Ironhold',
    icon: 'cable',
    description: 'WebSocket client that lets bb-mcp drive Blockbench.',
    version: '0.3.0',
    variant: 'desktop',
    min_version: '4.0.0',

    onload() {
        const URL = 'ws://127.0.0.1:31173';
        let ws = null;
        let reconnectTimer = null;
        let manuallyDisabled = false;

        // ---- Helpers -------------------------------------------------------

        function requireProject() {
            if (!Project) throw new Error('no open project — call open_project first');
            return Project;
        }

        function findElement(uuidOrName) {
            if (typeof uuidOrName !== 'string') throw new Error('uuid_or_name must be a string');
            const all = [].concat(Cube.all || [], Group.all || []);
            return all.find(e => e.uuid === uuidOrName)
                || all.find(e => e.name === uuidOrName) || null;
        }

        function findTexture(uuidOrIndex) {
            if (typeof uuidOrIndex === 'number') return Texture.all[uuidOrIndex] || null;
            return Texture.all.find(t => t.uuid === uuidOrIndex)
                || Texture.all.find(t => t.name === uuidOrIndex) || null;
        }

        function serializeCube(c) {
            return {
                type: 'cube', uuid: c.uuid, name: c.name,
                origin: c.from,
                size: [c.to[0] - c.from[0], c.to[1] - c.from[1], c.to[2] - c.from[2]],
                from: c.from, to: c.to,
                pivot: c.origin, rotation: c.rotation,
                uv_offset: c.uv_offset, mirror_uv: !!c.mirror_uv,
                visibility: c.visibility !== false,
                parent: c.parent && c.parent !== 'root' ? c.parent.uuid : null
            };
        }

        function serializeGroup(g) {
            return {
                type: 'group', uuid: g.uuid, name: g.name,
                pivot: g.origin, rotation: g.rotation,
                children: g.children.map(c => c instanceof Cube ? serializeCube(c) : serializeGroup(c)),
                parent: g.parent && g.parent !== 'root' ? g.parent.uuid : null
            };
        }

        // ---- Handlers ------------------------------------------------------

        const handlers = {
            ping() {
                return { ok: true, version: '0.3.0', blockbench: Blockbench.version };
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
                    texture_count: Texture.all.length,
                    selected_uuids: Outliner.selected.map(e => e.uuid)
                };
            },
            open_project({ path: filepath }) {
                // Blockbench.read only reads the bytes; we still have to feed them
                // to a codec so a Project is actually created. Pick the codec by
                // file extension, then call codec.load(model, file, add=false).
                return new Promise((resolve, reject) => {
                    if (!filepath) return reject(new Error('path is required'));
                    Blockbench.read([filepath], { errorbox: false }, (files) => {
                        if (!files.length) return reject(new Error('could not read file'));
                        const file = files[0];
                        let codec = null;
                        if (filepath.endsWith('.bbmodel')) {
                            codec = Codecs.project;
                        } else if (filepath.endsWith('.geo.json')) {
                            codec = Codecs.bedrock || Codecs.bedrock_old;
                        } else if (Codecs.findOnFile) {
                            codec = Codecs.findOnFile(file);
                        }
                        if (!codec) {
                            return reject(new Error(`no codec to load ${filepath}`));
                        }
                        try {
                            const model = (typeof file.content === 'string')
                                ? JSON.parse(file.content)
                                : file.content;
                            codec.load(model, file, false);   // add=false → replace project
                        } catch (e) {
                            return reject(new Error('codec load failed: ' + (e.message || e)));
                        }
                        // Codec.load is synchronous in current Blockbench, but give
                        // it one tick so any deferred init can complete.
                        setTimeout(() => {
                            resolve({
                                opened: filepath,
                                codec: codec.id || codec.name,
                                project_name: Project ? Project.name : null
                            });
                        }, 50);
                    });
                });
            },
            save_project() {
                const p = requireProject();
                if (!p.save_path) throw new Error('no save_path; call save_project_as first');
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
            // Returns the bedrock-compiled string; Python writes it to disk.
            export_geo_text() {
                requireProject();
                const codec = Codecs.bedrock;
                if (!codec) throw new Error('Bedrock codec not loaded');
                return { content: codec.compile() };
            },
            list_outliner() {
                requireProject();
                return Outliner.root.map(n => n instanceof Cube ? serializeCube(n) : serializeGroup(n));
            },
            get_element({ uuid_or_name }) {
                const e = findElement(uuid_or_name);
                if (!e) throw new Error(`element not found: ${uuid_or_name}`);
                return e instanceof Cube ? serializeCube(e) : serializeGroup(e);
            },
            add_cube({ name, parent, origin, size, uv, mirror, rotation, pivot }) {
                requireProject();
                if (!Array.isArray(origin) || origin.length !== 3) throw new Error('origin must be [x,y,z]');
                if (!Array.isArray(size) || size.length !== 3) throw new Error('size must be [w,h,d]');
                const cube = new Cube({
                    name: name || 'cube',
                    from: origin.slice(),
                    to: [origin[0]+size[0], origin[1]+size[1], origin[2]+size[2]],
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
                    const newSize = size || [e.to[0]-e.from[0], e.to[1]-e.from[1], e.to[2]-e.from[2]];
                    e.from = origin.slice();
                    e.to = [origin[0]+newSize[0], origin[1]+newSize[1], origin[2]+newSize[2]];
                } else if (size) {
                    e.to = [e.from[0]+size[0], e.from[1]+size[1], e.from[2]+size[2]];
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
            list_textures() {
                return Texture.all.map((t, i) => ({
                    uuid: t.uuid, index: i, name: t.name,
                    path: t.path || null, width: t.width, height: t.height
                }));
            },
            // Python sends file content as data URL; plugin loads it as a texture.
            load_texture_from_data_url({ data_url, name }) {
                requireProject();
                if (!data_url) throw new Error('data_url is required');
                const tex = new Texture({ name: name || 'texture' }).fromDataURL(data_url).add();
                return { uuid: tex.uuid, name: tex.name, index: Texture.all.indexOf(tex) };
            },
            set_texture_pixel({ texture, x, y, rgba }) {
                const tex = findTexture(texture);
                if (!tex) throw new Error(`texture not found: ${texture}`);
                if (!Array.isArray(rgba) || rgba.length !== 4) throw new Error('rgba must be [r,g,b,a] 0-255');
                tex.edit(canvas => {
                    const ctx = canvas.getContext('2d');
                    const data = ctx.createImageData(1, 1);
                    data.data[0]=rgba[0]; data.data[1]=rgba[1]; data.data[2]=rgba[2]; data.data[3]=rgba[3];
                    ctx.putImageData(data, x, y);
                });
                return { ok: true };
            },
            set_texture_rect({ texture, x, y, width, height, rgba }) {
                const tex = findTexture(texture);
                if (!tex) throw new Error(`texture not found: ${texture}`);
                tex.edit(canvas => {
                    const ctx = canvas.getContext('2d');
                    ctx.fillStyle = `rgba(${rgba[0]},${rgba[1]},${rgba[2]},${(rgba[3]||255)/255})`;
                    ctx.fillRect(x, y, width, height);
                });
                return { ok: true };
            },
            // Returns a data URL; Python writes the file.
            get_texture_data_url({ texture }) {
                const tex = findTexture(texture);
                if (!tex) throw new Error(`texture not found: ${texture}`);
                return { data_url: tex.canvas.toDataURL('image/png') };
            },
            screenshot_data_url({ view, width, height }) {
                requireProject();
                // Pick a preview. Prefer the selected one; fall back to the first
                // available, or the main preview from the active project.
                const preview = (typeof Preview !== 'undefined' && Preview.selected)
                    || (typeof Preview !== 'undefined' && Preview.all && Preview.all[0])
                    || (Project && Project.preview)
                    || null;
                if (!preview || !preview.canvas) {
                    throw new Error('no Blockbench preview canvas available');
                }
                if (view) {
                    // Best-effort camera angle. If the action name doesn't exist
                    // in this Blockbench version, just skip silently.
                    const map = { front: 'angle_north', back: 'angle_south', left: 'angle_west',
                                  right: 'angle_east', top: 'angle_top', bottom: 'angle_bottom' };
                    const action = BarItems[map[view]];
                    if (action && action.click) try { action.click(); } catch (_) {}
                }
                // Force a fresh render before reading the canvas, otherwise the
                // returned data URL can be the previous frame.
                if (typeof preview.render === 'function') {
                    try { preview.render(); } catch (_) {}
                }
                const dataUrl = preview.canvas.toDataURL('image/png');
                return { data_url: dataUrl };
            }
        };

        // ---- WebSocket client ---------------------------------------------

        function connect() {
            if (manuallyDisabled) return;
            try {
                ws = new WebSocket(URL);
            } catch (e) {
                console.warn('[ironhold-bridge] WebSocket constructor threw:', e);
                scheduleReconnect();
                return;
            }
            ws.onopen = () => {
                console.log('[ironhold-bridge] connected to', URL);
                Blockbench.showQuickMessage('Ironhold Bridge connected', 1500);
            };
            ws.onmessage = async (event) => {
                let envelope;
                try {
                    envelope = JSON.parse(event.data);
                } catch (e) {
                    console.error('[ironhold-bridge] bad JSON from server:', e);
                    return;
                }
                const { id, method, params } = envelope;
                const fn = handlers[method];
                let response;
                if (!fn) {
                    response = { id, error: `unknown method: ${method}` };
                } else {
                    try {
                        const result = await Promise.resolve(fn(params || {}));
                        response = { id, result };
                    } catch (e) {
                        console.error('[ironhold-bridge]', method, 'failed:', e);
                        response = { id, error: String((e && e.message) || e) };
                    }
                }
                try {
                    ws.send(JSON.stringify(response));
                } catch (e) {
                    console.error('[ironhold-bridge] send failed:', e);
                }
            };
            ws.onclose = () => {
                ws = null;
                if (!manuallyDisabled) {
                    console.log('[ironhold-bridge] disconnected, retrying in 3s');
                    scheduleReconnect();
                }
            };
            ws.onerror = (e) => {
                console.warn('[ironhold-bridge] socket error (will reconnect):', e.message || e);
                // onclose will fire after this — don't double-schedule.
            };
        }

        function scheduleReconnect() {
            if (reconnectTimer || manuallyDisabled) return;
            reconnectTimer = setTimeout(() => {
                reconnectTimer = null;
                connect();
            }, 3000);
        }

        function disconnect() {
            manuallyDisabled = true;
            if (reconnectTimer) { clearTimeout(reconnectTimer); reconnectTimer = null; }
            if (ws) { ws.close(); ws = null; }
        }

        function reenable() {
            manuallyDisabled = false;
            connect();
        }

        // Stash handles for onunload + the toggle action.
        this._connect = reenable;
        this._disconnect = disconnect;
        this._isConnected = () => !!ws && ws.readyState === 1;

        this._toggleAction = new Action('ironhold_bridge_toggle', {
            name: 'Ironhold Bridge: Toggle',
            description: `Connect/disconnect to bb-mcp at ${URL}`,
            icon: 'cable',
            category: 'tools',
            click: () => {
                if (this._isConnected() || reconnectTimer) {
                    disconnect();
                    Blockbench.showQuickMessage('Ironhold Bridge disconnected', 2000);
                } else {
                    reenable();
                    Blockbench.showQuickMessage('Ironhold Bridge: connecting…', 1500);
                }
            }
        });
        MenuBar.addAction(this._toggleAction, 'tools');

        // Auto-connect on load. The Python side may not be running yet; we'll
        // retry every 3 seconds until it is.
        connect();
    },

    onunload() {
        if (this._disconnect) this._disconnect();
        if (this._toggleAction) this._toggleAction.delete();
    }
});
