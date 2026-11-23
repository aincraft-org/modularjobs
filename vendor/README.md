# Vendored sources

Libraries copied in so a clean clone builds from public repositories only.
Do not edit these sources in place; replace them from upstream and update
this file.

| Directory | Upstream | Revision | License |
|---|---|---|---|
| `databag-common`, `databag-api`, `databag-paper` | https://github.com/mintychochip/databag | snapshot `0.0.0-20260831.032230-5` (sources jars; repo HEAD then `bc2941be542be611295d98d0dade642f3b2d2460` plus local work) | MIT (`databag-LICENSE`) |
| `utilities-event` (packages `org.aincraft.api.event`, `org.aincraft.event` only) | aincraft-org Utilities (`utilities-api` + `utilities-common`) | release `2026.08.27` (sources jars; repo HEAD `68cabcbd7939e27cc6513b1c9542da293743adf3`) | same owner as ModularJobs; upstream ships no license file |

The root build applies its quality gates (Error Prone, Checkstyle, PMD,
SpotBugs, Spotless) to first-party modules only, not to these.
