"""bb-mcp — MCP server that drives Blockbench via a WebSocket.

Architecture (v0.3+):
    Claude Code  ──MCP/stdio──▶  this server  ──WebSocket──▶  Blockbench plugin
                                  (hosts ws://127.0.0.1:31173)  (connects as client)

The plugin is a pure-browser WebSocket client (no Node `require()`), so there
are no Blockbench permission dialogs. The plugin auto-connects when it loads
and auto-reconnects if this server restarts.

Run:
    python3 server.py        # stdio transport, talks to Claude Code

Tools that produce file output (save_texture, screenshot, export_geo_json) ask
the plugin to return a data URL or text string and write the bytes to disk on
the Python side, so the plugin never needs filesystem permissions either.
"""
from __future__ import annotations

import asyncio
import base64
import json
import os
import sys
import threading
from concurrent.futures import Future
from typing import Any

import websockets
from mcp.server.fastmcp import FastMCP

HOST = os.environ.get("BB_BRIDGE_HOST", "127.0.0.1")
PORT = int(os.environ.get("BB_BRIDGE_PORT", "31173"))


# ---------- Bridge state shared between WS thread and MCP tools -------------

class Bridge:
    def __init__(self) -> None:
        self.client: Any = None       # active websockets server connection
        self.pending: dict[int, asyncio.Future] = {}
        self.next_id = 0
        self.loop: asyncio.AbstractEventLoop | None = None

    def reset_id(self) -> int:
        self.next_id += 1
        return self.next_id


bridge = Bridge()


async def _ws_handler(websocket):
    bridge.client = websocket
    print(f"[bb-mcp] Blockbench connected: {websocket.remote_address}", file=sys.stderr)
    try:
        async for message in websocket:
            try:
                data = json.loads(message)
            except Exception as e:
                print(f"[bb-mcp] bad message: {e}", file=sys.stderr)
                continue
            req_id = data.get("id")
            future = bridge.pending.pop(req_id, None)
            if future is None or future.done():
                continue
            if "error" in data:
                future.set_exception(RuntimeError(data["error"]))
            else:
                future.set_result(data.get("result"))
    finally:
        bridge.client = None
        # Cancel any in-flight calls that won't get answered.
        for fut in list(bridge.pending.values()):
            if not fut.done():
                fut.set_exception(RuntimeError("Blockbench disconnected mid-call"))
        bridge.pending.clear()
        print("[bb-mcp] Blockbench disconnected", file=sys.stderr)


async def _ws_call(method: str, params: dict[str, Any] | None, timeout: float):
    if bridge.client is None:
        raise RuntimeError(
            f"Blockbench is not connected. Open Blockbench with the Ironhold Bridge "
            f"plugin loaded — it auto-connects to ws://{HOST}:{PORT}."
        )
    req_id = bridge.reset_id()
    fut = asyncio.get_running_loop().create_future()
    bridge.pending[req_id] = fut
    try:
        await bridge.client.send(json.dumps({"id": req_id, "method": method, "params": params or {}}))
        return await asyncio.wait_for(fut, timeout=timeout)
    except asyncio.TimeoutError:
        bridge.pending.pop(req_id, None)
        raise RuntimeError(f"Blockbench did not respond to {method} within {timeout}s")
    except Exception:
        bridge.pending.pop(req_id, None)
        raise


def _bridge_loop_target():
    """Run the WebSocket server forever in a dedicated event loop / thread."""
    loop = asyncio.new_event_loop()
    asyncio.set_event_loop(loop)
    bridge.loop = loop

    async def main():
        async with websockets.serve(_ws_handler, HOST, PORT):
            print(f"[bb-mcp] WebSocket listening on ws://{HOST}:{PORT}", file=sys.stderr)
            await asyncio.Future()  # run forever

    try:
        loop.run_until_complete(main())
    except Exception as e:
        print(f"[bb-mcp] bridge thread crashed: {e}", file=sys.stderr)


