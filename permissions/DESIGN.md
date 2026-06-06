# A Better General-Purpose Permissions Manager (a LuckPerms replacement)

Working notes for a permission system that beats LuckPerms *as a general,
cross-platform replacement* — i.e. it must manage permissions for plugins/mods
it does not own, which is the constraint that shapes everything below.

(An Ironhold-internal-only variant is simpler — see §6 — but the interesting
problem is the general one.)

---

## 1. How LuckPerms works (and where it hurts)

- **Node** — the one atomic unit. *Everything* is a node: a permission
  (`essentials.fly`), group membership (`group.admin`), a prefix
  (`prefix.100.&cAdmin`), meta, temp state. A node has a value (true/false →
  grant/negate), an optional **context**, and an optional **expiry**.
- **Group** — a named bag of nodes; users join via `group.<name>`.
- **Inheritance** — groups inherit groups; ties broken by **weight** (a number;
  higher wins, also orders prefix/suffix).
- **Track** — ordered group list for `promote`/`demote`.
- **Context** — free key→value pairs (world, server, gamemode, dimension, custom).
- **Storage** — flatfile or SQL/Mongo, plus a *separate* messaging service
  (Redis/RabbitMQ/plugin-messaging) for multi-server sync. Heavy in-memory
  caching of resolved maps per context-set.

### The pain points a replacement must fix

1. **Stringly-typed everything** — no authoritative list of what permissions
   exist; typos silently no-op; no descriptions.
2. **Wildcards are a lie** — `essentials.*` only works if a plugin pre-registers
   children; LuckPerms can't enumerate an unknown namespace. #1 support topic.
3. **Resolution is opaque** — value × weight × inheritance × context × negation.
   "Why does this player have/lack X" needs a separate `verbose`/`check` mode.
4. **Weight is an implicit tiebreak** operators must keep in their heads.
5. **Reverse queries are awkward** — generic one-row-per-node store makes
   "who can do X?" and auditing afterthoughts.
6. **Sync needs a sidecar** — storage + messaging are two things that can drift.

LuckPerms is great *because* the node abstraction is uniform and string-based —
that's what gives it universal reach across plugins it's never heard of. But the
same uniformity is what makes it opaque and footgun-prone for the operator.

---

## 2. Thesis

> LuckPerms is a **write-only store of stringly-typed nodes you reason about
> blind.** The better system is **observed + reactive + explainable + queryable**:
> it learns the real permission universe by watching checks, recomputes
> reactively, explains every decision against real nodes, and lets you
> diff/preview before committing — all without requiring plugin cooperation.

---

## 3. The better ideas

### Idea 1 — Observe, don't require (the keystone)

Don't ask plugins to register their nodes. **Intercept the platform's permission
check pipeline** and learn the schema by watching. Every check yields: the node
string, the default the plugin requested, the calling plugin, and the live
context.

- Produces a **live, accurate catalog** of every permission actually used on
  *this* server, auto-grouped by namespace. Discoverability with zero plugin
  cooperation, reflecting reality instead of stale docs.
- **Wildcards finally become sound:** `essentials.*` expands against the observed
  catalog, and the UI can show "currently matches these 47 nodes" *before* apply.

This single move kills pain points #1 and #2 while staying 100% general.

> Platform note: on **Bukkit** strings dominate and there's no enforced registry,
> so observation is the win. On **NeoForge**, permissions are *already* registered
> (`PermissionGatherEvent`/`PermissionNode`) so you get the catalog for free — the
> typed model is native there. On **Sponge** you implement the permission service,
> so you have full visibility by definition. The catalog is the unifying
> abstraction across all three.

### Idea 2 — Explain-by-construction (and explain the *actual* denial)

The resolver is a pure function that always returns a decision *plus* provenance —
so `/perm why <player> <perm>` uses the exact same code path as a live check, not
a parallel "verbose" reimplementation. And because checks are intercepted, an
operator can click a player who just got "no permission" and see the *actual*
node that was checked and why it resolved false — no guessing the node name first.

### Idea 3 — Explicit precedence, not weight

Keep groups/inheritance, but resolve by a **documented top-to-bottom scan over an
explicit rank order**, with three-state values (GRANT / DENY / UNSET) where a DENY
stops the scan. Predictable by reading; no weight arithmetic. Per-player overrides
beat any role. Tracks become "move the player up/down the ladder."

### Idea 4 — Reactive, not cache-invalidated

LuckPerms caches resolved maps and occasionally needs `/lp sync`. Instead, model
effective permissions as a **reactive computed value over a dependency graph**:
change a group → dependents recompute → online players' effective sets update
immediately and verifiably. No manual sync, no staleness class of bugs.

### Idea 5 — A real queryable store

Treat the store as a proper indexed database, not a generic node bag. "Who can
`worldedit.*` in world `creative`?" is one query. Reverse lookups and audits are
first-class instead of bolt-ons.

### Idea 6 — Event-sourced: one mechanism for three jobs

An append-only change log (`Granted`, `Revoked`, `RoleAssigned`,
`TempGranted{expires}`, with actor + timestamp). Current state is a cached fold.
This delivers:
- **Audit for free** — "who opped Steve, when, from where."
- **Undo / time-travel.**
- **Sync without a sidecar** — the log *is* the replication channel; other
  servers tail it. No separately-configured Redis that drifts from storage.

### Idea 7 — Safety rails LuckPerms lacks

- **Dry-run diff:** "this change grants 12 nodes, revokes 3 — confirm?" Possible
  because you have the catalog + resolver.
- **Lint:** warn when a wildcard catches a dangerous node (`*` over `op`,
  `luckperms.*`), when a grant is shadowed and never takes effect, on orphan groups.
