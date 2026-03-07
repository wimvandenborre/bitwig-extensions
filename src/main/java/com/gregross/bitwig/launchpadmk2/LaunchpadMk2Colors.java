package com.gregross.bitwig.launchpadmk2;

public class LaunchpadMk2Colors
{
   // Clip state colors (velocity values from MK2's 128-color palette)
   public static final int OFF = 0;
   public static final int CLIP_STOPPED = 23;     // dim green
   public static final int CLIP_PLAYING = 21;     // bright green
   public static final int CLIP_RECORDING = 5;    // bright red
   public static final int CLIP_QUEUED = 61;      // amber/yellow
   public static final int TRACK_ARMED = 11;      // dim orange
   public static final int SCENE_LAUNCH = 65;     // dim cyan
   public static final int SCENE_PLAYING = 21;    // bright green

   // Top row button colors
   public static final int NAV_ACTIVE = 119;      // dim white
   public static final int NAV_INACTIVE = 0;      // off
   public static final int MODE_ACTIVE = 21;      // bright green
   public static final int MODE_INACTIVE = 0;     // off

   // SysEx header for Launchpad MK2
   private static final byte[] SYSEX_HEADER = {
      (byte) 0xF0, 0x00, 0x20, 0x29, 0x02, 0x18
   };

   // MIDI note mapping: grid pad note = (row + 1) * 10 + (col + 1)
   // Row 0 = bottom, row 7 = top; col 0 = left, col 7 = right
   public static int gridNote(int row, int col)
   {
      return (row + 1) * 10 + (col + 1);
   }

   // Scene launch buttons are in column 9: notes 19, 29, 39, ..., 89
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

   // SysEx: set LED to pulse mode (breathing animation)
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
