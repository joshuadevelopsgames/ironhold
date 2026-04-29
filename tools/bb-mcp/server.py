"""bb-mcp — MCP server that exposes Blockbench operations to Claude.

Architecture:
    Claude Code  ──MCP/stdio──▶  this server  ──HTTP──▶  Blockbench plugin (ironhold_bridge.js)

The Blockbench plugin must be installed and the bridge must be running
(toggleable from File → Tools → Ironhold Bridge: Toggle).

Every tool here is a thin wrapper around a JSON-RPC POST to the plugin.
The plugin runs inside Blockbench (Electron), so it has full access to
the editor's API: Project, Cube, Group, Texture, Screencam, etc.

Run:
    python3 server.py        # stdio transport, talks to Claude Code

Add to Claude Code's MCP config (.claude/settings.local.json):
    {
      "mcpServers": {
        "blockbench": {
          "command": "python3",
          "args": ["/abs/path/to/tools/bb-mcp/server.py"]
        }
      }
    }
"""
from __future__ import annotations

import base64
import json
import os
import sys
from typing import Any

import httpx
from mcp.server.fastmcp import FastMCP

BB_HOST = os.environ.get("BB_BRIDGE_HOST", "127.0.0.1")
BB_PORT = int(os.environ.get("BB_BRIDGE_PORT", "31173"))
BB_URL = f"http://{BB_HOST}:{BB_PORT}/rpc"

# Single shared client — keep-alive helps when we call several ops in a row.
_client = httpx.Client(timeout=httpx.Timeout(30.0, connect=2.0))

mcp = FastMCP(
    "blockbench",
    instructions=(
        "Drive Blockbench remotely. Requires the Ironhold Bridge plugin to be "
        "installed in Blockbench and the bridge to be running (File → Tools → "
        "Ironhold Bridge: Toggle, or auto-starts on Blockbench launch). "
        "Always call `bb_status` first to confirm Blockbench is reachable."
    ),
)


def _rpc(method: str, params: dict[str, Any] | None = None) -> Any:
    try:
        r = _client.post(BB_URL, json={"method": method, "params": params or {}})
    except httpx.ConnectError as e:
        raise RuntimeError(
            f"Cannot reach Blockbench bridge at {BB_URL}. Start Blockbench and "
            f"ensure the Ironhold Bridge plugin is installed and enabled. ({e})"
        ) from e
    if r.status_code >= 400:
        try:
            err = r.json().get("error", r.text)
        except Exception:
            err = r.text
        raise RuntimeError(f"Blockbench RPC error ({method}): {err}")
    return r.json().get("result")


# ---------- Connectivity ----------------------------------------------------


@mcp.tool()
def bb_ping() -> dict:
    """Check that the Blockbench bridge is up. Returns plugin + Blockbench versions."""
    return _rpc("ping")


@mcp.tool()
def bb_status() -> dict:
    """Get the current Blockbench session state: open project, dirty flag, counts of cubes/groups/textures, current selection."""
    return _rpc("status")


# ---------- Project ---------------------------------------------------------


@mcp.tool()
def bb_open_project(path: str) -> dict:
    """Open a .bbmodel or .geo.json file in Blockbench. `path` must be absolute."""
    return _rpc("open_project", {"path": path})


@mcp.tool()
def bb_save_project() -> dict:
    """Save the current Blockbench project to its existing save_path (must already be set)."""
    return _rpc("save_project")


@mcp.tool()
def bb_save_project_as(path: str) -> dict:
    """Save the current project to a specific path (sets save_path then saves)."""
    return _rpc("save_project_as", {"path": path})


@mcp.tool()
def bb_export_geo_json(path: str) -> dict:
    """Compile the current project to a Bedrock .geo.json file at `path`."""
    return _rpc("export_geo_json", {"path": path})


# ---------- Outliner / cubes / groups --------------------------------------


@mcp.tool()
def bb_list_outliner() -> list:
    """Return the full outliner tree (cubes + groups, nested)."""
    return _rpc("list_outliner")


