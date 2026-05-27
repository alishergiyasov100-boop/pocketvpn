# PocketVPN

Free, no ads, no servers. A monochrome Android VPN client built on top of Tor.

## What it is

A native Kotlin/Compose app that uses an embedded Tor process as its tunnel.
Server-free by design — there is no PocketVPN backend to pay for, and there is
no PocketVPN account.

- **Tor** as the underlying network (3-hop, encrypted, free)
- **Conflux** multi-path circuits enabled by default (~2× the median throughput)
- **Country pinning** via `ExitNodes` (DE, NL, SE, ~50 more)
- **Snowflake bridge mode** as an opt-in: your phone helps users behind firewalls
- Monochrome UI, side drawer, large center power button, animated transitions

## Why not commercial VPN?

Because nobody is selling you anything, and nobody is logging anything either.
The trade-off: typical Tor speed is 1–5 Mbps baseline. With Conflux + a pinned
fast-Europe exit it climbs to roughly 5–10 Mbps — enough for 720p video,
messaging, browsing. Not enough for 4K streaming.

## v0.1.0

This is the UI/skeleton release. The `TorEngine` is a stub that simulates
bootstrap timing so the state machine and animations are wired end-to-end.
v0.2.0 will bundle `tor-android-binary` + `hev-socks5-tunnel` and turn the
on/off button into a real circuit.

## Build

GitHub Actions builds debug + release APKs on every push to `main`. Locally:

```
./gradlew assembleDebug
```