_bridge_thread = threading.Thread(target=_bridge_loop_target, daemon=True, name="bb-mcp-ws")
_bridge_thread.start()


def call_bb(method: str, params: dict[str, Any] | None = None, timeout: float = 30.0):
    """Sync facade for MCP tools. Submits the call into the bridge thread's loop."""
    if bridge.loop is None:
        # Wait briefly for the loop to spin up on first call.
        for _ in range(50):
            if bridge.loop is not None:
                break
            threading.Event().wait(0.05)
    if bridge.loop is None:
        raise RuntimeError("bb-mcp WebSocket loop didn't start")
    fut: Future = asyncio.run_coroutine_threadsafe(
        _ws_call(method, params, timeout),
        bridge.loop,
    )
    return fut.result(timeout=timeout + 5)


# ---------- MCP server ------------------------------------------------------

mcp = FastMCP(
    "blockbench",
    instructions=(
        "Drive Blockbench remotely via the Ironhold Bridge WebSocket plugin. "
        "Always call `bb_ping` first to confirm Blockbench is connected. "
        "Coordinates use Bedrock convention (Y up, entity feet at y=0)."
    ),
)


# ---------- Connectivity ----------------------------------------------------


@mcp.tool()
def bb_ping() -> dict:
    """Check that Blockbench is connected. Returns plugin + Blockbench versions."""
    return call_bb("ping", timeout=5.0)


@mcp.tool()
def bb_status() -> dict:
    """Current Blockbench session: open project, dirty flag, counts of cubes/groups/textures, current selection."""
    return call_bb("status", timeout=5.0)


# ---------- Project ---------------------------------------------------------


@mcp.tool()
def bb_open_project(path: str) -> dict:
    """Open a .bbmodel or .geo.json file in Blockbench. `path` must be absolute."""
    return call_bb("open_project", {"path": path})


@mcp.tool()
def bb_save_project() -> dict:
    """Save the current project to its existing save_path (must already be set)."""
    return call_bb("save_project")


@mcp.tool()
def bb_save_project_as(path: str) -> dict:
    """Save the current project to a specific path."""
    return call_bb("save_project_as", {"path": path})


@mcp.tool()
def bb_export_geo_json(path: str) -> dict:
    """Compile the current project to a Bedrock .geo.json file at `path`."""
    result = call_bb("export_geo_text")
    text = result.get("content", "")
    os.makedirs(os.path.dirname(path) or ".", exist_ok=True)
    with open(path, "w") as f:
        f.write(text)
    return {"saved": True, "path": path, "bytes": len(text)}


# ---------- Outliner / cubes / groups --------------------------------------


@mcp.tool()
def bb_list_outliner() -> list:
    """Return the full outliner tree (cubes + groups, nested)."""
    return call_bb("list_outliner")


@mcp.tool()
def bb_get_element(uuid_or_name: str) -> dict:
    """Read a single cube or group by uuid (preferred) or name."""
    return call_bb("get_element", {"uuid_or_name": uuid_or_name})


@mcp.tool()
def bb_add_cube(
    name: str,
    origin: list[float],
    size: list[float],
    uv: list[int] | None = None,
    parent: str | None = None,
    mirror: bool = False,
    rotation: list[float] | None = None,
    pivot: list[float] | None = None,
) -> dict:
    """Add a cube to the current project. Coords in Bedrock convention (Y up, feet at y=0)."""
    params: dict[str, Any] = {"name": name, "origin": origin, "size": size}
    if uv is not None: params["uv"] = uv
    if parent is not None: params["parent"] = parent
    if mirror: params["mirror"] = True
    if rotation is not None: params["rotation"] = rotation
    if pivot is not None: params["pivot"] = pivot
    return call_bb("add_cube", params)