@mcp.tool()
def bb_get_element(uuid_or_name: str) -> dict:
    """Read a single cube or group by uuid (preferred) or name."""
    return _rpc("get_element", {"uuid_or_name": uuid_or_name})


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
    """Add a cube to the current project.

    Args:
        name: Display name for the cube.
        origin: World [x, y, z] of the cube's lowest corner.
        size: [w, h, d] in pixels (Bedrock-style coords; entity floor at y=0).
        uv: [u, v] top-left of the cube's atlas unwrap (default [0, 0]).
        parent: uuid or name of a parent group; omit to attach at root.
        mirror: mirror the UV unwrap.
        rotation: [x, y, z] in degrees (default 0,0,0).
        pivot: rotation pivot in world coords (default = origin).

    Returns the new cube serialized (uuid, name, origin, size, uv, etc.).
    """
    params = {"name": name, "origin": origin, "size": size}
    if uv is not None: params["uv"] = uv
    if parent is not None: params["parent"] = parent
    if mirror: params["mirror"] = True
    if rotation is not None: params["rotation"] = rotation
    if pivot is not None: params["pivot"] = pivot
    return _rpc("add_cube", params)


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
    """Modify any subset of a cube's fields. Pass only the fields you want to change."""
    params: dict[str, Any] = {"uuid_or_name": uuid_or_name}
    for k, v in [("origin", origin), ("size", size), ("uv", uv), ("mirror", mirror),
                 ("rotation", rotation), ("pivot", pivot), ("name", name)]:
        if v is not None:
            params[k] = v
    return _rpc("update_cube", params)


@mcp.tool()
def bb_delete_element(uuid_or_name: str) -> dict:
    """Delete a cube or group by uuid (preferred) or name."""
    return _rpc("delete_element", {"uuid_or_name": uuid_or_name})


@mcp.tool()
def bb_add_group(
    name: str,
    pivot: list[float] | None = None,
    rotation: list[float] | None = None,
    parent: str | None = None,
) -> dict:
    """Add a group (bone) to the current project. Children attach via their `parent` field."""
    params: dict[str, Any] = {"name": name}
    if pivot is not None: params["pivot"] = pivot
    if rotation is not None: params["rotation"] = rotation
    if parent is not None: params["parent"] = parent
    return _rpc("add_group", params)


# ---------- Textures --------------------------------------------------------


@mcp.tool()
def bb_list_textures() -> list:
    """List all textures in the current project (uuid, index, name, path, w, h)."""
    return _rpc("list_textures")


@mcp.tool()
def bb_load_texture_from_file(path: str, name: str | None = None) -> dict:
    """Add a new texture to the project from a PNG on disk."""
    params = {"path": path}
    if name: params["name"] = name
    return _rpc("load_texture_from_file", params)


@mcp.tool()
def bb_set_texture_pixel(texture: str, x: int, y: int, rgba: list[int]) -> dict:
    """Paint a single pixel at (x, y) on the given texture (uuid or name).

    `rgba` is [r, g, b, a] with each component 0..255.
    """
    return _rpc("set_texture_pixel", {"texture": texture, "x": x, "y": y, "rgba": rgba})


@mcp.tool()
def bb_set_texture_rect(
    texture: str,
    x: int,
    y: int,
    width: int,
    height: int,
    rgba: list[int],
) -> dict:
    """Fill a rectangular region of the texture with a solid color."""
    return _rpc("set_texture_rect", {
        "texture": texture, "x": x, "y": y,
        "width": width, "height": height, "rgba": rgba,
    })


@mcp.tool()
def bb_save_texture(texture: str, path: str) -> dict:
    """Export a texture to a PNG file at the given absolute path."""
    return _rpc("save_texture", {"texture": texture, "path": path})


# ---------- Visual verification --------------------------------------------


@mcp.tool()
def bb_screenshot(
    path: str,
    view: str | None = None,
    width: int = 512,
    height: int = 512,
) -> dict:
    """Take a screenshot of the current Blockbench preview to a PNG.

    Args:
        path: Absolute output path.
        view: One of 'front', 'back', 'left', 'right', 'top', 'bottom'. Omit to keep current view.
        width / height: Output image size in pixels.
    """
    params: dict[str, Any] = {"path": path, "width": width, "height": height}
    if view: params["view"] = view
    return _rpc("screenshot", params)


# ---------- main ------------------------------------------------------------


if __name__ == "__main__":
    mcp.run()
