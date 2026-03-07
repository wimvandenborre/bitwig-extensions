package com.gregross.bitwig.launchpadmk2;

import com.bitwig.extension.controller.ControllerExtension;
import com.bitwig.extension.controller.api.ClipLauncherSlot;
import com.bitwig.extension.controller.api.ClipLauncherSlotBank;
import com.bitwig.extension.controller.api.ColorValue;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extension.controller.api.MidiOut;
import com.bitwig.extension.controller.api.SceneBank;
import com.bitwig.extension.controller.api.Track;
import com.bitwig.extension.controller.api.TrackBank;

public class LaunchpadMk2Extension extends ControllerExtension
{
   private static final int GRID_SIZE = 8;

   // LED mode flags for cache
   private static final int MODE_VELOCITY = 0;
   private static final int MODE_RGB = 1;
   private static final int MODE_PULSE = 2;
   private static final int MODE_FLASH = 3;

   private MidiOut midiOut;
   private TrackBank trackBank;
   private SceneBank sceneBank;

   // Cached LED states: [track][scene] stores color value
   private final int[][] gridLedColor = new int[GRID_SIZE][GRID_SIZE];
   private final int[][] gridLedMode = new int[GRID_SIZE][GRID_SIZE];
   private final int[] sceneLedState = new int[GRID_SIZE];
   private final int[] topRowLedState = new int[GRID_SIZE];
   private boolean ledsDirty = true;

   protected LaunchpadMk2Extension(
      final LaunchpadMk2ExtensionDefinition definition, final ControllerHost host)
   {
      super(definition, host);
   }

   @Override
   public void init()
   {
      final ControllerHost host = getHost();

      final MidiIn midiIn = host.getMidiInPort(0);
      midiOut = host.getMidiOutPort(0);

      midiOut.sendSysex(toHexString(LaunchpadMk2Colors.setSessionLayout()));

      trackBank = host.createTrackBank(GRID_SIZE, 0, GRID_SIZE);
      trackBank.setShouldShowClipLauncherFeedback(true);
      sceneBank = trackBank.sceneBank();

      midiIn.setMidiCallback(this::onMidi);

      for (int track = 0; track < GRID_SIZE; track++)
      {
         final Track t = trackBank.getItemAt(track);
         final ClipLauncherSlotBank slotBank = t.clipLauncherSlotBank();

         for (int scene = 0; scene < GRID_SIZE; scene++)
         {
            final ClipLauncherSlot slot = slotBank.getItemAt(scene);
            slot.hasContent().markInterested();
            slot.isPlaying().markInterested();
            slot.isRecording().markInterested();
            slot.isPlaybackQueued().markInterested();
            slot.isStopQueued().markInterested();
            slot.isRecordingQueued().markInterested();
            slot.color().markInterested();

            slot.hasContent().addValueObserver(v -> markDirty());
            slot.isPlaying().addValueObserver(v -> markDirty());
            slot.isRecording().addValueObserver(v -> markDirty());
            slot.isPlaybackQueued().addValueObserver(v -> markDirty());
            slot.isStopQueued().addValueObserver(v -> markDirty());
            slot.isRecordingQueued().addValueObserver(v -> markDirty());
            slot.color().addValueObserver((r, g, b) -> markDirty());
         }

         t.arm().markInterested();
         t.arm().addValueObserver(v -> markDirty());
         t.color().markInterested();
         t.color().addValueObserver((r, g, b) -> markDirty());
      }

      trackBank.canScrollBackwards().markInterested();
      trackBank.canScrollForwards().markInterested();
      sceneBank.canScrollBackwards().markInterested();
      sceneBank.canScrollForwards().markInterested();

      trackBank.canScrollBackwards().addValueObserver(v -> markDirty());
      trackBank.canScrollForwards().addValueObserver(v -> markDirty());
      sceneBank.canScrollBackwards().addValueObserver(v -> markDirty());
      sceneBank.canScrollForwards().addValueObserver(v -> markDirty());

      // Initialize cache to -1 so first flush sends everything
      for (int t = 0; t < GRID_SIZE; t++)
         for (int s = 0; s < GRID_SIZE; s++)
         {
            gridLedColor[t][s] = -1;
            gridLedMode[t][s] = -1;
         }
      for (int i = 0; i < GRID_SIZE; i++)
      {
         sceneLedState[i] = -1;
         topRowLedState[i] = -1;
      }

      midiOut.sendSysex(toHexString(LaunchpadMk2Colors.resetLeds()));
      ledsDirty = true;

      host.println("Launchpad MK2 initialized");
   }

