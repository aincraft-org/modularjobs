# Payables & economy — Living Spec

> Status: active  
> Last updated: 2026-08-31
> Owners: ModularJobs maintainers

## Intent

Job tasks award typed **payables** (experience, money, …). Money deposits go
through an **EconomyProvider** bridge so the payment pipeline stays independent
of a specific ledger implementation. Servers without an economy provider remain
usable by default; operators who require real currency can select fail-fast
startup behavior.

## Boundaries

### In scope

- `EconomyProvider` contract and factory selection
- Typed Mint2 ledger bridge and Vault economy fallback
- `BlackholeEconomyProvider` fallback for missing providers
- Payable wiring and experience bar UX helpers
- `economy.required` compatibility and `economy.missing-provider` policy
- Optional Bukkit soft-depends on Mint2 and Vault

### Out of scope / non-goals

- Implementing a full economy/ledger inside ModularJobs
- Treating a missing provider as a payment-time exception by default
- Durable payout identifiers in the current payment pipeline
- Coupling messages or chat theming to an economy provider

## Invariants

- Provider APIs are compile-only dependencies of `modularjobs-paper`; provider
  types never enter the pure API module or shaded plugin output.
- Mint2 is selected whenever its plugin is enabled and resolves its Bukkit
  service lazily per deposit because registration may complete asynchronously.
- Vault is selected only when Mint2 is absent and Vault has registered an economy.
- When `economy.missing-provider: blackhole` is selected, positive currency
  payables return success without changing a balance; invalid amounts return
  false.
- When `economy.missing-provider: fail` is selected, missing Mint2 and Vault
  providers fail plugin wiring with an actionable configuration message.
- `economy.required: true` maps to `fail` only when no explicit
  `economy.missing-provider` value is present.
- A failed or unknown deposit outcome must not trigger an automatic retry through
  the other provider.
- Currency/account namespaces for ModularJobs are explicit
  (`modularjobs:…`); do not invent ad-hoc strings in random call sites.

## Implementation guidance

- Factory: `EconomyProviderFactory` — prefer Mint2, then Vault, then apply the
  configured missing-provider policy.
- Package: `dev.mintychochip.payable`; wire via `PayableWiring`.
- Tests: unit-test factory policy and fallback behavior; use MockBukkit/service
  registration where a live Bukkit service is needed.
- Keep the payment pipeline calling `EconomyProvider.deposit` only — no direct
  ledger calls outside the provider.
- Keep blackhole logging at provider selection, never once per reward.

### Explicit do-nots

- Do not require Mint2 service registration during factory selection; that races startup.
- Do not treat deposit failures as XP rollback unless atomic multi-currency
  payouts become an explicit product decision.
- Do not add a provider-specific dependency to the pure `api` module.

## Current

- [x] `EconomyProvider` abstraction + factory
- [x] Typed Mint2 adapter with lazy service resolution
- [x] Vault adapter and deterministic Mint2-first selection
- [x] Default blackhole fallback and explicit fail policy
- [x] Local preferences service has no external Preferences dependency
- [x] Factory, fallback, Mint2, and Vault adapter tests
- [x] Per-player XP boss bar color preference (external Preferences plugin, green fallback)

### Current notes

Mint2 and Vault deposit failures return false and are logged by their adapters.
Both deposit into the selected provider's default currency. The payment pipeline
does not retry through the other provider after an unknown outcome.
The XP boss bar color is a per-player preference registered with the external
Preferences plugin when present; without it the bar stays default green.

## Next

- [ ] Add a durable payout identifier if true at-most-once payment semantics
  become a requirement.

## Future

- [ ] Durable payout / idempotency keys from the payment pipeline
- [ ] Multi-currency payables if content needs more than `modularjobs:coin`

## Decisions log

| Date | Decision | Why |
|------|----------|-----|
| 2026-08-31 | Prefer Mint2, then fall back to Vault | Keep Mint2 authoritative while supporting standard server economies |
| 2026-08-31 | Provider adapters use the provider default currency | Vault exposes one economy and reward metadata stays provider-neutral |
| 2026-08-10 | Blackhole is the default missing-provider policy | Experience-only/development servers start safely |
| 2026-08-10 | Explicit `fail` policy remains available | Currency-required servers need startup safety |

