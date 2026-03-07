# Launchpad MK2 — Bitwig Controller Extension

A dedicated Bitwig Studio controller extension for the Novation Launchpad MK2.
Provides clip launching, scene launching, and track/scene navigation with full
RGB LED feedback.

## Features

- 8x8 clip launcher grid with color-coded LED feedback
- Scene launch buttons (right side)
- Track and scene bank navigation (top row arrows)
- LED states: playing (green), stopped (dim green), recording (red),
  queued (amber pulse), armed (dim orange)
- Clean LED reset on extension exit

## Button Map

```
 [Up] [Dn] [Lt] [Rt] [Session] [User1] [User2] [Mixer]   <- Top Row (CC 104-111)
  |    |    |    |       |
  |    |    |    |       +-- Lit green (active mode)
  |    |    +----+---------- Scroll track bank left/right
  +----+-------------------- Scroll scene bank up/down

 +----+----+----+----+----+----+----+----+------+
 | 81 | 82 | 83 | 84 | 85 | 86 | 87 | 88 | [89] |  Scene 8
 | 71 | 72 | 73 | 74 | 75 | 76 | 77 | 78 | [79] |  Scene 7
 | 61 | 62 | 63 | 64 | 65 | 66 | 67 | 68 | [69] |  Scene 6
 | 51 | 52 | 53 | 54 | 55 | 56 | 57 | 58 | [59] |  Scene 5
 | 41 | 42 | 43 | 44 | 45 | 46 | 47 | 48 | [49] |  Scene 4
 | 31 | 32 | 33 | 34 | 35 | 36 | 37 | 38 | [39] |  Scene 3
 | 21 | 22 | 23 | 24 | 25 | 26 | 27 | 28 | [29] |  Scene 2
 | 11 | 12 | 13 | 14 | 15 | 16 | 17 | 18 | [19] |  Scene 1
 +----+----+----+----+----+----+----+----+------+
   T1   T2   T3   T4   T5   T6   T7   T8   Scene
                                             Launch

 Grid: Columns = Tracks, Rows = Scenes
 [xx] = Scene launch buttons (launch entire row)
```

## LED Color Reference

| State             | Color       | Velocity |
|-------------------|-------------|----------|
| Empty slot        | Off         | 0        |
| Clip stopped      | Dim green   | 23       |
| Clip playing      | Bright green| 21       |
| Clip recording    | Bright red  | 5        |
| Clip queued       | Amber pulse | 61       |
| Track armed       | Dim orange  | 11       |
| Scene launch      | Dim cyan    | 65       |
| Nav active        | Dim white   | 119      |

## Build

Requires Java 21+ and Gradle.

```sh
./gradlew build
```

Output: `build/libs/LaunchpadMk2.bwextension`

## Install

Copy the `.bwextension` file to your Bitwig Extensions directory:

```sh
./gradlew install
```

Or manually copy to:
- **macOS:** `~/Documents/Bitwig Studio/Extensions/`
- **Windows:** `%USERPROFILE%\Documents\Bitwig Studio\Extensions\`
- **Linux:** `~/Bitwig Studio/Extensions/`

## Setup in Bitwig

1. Build and install the extension
2. Open Bitwig Studio
3. Go to Settings > Controllers
4. Click "Add Controller"
5. Select "Novation" > "Launchpad MK2"
6. Assign MIDI input/output to your Launchpad MK2

The extension should auto-detect if your Launchpad MK2 is connected.

## Usage

- **Grid pads:** Press to launch a clip. Press a playing clip to stop it.
- **Scene launch (right):** Press to launch all clips in that scene row.
- **Arrow buttons (top):** Navigate the visible 8x8 window across your session.
  Lit when scrolling is possible in that direction.

## Requirements

- Bitwig Studio (API version 22+)
- Novation Launchpad MK2
- Java 21+
