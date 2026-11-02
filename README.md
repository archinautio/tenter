<img src="docs/site/assets/tenter-logo.svg" alt="Tenter logo: terminal panels forming a T" width="128" height="128">

# Tenter

A Kotlin/JVM terminal-UI toolkit built on Mordant.

- [User documentation](https://archinautio.github.io/tenter/) — installation and usage guides.
- [Documentation source](docs/site/index.md) — read the guides in this checkout.
- [Architecture](docs/architecture.md) — module boundaries and implementation invariants.
- [Contributor and agent instructions](CLAUDE.md) — development commands and working conventions.
- [Documentation maintenance](docs/documentation.md) — preview, validation, and publishing.

## Try the showcase

The example application shows every Tenter widget, one page each, with keys to change them
live. From this checkout, build it and run it in a real terminal:

```sh
./gradlew :tenter-example:installDist
tenter-example/build/install/tenter-example/bin/tenter-example
```

Press Tab to move between panels, `[` and `]` to change pages, and `q` to quit. See
[Run the showcase](docs/site/examples.md#run-the-showcase) for every page, its keys, and the
`--scheme` option.

## License

[Apache License, Version 2.0](LICENSE).
