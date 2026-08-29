# Task 2 report — native Paper scoreboard and boss-bar surfaces

## TDD evidence

### Initial red run

Command:

```text
./gradlew :modularjobs-paper:test --tests 'dev.mintychochip.gui.PaperSurfacesTest' --no-daemon
```

Observed output:

```text
> Task :modularjobs-paper:compileTestJava FAILED
PaperSurfacesTest.java:127: error: cannot find symbol
  private static Map<String, BossBar> bossBars(PaperSurfaces surfaces) throws Exception {
                                               ^
  symbol:   class PaperSurfaces
...
12 errors
BUILD FAILED
```

The test failed at compilation because the requested `PaperSurfaces` production class did not yet exist.

### Final green run

Command:

```text
./gradlew :modularjobs-paper:test --tests 'dev.mintychochip.gui.PaperSurfacesTest' --no-daemon
```

Observed output:

```text
BUILD SUCCESSFUL in 5s
8 actionable tasks: 3 executed, 5 up-to-date
```

`PaperSurfacesTest` completed all 4 tests successfully.

## Changed files

- `modularjobs-paper/src/main/java/dev/mintychochip/gui/PaperSurfaces.java`
  - Added native UUID-owned scoreboard and boss-bar manager.
  - Uses a fresh sidebar objective, unique invisible entry suffixes, descending scores from 15, stale-entry cleanup, and restoration of the scoreboard captured before the first show.
  - Uses Bukkit `BossBar`, `BarColor`, and solid progress style; clamps invalid progress and isolates bars by audience plus stable key.
  - Ignores offline audiences and clears all native/tracked state through `hideAllBossBars` and `closeAll`.
- `modularjobs-paper/src/test/java/dev/mintychochip/gui/PaperSurfacesTest.java`
  - MockBukkit coverage for scoreboard limits/duplicate entries/replacement/restoration, boss-bar clamping/color mapping/key replacement, and complete cleanup.
- `modularjobs-paper/src/main/java/dev/mintychochip/commands/TextScoreboard.java`
  - Converted the command-facing factory and storage to `PaperSurfaces`; retained line bounds, plain title/line formatting, and `setCurrent(Player)` behavior.
- `modularjobs-paper/src/main/java/dev/mintychochip/payable/ExperienceBarControllerImpl.java`
  - Retained the Adventure formatter, buffering, 50-tick removal, and quit cleanup while passing only native title/progress/color inputs to `PaperSurfaces`.
- `modularjobs-paper/src/main/java/dev/mintychochip/payable/PayableWiring.java`
  - Updated the experience-bar surface constructor boundary.
- `modularjobs-paper/src/main/java/dev/mintychochip/commands/TopCommand.java`
  - Updated the scoreboard command surface constructor boundary.
- `modularjobs-paper/src/main/java/dev/mintychochip/PluginContext.java`
  - Constructs one native surface manager and wires it to the top command and payable wiring; Craftux inventory host remains for screens.

## Commit

Implementation commit: `093d27e` (`Replace scoreboard and boss bar surfaces with Paper APIs`).

## Concerns

- MockBukkit 4.116.1's `ScoreboardMock#getEntries()` does not expose objective score holders, so the focused test inspects the MockBukkit objective score map reflectively to verify native score entries. Production code uses only Bukkit/Paper APIs.
- Paper API 26.2 exposes `Player#showBossBar`/`hideBossBar` for Adventure boss bars, while `Bukkit#createBossBar` returns the native Bukkit boss-bar type. The implementation therefore attaches/removes native bars with `BossBar#addPlayer`/`removePlayer`, preserving the requested native object behavior and lifecycle.
- The pre-existing migration test still asserts the old Craftux scoreboard/boss-bar boundary; it was intentionally not run because the task requires the native replacement and the focused command is the mandated validation.
- No formatters, linters, or project-wide suites were run. The unrelated pre-existing MapGUI build change was not modified or staged.
