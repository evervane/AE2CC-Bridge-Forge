# AE2CC Bridge Forge - Build Guide

## Requirements

- JDK 17
- Internet connection (first build downloads dependencies)

## Setup

1. Download dependency jars and place in `ae2cc-forge/libs/`:
   - [Applied Energistics 2](https://www.curseforge.com/minecraft/mc-mods/applied-energistics-2/files) — `appliedenergistics2-forge-15.4.10.jar`
   - [CC:Tweaked](https://www.curseforge.com/minecraft/mc-mods/cc-tweaked/files/5379173) — `cc-tweaked-1.20.1-forge-1.120.2.jar`

2. Build:
   ```bash
   cd ae2cc-forge
   ./gradlew build
   ```

3. Output: `build/libs/ae2cc-forge-1.0.0.jar`

## Installation

1. Copy `ae2cc-forge-1.0.0.jar` to Minecraft `mods/` folder
2. Ensure dependencies are installed:
   - Forge 1.20.1 (47.x)
   - Applied Energistics 2 (15.x)
   - CC:Tweaked (1.120.x)
3. Launch game
