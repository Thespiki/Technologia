# Versioned mod builds

Run `./gradlew packageRelease` (Windows: `.\gradlew.bat packageRelease`) from the project root to build all three loaders, run the shared unit tests, and collect the installable jars here.

The folder name comes from `version` in `gradle.properties`:

```text
releases/
  0.1.0-alpha.1/
    technologia-fabric-1.21.1-0.1.0-alpha.1.jar
    technologia-neoforge-1.21.1-0.1.0-alpha.1.jar
    technologia-forge-1.21.1-0.1.0-alpha.1.jar
```

Each filename identifies the loader, Minecraft version and mod version. Install only the jar matching your loader. Fabric also requires Fabric API; Team Reborn Energy is bundled.

Change the mod version before packaging a new release. Older version folders are preserved, including when cleaning the modules' build directories. Packaging the same version again refreshes its jars. This folder contains the runtime jars only, without source or Javadoc archives. Generated jars are local build outputs and are excluded from Git.

Packaging confirms the builds and shared unit tests pass. It does not replace server GameTests, client playtesting or compatibility checks; consult `docs/VALIDATION.md` for tested behavior.

## GitHub Releases

Public downloads are also published under [GitHub Releases](https://github.com/Thespiki/Technologia/releases).

For each new mod version, update `gradle.properties`, write `docs/releases/<version>.md`, run `packageRelease`, and commit the source and notes. Push an annotated tag named `v<version>` at that commit. The **Publish mod release** workflow verifies the tag/version, builds the three jars, runs resource checks, unit tests and NeoForge server GameTests, then publishes the release with those three assets. Versions such as `0.1.0-alpha.1` are marked as prereleases. Existing releases are not silently overwritten.

The `releases/<version>/` files are the local collection; GitHub release assets are built from the exact tagged commit on the runner. Both identify their Minecraft version and loader in their filenames.
