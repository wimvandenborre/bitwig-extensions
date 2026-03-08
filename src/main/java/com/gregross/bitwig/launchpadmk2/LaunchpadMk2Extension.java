package com.gregross.bitwig.launchpadmk2;

import com.bitwig.extension.controller.ControllerExtension;
import com.bitwig.extension.controller.api.ClipLauncherSlot;
import com.bitwig.extension.controller.api.ClipLauncherSlotBank;
import com.bitwig.extension.controller.api.ColorValue;
import com.bitwig.extension.controller.api.Application;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.CursorTrack;
import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extension.controller.api.MidiOut;
import com.bitwig.extension.controller.api.Scene;
import com.bitwig.extension.controller.api.SceneBank;
import com.bitwig.extension.controller.api.Track;
import com.bitwig.extension.controller.api.TrackBank;
import com.bitwig.extension.controller.api.Transport;

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
   private CursorTrack cursorTrack;
   private Transport transport;
   private Application application;

   // Cached LED states: [track][scene] stores color value
   private final int[][] gridLedColor = new int[GRID_SIZE][GRID_SIZE];
   private final int[][] gridLedMode = new int[GRID_SIZE][GRID_SIZE];
   private final int[] sceneLedColor = new int[GRID_SIZE];
   private final int[] sceneLedMode = new int[GRID_SIZE];
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

      cursorTrack = host.createCursorTrack("LAUNCHPAD_MK2_CURSOR", "Cursor Track", 0, GRID_SIZE, true);
      trackBank = host.createMainTrackBank(GRID_SIZE, 0, GRID_SIZE);
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

      for (int scene = 0; scene < GRID_SIZE; scene++)
      {
         final Scene s = sceneBank.getScene(scene);
         s.color().markInterested();
         s.color().addValueObserver((r, g, b) -> markDirty());
      }

      transport = host.createTransport();
      transport.isPlaying().markInterested();
      transport.isPlaying().addValueObserver(v -> markDirty());
      transport.isArrangerRecordEnabled().markInterested();
      transport.isArrangerRecordEnabled().addValueObserver(v -> markDirty());

      application = host.createApplication();

      cursorTrack.hasPrevious().markInterested();
      cursorTrack.hasNext().markInterested();
      sceneBank.canScrollBackwards().markInterested();
      sceneBank.canScrollForwards().markInterested();

      cursorTrack.hasPrevious().addValueObserver(v -> markDirty());
      cursorTrack.hasNext().addValueObserver(v -> markDirty());
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
         sceneLedColor[i] = -1;
         sceneLedMode[i] = -1;
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
         final int padRow = (GRID_SIZE - 1) - scene;
         final int note = LaunchpadMk2Colors.sceneLaunchNote(padRow);

         // Check clip states across all tracks for this scene
         boolean allPlaying = true;
         boolean anyPlaying = false;
         boolean anyQueued = false;
         boolean anyContent = false;
         for (int t = 0; t < GRID_SIZE; t++)
         {
            final ClipLauncherSlot slot = trackBank.getItemAt(t).clipLauncherSlotBank().getItemAt(scene);
            if (slot.hasContent().get())
            {
               anyContent = true;
               if (slot.isPlaying().get()) anyPlaying = true;
               else allPlaying = false;
            }
            if (slot.isPlaybackQueued().get() || slot.isStopQueued().get()) anyQueued = true;
         }
         if (!anyContent) allPlaying = false;

         int mode;
         int color;

         if (anyQueued)
         {
            mode = MODE_PULSE;
            color = LaunchpadMk2Colors.CLIP_QUEUED;
         }
         else if (allPlaying && anyContent)
         {
            // All clips playing — pressing will stop; show pulse red
            mode = MODE_PULSE;
            color = LaunchpadMk2Colors.CLIP_RECORDING; // bright red velocity 5
         }
         else if (anyPlaying)
         {
            // Some but not all playing — pressing will launch; show pulse orange
            mode = MODE_PULSE;
            color = 9; // orange velocity
         }
         else
         {
            // Idle: show scene's own color
            final ColorValue c = sceneBank.getScene(scene).color();
            mode = MODE_RGB;
            color = LaunchpadMk2Colors.packRgb(c.red(), c.green(), c.blue());
         }

         if (sceneLedColor[scene] != color || sceneLedMode[scene] != mode)
         {
            sceneLedColor[scene] = color;
            sceneLedMode[scene] = mode;

            switch (mode)
            {
               case MODE_RGB:
                  midiOut.sendSysex(toHexString(
                     LaunchpadMk2Colors.setLedRgb(note, color)));
                  break;
               case MODE_PULSE:
                  midiOut.sendMidi(0x92, note, color);
                  break;
               default:
                  midiOut.sendMidi(0x90, note, color);
                  break;
            }
         }
      }
   }

   private void flushTopRow()
   {
      sendTopRowCC(0, sceneBank.canScrollBackwards().get()
         ? LaunchpadMk2Colors.NAV_SCENE_ACTIVE : LaunchpadMk2Colors.NAV_INACTIVE);
      sendTopRowCC(1, sceneBank.canScrollForwards().get()
         ? LaunchpadMk2Colors.NAV_SCENE_ACTIVE : LaunchpadMk2Colors.NAV_INACTIVE);
      // CCW rotation: left (idx 2) points down = next track, right (idx 3) points up = prev track
      sendTopRowCC(2, cursorTrack.hasNext().get()
         ? LaunchpadMk2Colors.NAV_TRACK_ACTIVE : LaunchpadMk2Colors.NAV_INACTIVE);
      sendTopRowCC(3, cursorTrack.hasPrevious().get()
         ? LaunchpadMk2Colors.NAV_TRACK_ACTIVE : LaunchpadMk2Colors.NAV_INACTIVE);
      // Utility buttons (after CCW rotation: bottom 4 on left column)
      // CC 108 = Play/Stop
      if (transport.isPlaying().get())
         sendTopRowCCPulse(4, 21); // green pulse when playing
      else
         sendTopRowCC(4, 23); // dim green when stopped

      // CC 109 = Stop All — static red
      sendTopRowCC(5, LaunchpadMk2Colors.CLIP_RECORDING);

      // CC 110 = Undo — static white
      sendTopRowCC(6, 3);

      // CC 111 = Record
      if (transport.isArrangerRecordEnabled().get())
         sendTopRowCCPulse(7, 5); // red pulse when recording
      else
         sendTopRowCC(7, 0); // off when not recording
   }

   private void sendTopRowCC(int index, int color)
   {
      if (topRowLedState[index] != color)
      {
         topRowLedState[index] = color;
         midiOut.sendMidi(0xB0, LaunchpadMk2Colors.CC_UP + index, color);
      }
   }

   private void sendTopRowCCPulse(int index, int color)
   {
      if (topRowLedState[index] != color)
      {
         topRowLedState[index] = color;
         midiOut.sendMidi(0xB2, LaunchpadMk2Colors.CC_UP + index, color);
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
         // Check if ALL tracks with content in this scene are playing
         boolean allPlaying = true;
         boolean anyContent = false;
         for (int t = 0; t < GRID_SIZE; t++)
         {
            final ClipLauncherSlot s = trackBank.getItemAt(t).clipLauncherSlotBank().getItemAt(scene);
            if (s.hasContent().get())
            {
               anyContent = true;
               if (!s.isPlaying().get())
               {
                  allPlaying = false;
                  break;
               }
            }
         }

         if (allPlaying && anyContent)
         {
            // All clips in scene are playing — stop all tracks
            for (int t = 0; t < GRID_SIZE; t++)
            {
               trackBank.getItemAt(t).stop();
            }
         }
         else
         {
            // Not all playing — launch the scene
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
            // After CCW rotation, left arrow points down → select next track
            cursorTrack.selectNext();
            break;
         case LaunchpadMk2Colors.CC_RIGHT:
            // After CCW rotation, right arrow points up → select previous track
            cursorTrack.selectPrevious();
            break;
         case LaunchpadMk2Colors.CC_SESSION:
            transport.togglePlay();
            break;
         case LaunchpadMk2Colors.CC_USER1:
            // Stop all clips
            for (int t = 0; t < GRID_SIZE; t++)
            {
               trackBank.getItemAt(t).stop();
            }
            break;
         case LaunchpadMk2Colors.CC_USER2:
            application.undo();
            break;
         case LaunchpadMk2Colors.CC_MIXER:
            transport.isArrangerRecordEnabled().toggle();
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
