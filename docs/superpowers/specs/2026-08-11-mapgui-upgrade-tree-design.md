# Native Paper Upgrade Tree Design

**Goal:** Provide the player-facing job upgrade-tree inventory through native
Paper UI while preserving legacy/v2 upgrade behavior.

## Scope

- `/jobs upgrade <job>` opens a native Paper inventory screen.
- Legacy `UpgradeTree` and v2 `SkillTree` are rendered.
- Node state, prerequisites, exclusions, costs, effects, level purchases, and major-node confirmation remain service-authoritative.
- Existing `Messages` text and Bukkit sound semantics remain unchanged.
- Existing `/jobs upgrade <job> reset` command behavior remains unchanged.
- The admin tree-editor controls share the same native Paper inventory lifecycle.

## Architecture

`UpgradeTreeGui` is the project-owned opener/screen boundary. The command resolves
the job and opens the native Paper inventory screen through `PaperUiHost`. The
screen owns only transient presentation state and delegates every mutation to
`UpgradeService`.

The graph screen uses inventory slots for edges, node cells, hover state, and hit
testing. Its coordinate system is derived from configured node positions; null-
position nodes remain non-interactive and are not coalesced at `(0,0)`. Graph
panning is bounded and invalidates the screen.

Clicking a node opens a detail view through the native inventory session. Detail
content uses a keyed paginated view. Skill nodes call `purchaseSkillLevel`; major
nodes push a confirmation view and call `purchaseMajor` exactly once after
confirmation. Legacy nodes call `unlock`. Success and failure feedback uses the
existing message/sound matrix.

## Runtime Metadata

- Keep `plugin.yml` and its identity/permissions unchanged.
- Do not declare or download an external UI runtime.
- Native Paper UI classes remain inside the Paper module and its shadow jar.

## Lifecycle and Input

- Inventory close clears transient pending confirmation state.
- Native inventory click handling does not bypass existing service checks.
- Paging and navigation controls remain bounded and preserve the current view.
- Detail back/close returns through the native inventory session.

## Verification

- Unit tests cover graph layout/hit boundaries, null positions, status precedence,
  legacy/v2 node actions, major confirmation/cancellation, feedback mapping,
  panning bounds, and lifecycle cleanup.
- Descriptor tests verify permission preservation and native UI ownership.
- Focused tests run before the full `:paper:test` suite.
- Runtime smoke starts Paper, confirms plugin enablement, and exercises
  `/jobs upgrade miner`.