   @Override
   public void exit()
   {
      midiOut.sendSysex(toHexString(LaunchpadMk2Colors.resetLeds()));
      getHost().println("Launchpad MK2 exited");
   }

   @Override
   public void flush()
   {
      if (!ledsDirty) return;
      ledsDirty = false;

      flushGrid();
      flushSceneLaunch();
      flushTopRow();
   }

   private void flushGrid()
   {
      for (int track = 0; track < GRID_SIZE; track++)
      {
         final Track t = trackBank.getItemAt(track);
         final ClipLauncherSlotBank slotBank = t.clipLauncherSlotBank();
         final boolean isArmed = t.arm().get();

         for (int scene = 0; scene < GRID_SIZE; scene++)
         {
            final ClipLauncherSlot slot = slotBank.getItemAt(scene);

            // Invert row: Bitwig scene 0 = top = Launchpad row 7
            final int padRow = (GRID_SIZE - 1) - scene;
            // Invert column for CCW rotation: track 0 = rightmost column (top when rotated)
            final int padCol = (GRID_SIZE - 1) - track;
            final int note = LaunchpadMk2Colors.gridNote(padRow, padCol);

            int mode;
            int color;

            if (slot.isRecording().get() || slot.isRecordingQueued().get())
            {
               mode = MODE_VELOCITY;
               color = LaunchpadMk2Colors.CLIP_RECORDING;
            }
            else if (slot.isPlaybackQueued().get() || slot.isStopQueued().get())
            {
               mode = MODE_PULSE;
               color = LaunchpadMk2Colors.CLIP_QUEUED;
            }
            else if (slot.isPlaying().get() && slot.hasContent().get())
            {
               // Playing: pulse bright red — stands out against colored stopped clips
               mode = MODE_PULSE;
               color = 5; // bright red velocity
            }
            else if (slot.hasContent().get())
            {
               // Stopped with content: full brightness clip color via SysEx RGB
               final ColorValue c = slot.color();
               mode = MODE_RGB;
               color = LaunchpadMk2Colors.packRgb(c.red(), c.green(), c.blue());
            }
            else if (isArmed)
            {
               mode = MODE_VELOCITY;
               color = LaunchpadMk2Colors.TRACK_ARMED;
            }
            else
            {
               // Empty slot: off
               mode = MODE_VELOCITY;
               color = LaunchpadMk2Colors.OFF;
            }

            if (gridLedColor[track][scene] != color || gridLedMode[track][scene] != mode)
            {
               gridLedColor[track][scene] = color;
               gridLedMode[track][scene] = mode;

               switch (mode)
               {
                  case MODE_RGB:
                     midiOut.sendSysex(toHexString(
                        LaunchpadMk2Colors.setLedRgb(note, color)));
                     break;
                  case MODE_FLASH:
                     // Channel 1 (0x90) sets static color, Channel 2 (0x91) sets flash
                     // Flash alternates between static and flash color
                     midiOut.sendMidi(0x90, note, color);  // base color
                     midiOut.sendMidi(0x91, note, color);  // flash same color (blinks on/off)
                     break;
                  case MODE_PULSE:
                     // Channel 3 (0x92) sets pulse (breathing animation)
                     midiOut.sendMidi(0x92, note, color);
                     break;
                  default:
                     midiOut.sendMidi(0x90, note, color);
                     break;
               }
            }
         }
      }
   }

   private void flushSceneLaunch()
   {
      for (int scene = 0; scene < GRID_SIZE; scene++)
      {
         final int color = LaunchpadMk2Colors.SCENE_LAUNCH;
         final int padRow = (GRID_SIZE - 1) - scene;
         final int note = LaunchpadMk2Colors.sceneLaunchNote(padRow);

         if (sceneLedState[scene] != color)
         {
            sceneLedState[scene] = color;
            midiOut.sendMidi(0x90, note, color);
         }
      }
   }