@mcp.tool()
def bb_update_cube(
    uuid_or_name: str,
    origin: list[float] | None = None,
    size: list[float] | None = None,
    uv: list[int] | None = None,
    mirror: bool | None = None,
    rotation: list[float] | None = None,
    pivot: list[float] | None = None,
    name: str | None = None,
) -> dict:
    """Modify any subset of a cube's fields."""
    params: dict[str, Any] = {"uuid_or_name": uuid_or_name}
    for k, v in [("origin", origin), ("size", size), ("uv", uv), ("mirror", mirror),
                 ("rotation", rotation), ("pivot", pivot), ("name", name)]:
        if v is not None:
            params[k] = v
    return call_bb("update_cube", params)


@mcp.tool()
def bb_delete_element(uuid_or_name: str) -> dict:
    """Delete a cube or group by uuid (preferred) or name."""
    return call_bb("delete_element", {"uuid_or_name": uuid_or_name})


@mcp.tool()
def bb_add_group(
    name: str,
    pivot: list[float] | None = None,
    rotation: list[float] | None = None,
    parent: str | None = None,
) -> dict:
    """Add a group (bone) to the current project."""
    params: dict[str, Any] = {"name": name}
    if pivot is not None: params["pivot"] = pivot
    if rotation is not None: params["rotation"] = rotation
    if parent is not None: params["parent"] = parent
    return call_bb("add_group", params)


# ---------- Textures --------------------------------------------------------


@mcp.tool()
def bb_list_textures() -> list:
    """List all textures in the current project."""
    return call_bb("list_textures")


@mcp.tool()
def bb_load_texture_from_file(path: str, name: str | None = None) -> dict:
    """Load a PNG from disk into the current project. Server reads the file and sends the bytes."""
    if not os.path.isfile(path):
        raise RuntimeError(f"file not found: {path}")
    with open(path, "rb") as f:
        b64 = base64.b64encode(f.read()).decode()
    data_url = f"data:image/png;base64,{b64}"
    params = {"data_url": data_url}
    if name: params["name"] = name
    return call_bb("load_texture_from_data_url", params)


@mcp.tool()
def bb_set_texture_pixel(texture: str, x: int, y: int, rgba: list[int]) -> dict:
    """Paint a single pixel at (x, y). `rgba` is [r, g, b, a] each 0..255."""
    return call_bb("set_texture_pixel", {"texture": texture, "x": x, "y": y, "rgba": rgba})


@mcp.tool()
def bb_set_texture_rect(
    texture: str, x: int, y: int, width: int, height: int, rgba: list[int],
) -> dict:
    """Fill a rectangular region of the texture with a solid color."""
    return call_bb("set_texture_rect", {
        "texture": texture, "x": x, "y": y,
        "width": width, "height": height, "rgba": rgba,
    })


@mcp.tool()
def bb_save_texture(texture: str, path: str) -> dict:
    """Export a texture to a PNG file at the given absolute path."""
    result = call_bb("get_texture_data_url", {"texture": texture})
    data_url = result["data_url"]
    b64 = data_url.split(",", 1)[1] if "," in data_url else data_url
    raw = base64.b64decode(b64)
    os.makedirs(os.path.dirname(path) or ".", exist_ok=True)
    with open(path, "wb") as f:
        f.write(raw)
    return {"saved": True, "path": path, "bytes": len(raw)}


# ---------- Visual verification --------------------------------------------


@mcp.tool()
def bb_screenshot(
    path: str, view: str | None = None, width: int = 512, height: int = 512,
) -> dict:
    """Take a screenshot of the current Blockbench preview to a PNG.

    `view` is one of front/back/left/right/top/bottom; omit to keep current view.
    """
    params: dict[str, Any] = {"width": width, "height": height}
    if view: params["view"] = view
    result = call_bb("screenshot_data_url", params, timeout=15.0)
    data_url = result["data_url"]
    b64 = data_url.split(",", 1)[1] if "," in data_url else data_url
    raw = base64.b64decode(b64)
    os.makedirs(os.path.dirname(path) or ".", exist_ok=True)
    with open(path, "wb") as f:
        f.write(raw)
    return {"saved": True, "path": path, "bytes": len(raw)}


# ---------- main ------------------------------------------------------------


if __name__ == "__main__":
    mcp.run()
