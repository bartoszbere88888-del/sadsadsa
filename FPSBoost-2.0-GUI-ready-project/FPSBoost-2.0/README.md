# FPSBoost 2.0 — Fabric client project

Target: Minecraft Java **1.21.4**, Fabric Loader, Java 21. Sodium is suggested but this project does not inject into or modify Sodium internals.

## Current features
- Press **O** in game to open the FPSBoost settings screen.
- Dark themed GUI with FPSBoost 2.0 title.
- ON/OFF toggle. When off, the mod restores the Minecraft particle option to `All`; when on, it applies the selected particle mode.
- Cycle particle mode: Minimalne / Zmniejszone / Wszystkie.
- Save settings to `.minecraft/config/fpsboost.json` (also auto-saves after changes).
- Discord button for `https://dc.gg/fpsbost.pl`.
- GitHub Actions workflow builds the JAR and uploads it as an artifact.

## Important limitations
This is a modest client-side optimization: it controls particle density. It does **not** yet contain custom mixins or broad rendering optimizations, and no FPS gain is guaranteed. Sodium compatibility is not verified by a runtime test. This project has not been compiled or launched in Minecraft in the current environment, so treat the first GitHub build and in-game test as necessary validation.

## Build with GitHub Actions
1. Create a repository on GitHub.
2. Upload the **contents** of this folder to the repository root, including the hidden `.github` folder.
3. Open **Actions** and select **Build FPSBoost**. Pushes should also trigger the workflow.
4. Open the successful run and download the `FPSBoost-jar` artifact. Inside `build/libs`, choose the main JAR, not `-sources.jar`.
5. Test on a separate Minecraft 1.21.4 Fabric profile first.

## Local build
Use JDK 21 and Gradle 8.12 or compatible. Run `gradle build` from this project folder. Output is in `build/libs/` if the build succeeds.
