package com.gregross.bitwig.launchpadmk2;

public class LaunchpadMk2Colors
{
   // Special state velocity colors
   public static final int OFF = 0;
   public static final int GRID_OUTLINE = 1;       // dark grey — visible grid boundary
   public static final int CLIP_RECORDING = 5;     // bright red
   public static final int CLIP_QUEUED = 61;       // amber/yellow
   public static final int TRACK_ARMED = 11;       // dim orange
   public static final int SCENE_LAUNCH = 65;      // dim cyan

   // Top row button colors
   public static final int NAV_ACTIVE = 119;       // dim white
   public static final int NAV_INACTIVE = 0;       // off
   public static final int MODE_ACTIVE = 21;       // bright green
   public static final int MODE_INACTIVE = 0;      // off

   // MIDI note mapping: grid pad note = (row + 1) * 10 + (col + 1)
   public static int gridNote(int row, int col)
   {
      return (row + 1) * 10 + (col + 1);
   }

   // Scene launch buttons: notes 19, 29, 39, ..., 89
   public static int sceneLaunchNote(int row)
   {
      return (row + 1) * 10 + 9;
   }

   // Top row button CCs
   public static final int CC_UP = 104;
   public static final int CC_DOWN = 105;
   public static final int CC_LEFT = 106;
   public static final int CC_RIGHT = 107;
   public static final int CC_SESSION = 108;
   public static final int CC_USER1 = 109;
   public static final int CC_USER2 = 110;
   public static final int CC_MIXER = 111;

   /**
    * Pack RGB floats (0.0-1.0) into a single int for caching.
    * MK2 SysEx RGB uses 6-bit values (0-63 per channel).
    */
   public static int packRgb(float r, float g, float b)
   {
      int ri = Math.min(63, (int)(r * 63));
      int gi = Math.min(63, (int)(g * 63));
      int bi = Math.min(63, (int)(b * 63));
      return (ri << 12) | (gi << 6) | bi;
   }

   /**
    * Dim an RGB color by reducing brightness to ~40%.
    */
   public static int dimPackedRgb(int packed)
   {
      int r = (packed >> 12) & 0x3F;
      int g = (packed >> 6) & 0x3F;
      int b = packed & 0x3F;
      return ((r * 4 / 10) << 12) | ((g * 4 / 10) << 6) | (b * 4 / 10);
   }

   // SysEx: set LED via RGB directly (exact color match)
   public static byte[] setLedRgb(int note, int packedRgb)
   {
      int r = (packedRgb >> 12) & 0x3F;
      int g = (packedRgb >> 6) & 0x3F;
      int b = packedRgb & 0x3F;
      return new byte[] {
         (byte) 0xF0, 0x00, 0x20, 0x29, 0x02, 0x18,
         0x0B, (byte) note,
         (byte) r, (byte) g, (byte) b,
         (byte) 0xF7
      };
   }

   /**
    * Find the closest bright MK2 velocity color for flash/pulse mode.
    * Maps RGB to one of the main hue entries in the velocity palette.
    */
   public static int findClosestVelocity(float r, float g, float b)
   {
      float max = Math.max(r, Math.max(g, b));
      float min = Math.min(r, Math.min(g, b));
      float delta = max - min;

      // Low saturation → white
      if (delta < 0.15f) return 3; // white

      float hue;
      if (max == r)
         hue = 60 * (((g - b) / delta) % 6);
      else if (max == g)
         hue = 60 * (((b - r) / delta) + 2);
      else
         hue = 60 * (((r - g) / delta) + 4);
      if (hue < 0) hue += 360;

      // Map hue to bright velocity colors
      if (hue < 15 || hue >= 345) return 5;   // red
      if (hue < 45) return 9;                   // orange
      if (hue < 75) return 13;                  // yellow
      if (hue < 105) return 17;                 // lime
      if (hue < 150) return 21;                 // green
      if (hue < 180) return 29;                 // mint
      if (hue < 210) return 37;                 // cyan
      if (hue < 240) return 41;                 // light blue
      if (hue < 270) return 45;                 // blue
      if (hue < 300) return 49;                 // purple
      if (hue < 330) return 53;                 // magenta
      return 57;                                 // pink
   }

   // SysEx: set LED to flash mode (rapid on/off blink, synced to tempo)
   public static byte[] flashLed(int note, int color)
   {
      return new byte[] {
         (byte) 0xF0, 0x00, 0x20, 0x29, 0x02, 0x18,
         0x23, (byte) note, (byte) color,
         (byte) 0xF7
      };
   }

   // SysEx: set LED to pulse mode (breathing animation, synced to tempo)
   public static byte[] pulseLed(int note, int color)
   {
      return new byte[] {
         (byte) 0xF0, 0x00, 0x20, 0x29, 0x02, 0x18,
         0x28, (byte) note, (byte) color,
         (byte) 0xF7
      };
   }

   // SysEx: reset all LEDs to off
   public static byte[] resetLeds()
   {
      return new byte[] {
         (byte) 0xF0, 0x00, 0x20, 0x29, 0x02, 0x18,
         0x0E, 0x00,
         (byte) 0xF7
      };
   }

   // SysEx: set session layout mode
   public static byte[] setSessionLayout()
   {
      return new byte[] {
         (byte) 0xF0, 0x00, 0x20, 0x29, 0x02, 0x18,
         0x22, 0x00,
         (byte) 0xF7
      };
   }
}