- **Optional typed registration:** a cooperating plugin *may* register typed perms
  for compile-time safety as *enrichment* — never a requirement. (Best of both.)

---

## 4. Feasibility — is this actually possible?

Yes. The riskiest-sounding part is already proven; the rest is ordinary work.

| Piece | Verdict | Evidence |
|---|---|---|
| Intercept every permission check | **Proven** | LuckPerms already replaces each Bukkit `Permissible`; NeoForge requires registration; Sponge = you are the service |
| Build a catalog from observation | Feasible | Bukkit also exposes `PluginManager.getPermissions()`; NeoForge nodes are enumerable |
| Sound wildcards | Feasible | Falls out of the catalog |
| Explain w/ provenance | Feasible | Pure resolver returning a trace |
| Reactive recompute | Feasible | LuckPerms recomputes on change today; this is a cleaner impl |
| Event-sourced store + log-as-sync | Standard pattern | LuckPerms already has a messaging layer; we unify it with storage |
| Queryable store, dry-run, lint | Trivial | Given catalog + resolver |

**Real costs (stated plainly):** per-platform injection is the unglamorous bulk
of the work; "observe every check" is on a hot path → sample/cache; and you're up
against LuckPerms' familiarity moat. None fatal. The genuinely new parts (catalog
UX, sound wildcards, explain-the-actual-denial, preview-before-commit) are a
*presentation layer on plumbing already known to work*.

---

## 5. Data model sketch

```
Catalog     { node: string, defaultValue, observedFrom: plugin, lastSeen }   // learned
Group/Role  { id, rank: int, rules: List<Rule> }                             // data
Rule        { node | wildcard, state: GRANT|DENY, context: ContextSet }
Assignment  { player: UUID, role, context?, expires? }
Override    { player: UUID, node, state, context?, expires? }                // beats roles
Event log   { append-only: Granted/Revoked/Assigned/... + actor + timestamp }
```

Resolution (pure, returns Decision + Provenance):
1. Player overrides matching context → highest-rank source wins, return.
2. Roles by descending rank; first GRANT/DENY whose rule + context match wins.
3. Else platform default (Bukkit registered default / NeoForge node default).
4. Record every step tried.

---

## 6. Scope note: the Ironhold-internal variant

For Ironhold's *own* permissions (NeoForge, single Folium server) you don't need
the observation machinery — NeoForge's registry gives you the typed catalog
natively. The internal slice is just: a typed `ModPermissions` registry +
explainable resolver + fallback to vanilla `PermissionSet`/`PermissionLevel`.
That ships value immediately; the general-replacement layers (observation,
reactive graph, event log, multi-platform) build on top without rework.

---

## 7b. Key finding — MC 1.26 changed the platform (and helps us)

Investigating the patched 1.26 jar + NeoForge 26.1 turned up two facts that reshape the plan:

1. **NeoForge 26.1 dropped its permission API entirely.** There is no `PermissionAPI` /
   `PermissionHandler` / `PermissionNode` / `PermissionGatherEvent` anymore — zero `permission`
   classes in the NeoForge jar. Mods on 1.26 must use the *vanilla* system.
2. **Vanilla 1.26 is now itself a typed, registry/codec permission framework.** A `Permission` is a
   registry type (`PermissionTypes`, codecs) with built-in subtypes `Permission.Atom(Identifier)` and
   `Permission.HasCommandLevel(PermissionLevel)`. Every source resolves checks through a single
   `PermissionSet` (composable via `union`/`PermissionSetUnion`), supplied per-source by
   `PermissionSetSupplier`. A player's set originates at `MinecraftServer.getProfilePermissions()` and
   is returned by `ServerPlayer.permissions()`.

Consequences:
- The two-surface worry (vanilla commands vs. modded perms) **doesn't exist on 1.26** — both go
  through the one `PermissionSet`. Owning a player's set = full control over both.
- "Observe-don't-require" is largely unnecessary here: vanilla permissions are already typed
  `Identifier`-keyed atoms, so the catalog is enumerable by design. The Bukkit-only nature of that
  idea (noted in §3/Idea 1) is confirmed.
- **The server-manager hook is a single Mixin**: wrap `ServerPlayer.permissions()` to return an
  overlay `PermissionSet` ([IronholdPermissionSet](../src/main/java/kingdom/smp/perms/IronholdPermissionSet.java))
  that resolves `Permission.Atom` nodes through the store and otherwise defers to the vanilla base.
  Because it keys on `Identifier`, it manages **any mod's** permissions, not just Ironhold's.

## 8. Implementation status

Built and compiling (`mod_version` 1.63.0):
- Typed registry, explainable resolver, store, `/perm` commands — §6 internal slice.
- **Server-wide overlay** — `IronholdPermissionSet` + `ServerPlayerPermissionsMixin` make every
  player's `PermissionSet` resolve through the store first, defer to vanilla on UNSET. This is the
  "full server manager" mechanism for NeoForge 1.26.
- Not yet runtime-tested on a live server (Mixin application + overlay behavior need an in-game check).

Still missing for parity with LuckPerms: temporary grants/expiry, contexts (world/dimension),
prefixes/meta, multi-server sync, an event-sourced audit log, a GUI, and a LuckPerms import path.

## 9. Open questions / next steps

- **Platforms:** general cross-platform replacement, or NeoForge-first then expand?
- **Persistence:** SQLite/SQL event log vs flatfile. Event log fits multi-server.
- **GUI:** in-game role/catalog editor (project already does custom GUIs) vs commands.
- **Migration:** import from a LuckPerms export so people can switch in place.
- **Recommended first slice:** the Ironhold-internal variant (§6) to validate the
  resolver + explain UX, then add Idea 1 (observation) to go general.
