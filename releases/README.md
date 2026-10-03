# Versioned mod builds

Run `./gradlew packageRelease` (Windows: `.\gradlew.bat packageRelease`) from the project root to build all three loaders, run the shared unit tests, and collect the installable jars here.

The folder name comes from `version` in `gradle.properties`. There is one folder per mod version, and each folder holds three jars, one per loader. With alpha.1 to alpha.3 delivered, packaging 0.1.0-alpha.4 gives this layout:

```text
releases/
  README.md
  0.1.0-alpha.1/
    technologia-fabric-1.21.1-0.1.0-alpha.1.jar
    technologia-forge-1.21.1-0.1.0-alpha.1.jar
    technologia-neoforge-1.21.1-0.1.0-alpha.1.jar
  0.1.0-alpha.2/
    technologia-fabric-1.21.1-0.1.0-alpha.2.jar
    technologia-forge-1.21.1-0.1.0-alpha.2.jar
    technologia-neoforge-1.21.1-0.1.0-alpha.2.jar
  0.1.0-alpha.3/
    technologia-fabric-1.21.1-0.1.0-alpha.3.jar
    technologia-forge-1.21.1-0.1.0-alpha.3.jar
    technologia-neoforge-1.21.1-0.1.0-alpha.3.jar
  0.1.0-alpha.4/
    technologia-fabric-1.21.1-0.1.0-alpha.4.jar
    technologia-forge-1.21.1-0.1.0-alpha.4.jar
    technologia-neoforge-1.21.1-0.1.0-alpha.4.jar
```

Each filename identifies the loader, Minecraft version and mod version: `technologia-<loader>-<minecraft version>-<mod version>.jar`. Install only the jar matching your loader, on the client and on the server, with Minecraft 1.21.1 and Java 21. The 0.1.0-alpha.4 jars need Fabric Loader 0.16.9 or newer with Fabric API 0.109.0+1.21.1 or newer (Team Reborn Energy is bundled), NeoForge 21.1.80 or newer, or Forge 52.0.28 or newer. What each version contains and how to upgrade is in `docs/releases/<mod version>.md`.

Change the mod version before packaging a new release. Older version folders are preserved, including when cleaning the modules' build directories. Packaging refuses to replace an existing jar with different bytes; an identical repeat is allowed. Each GitHub release and its source tag are retained. This folder contains the runtime jars only, without source or Javadoc archives. Generated jars are local build outputs and are excluded from Git.

Archives are built without file timestamps and with a stable entry order, so the same source gives the same jar. Packaging checks each jar before collecting it: the jar must contain the loader's mod metadata, `technologia/tiers.json`, the project licence, the two template licences and `THIRD_PARTY_NOTICES.txt`, and it must not contain test classes or test structures.

Packaging runs the builds and the shared unit tests. It does not replace server GameTests, client playtesting or compatibility checks; consult `docs/VALIDATION.md` for tested behavior.

## GitHub Releases

Public downloads are also published under [GitHub Releases](https://github.com/Thespiki/Technologia/releases).

For each new mod version, update `gradle.properties`, write `docs/releases/<version>.md`, run `packageRelease`, and commit the source and notes. Push an annotated tag named `v<version>` at that commit. The **Publish mod release** workflow verifies the tag/version, builds the three jars, runs resource checks, unit tests and the server tests (the full GameTest suite on NeoForge, loader smoke tests on Fabric and Forge), then publishes the release with those three assets. Versions such as `0.1.0-alpha.4` are marked as prereleases. Existing releases are not silently overwritten.

The `releases/<version>/` files are the local collection; GitHub release assets are built from the exact tagged commit on the runner. Both identify their Minecraft version and loader in their filenames.