   private void flushTopRow()
   {
      sendTopRowCC(0, sceneBank.canScrollBackwards().get()
         ? LaunchpadMk2Colors.NAV_ACTIVE : LaunchpadMk2Colors.NAV_INACTIVE);
      sendTopRowCC(1, sceneBank.canScrollForwards().get()
         ? LaunchpadMk2Colors.NAV_ACTIVE : LaunchpadMk2Colors.NAV_INACTIVE);
      // Swapped for CCW rotation: left arrow (idx 2) now points down, right (idx 3) points up
      sendTopRowCC(2, trackBank.canScrollForwards().get()
         ? LaunchpadMk2Colors.NAV_ACTIVE : LaunchpadMk2Colors.NAV_INACTIVE);
      sendTopRowCC(3, trackBank.canScrollBackwards().get()
         ? LaunchpadMk2Colors.NAV_ACTIVE : LaunchpadMk2Colors.NAV_INACTIVE);
      sendTopRowCC(4, LaunchpadMk2Colors.MODE_ACTIVE);
      sendTopRowCC(5, LaunchpadMk2Colors.MODE_INACTIVE);
      sendTopRowCC(6, LaunchpadMk2Colors.MODE_INACTIVE);
      sendTopRowCC(7, LaunchpadMk2Colors.MODE_INACTIVE);
   }

   private void sendTopRowCC(int index, int color)
   {
      if (topRowLedState[index] != color)
      {
         topRowLedState[index] = color;
         midiOut.sendMidi(0xB0, LaunchpadMk2Colors.CC_UP + index, color);
      }
   }

   private void onMidi(int status, int data1, int data2)
   {
      final int type = status & 0xF0;

      if (type == 0x90 && data2 > 0)
      {
         onNoteOn(data1);
      }
      else if (type == 0xB0 && data2 > 0)
      {
         onCC(data1);
      }
   }

   private void onNoteOn(int note)
   {
      final int col = (note % 10) - 1;
      final int padRow = (note / 10) - 1;
      final int scene = (GRID_SIZE - 1) - padRow;

      if (col == 8 && scene >= 0 && scene < GRID_SIZE)
      {
         // Check if any clip in this scene row is playing
         boolean anyPlaying = false;
         for (int t = 0; t < GRID_SIZE; t++)
         {
            final ClipLauncherSlot s = trackBank.getItemAt(t).clipLauncherSlotBank().getItemAt(scene);
            if (s.isPlaying().get())
            {
               anyPlaying = true;
               break;
            }
         }

         if (anyPlaying)
         {
            // Stop all tracks
            for (int t = 0; t < GRID_SIZE; t++)
            {
               trackBank.getItemAt(t).stop();
            }
         }
         else
         {
            sceneBank.getItemAt(scene).launch();
         }
         return;
      }

      if (col < 0 || col >= GRID_SIZE || scene < 0 || scene >= GRID_SIZE) return;

      // Invert column for CCW rotation: rightmost hardware col = track 0
      final int track = (GRID_SIZE - 1) - col;
      final ClipLauncherSlotBank slotBank = trackBank.getItemAt(track).clipLauncherSlotBank();
      final ClipLauncherSlot slot = slotBank.getItemAt(scene);

      if (slot.isPlaying().get())
      {
         trackBank.getItemAt(track).stop();
      }
      else
      {
         slot.launch();
      }
   }

   private void onCC(int cc)
   {
      switch (cc)
      {
         case LaunchpadMk2Colors.CC_UP:
            sceneBank.scrollBackwards();
            break;
         case LaunchpadMk2Colors.CC_DOWN:
            sceneBank.scrollForwards();
            break;
         case LaunchpadMk2Colors.CC_LEFT:
            // After CCW rotation, left arrow points down → scroll tracks forward
            trackBank.scrollForwards();
            break;
         case LaunchpadMk2Colors.CC_RIGHT:
            // After CCW rotation, right arrow points up → scroll tracks backward
            trackBank.scrollBackwards();
            break;
      }
   }

   private void markDirty()
   {
      ledsDirty = true;
      getHost().requestFlush();
   }

   private static String toHexString(byte[] bytes)
   {
      final StringBuilder sb = new StringBuilder(bytes.length * 2);
      for (byte b : bytes)
      {
         sb.append(String.format("%02X", b & 0xFF));
      }
      return sb.toString();
   }
}
