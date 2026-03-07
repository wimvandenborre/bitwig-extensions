package com.gregross.bitwig.launchpadmk2;

import com.bitwig.extension.controller.ControllerExtension;
import com.bitwig.extension.controller.api.ClipLauncherSlot;
import com.bitwig.extension.controller.api.ClipLauncherSlotBank;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extension.controller.api.MidiOut;
import com.bitwig.extension.controller.api.SceneBank;
import com.bitwig.extension.controller.api.Scene;
import com.bitwig.extension.controller.api.Track;
import com.bitwig.extension.controller.api.TrackBank;

public class LaunchpadMk2Extension extends ControllerExtension
{
   private static final int GRID_SIZE = 8;

   private MidiOut midiOut;
   private TrackBank trackBank;
   private SceneBank sceneBank;

   // Cached LED states to avoid redundant MIDI sends
   private final int[][] gridLedState = new int[GRID_SIZE][GRID_SIZE];
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

      // Set session layout mode on the Launchpad
      midiOut.sendSysex(toHexString(LaunchpadMk2Colors.setSessionLayout()));

      // Create track bank: 8 tracks, 0 sends, 8 scenes
      trackBank = host.createTrackBank(GRID_SIZE, 0, GRID_SIZE);
      sceneBank = trackBank.sceneBank();

      // Set up MIDI input callbacks
      midiIn.setMidiCallback(this::onMidi);

      // Set up observers for each clip slot in the 8x8 grid
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

            slot.hasContent().addValueObserver(v -> markDirty());
            slot.isPlaying().addValueObserver(v -> markDirty());
            slot.isRecording().addValueObserver(v -> markDirty());
            slot.isPlaybackQueued().addValueObserver(v -> markDirty());
            slot.isStopQueued().addValueObserver(v -> markDirty());
            slot.isRecordingQueued().addValueObserver(v -> markDirty());
         }

         t.arm().markInterested();
         t.arm().addValueObserver(v -> markDirty());
      }

      // Track bank scroll observers for navigation LED feedback
      trackBank.canScrollBackwards().markInterested();
      trackBank.canScrollForwards().markInterested();
      sceneBank.canScrollBackwards().markInterested();
      sceneBank.canScrollForwards().markInterested();

      trackBank.canScrollBackwards().addValueObserver(v -> markDirty());
      trackBank.canScrollForwards().addValueObserver(v -> markDirty());
      sceneBank.canScrollBackwards().addValueObserver(v -> markDirty());
      sceneBank.canScrollForwards().addValueObserver(v -> markDirty());

      // Reset all LEDs on startup
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
            final int color = getClipColor(slot, isArmed);
            final int note = LaunchpadMk2Colors.gridNote(scene, track);

            if (gridLedState[track][scene] != color)
            {
               gridLedState[track][scene] = color;

               if (slot.isPlaybackQueued().get() || slot.isRecordingQueued().get()
                  || slot.isStopQueued().get())
               {
                  midiOut.sendSysex(toHexString(
                     LaunchpadMk2Colors.pulseLed(note, color)));
               }
               else
               {
                  midiOut.sendMidi(0x90, note, color);
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
         final int note = LaunchpadMk2Colors.sceneLaunchNote(scene);

         if (sceneLedState[scene] != color)
         {
            sceneLedState[scene] = color;
            midiOut.sendMidi(0x90, note, color);
         }
      }
   }

   private void flushTopRow()
   {
      // Up arrow — can scroll scenes backward (up)
      sendTopRowCC(0, sceneBank.canScrollBackwards().get()
         ? LaunchpadMk2Colors.NAV_ACTIVE : LaunchpadMk2Colors.NAV_INACTIVE);
      // Down arrow — can scroll scenes forward (down)
      sendTopRowCC(1, sceneBank.canScrollForwards().get()
         ? LaunchpadMk2Colors.NAV_ACTIVE : LaunchpadMk2Colors.NAV_INACTIVE);
      // Left arrow — can scroll tracks backward (left)
      sendTopRowCC(2, trackBank.canScrollBackwards().get()
         ? LaunchpadMk2Colors.NAV_ACTIVE : LaunchpadMk2Colors.NAV_INACTIVE);
      // Right arrow — can scroll tracks forward (right)
      sendTopRowCC(3, trackBank.canScrollForwards().get()
         ? LaunchpadMk2Colors.NAV_ACTIVE : LaunchpadMk2Colors.NAV_INACTIVE);
      // Session — always lit as active mode
      sendTopRowCC(4, LaunchpadMk2Colors.MODE_ACTIVE);
      // User1, User2, Mixer — reserved, off
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

   private int getClipColor(ClipLauncherSlot slot, boolean trackArmed)
   {
      if (slot.isRecording().get())
         return LaunchpadMk2Colors.CLIP_RECORDING;
      if (slot.isRecordingQueued().get())
         return LaunchpadMk2Colors.CLIP_RECORDING;
      if (slot.isPlaybackQueued().get() || slot.isStopQueued().get())
         return LaunchpadMk2Colors.CLIP_QUEUED;
      if (slot.isPlaying().get())
         return LaunchpadMk2Colors.CLIP_PLAYING;
      if (slot.hasContent().get())
         return LaunchpadMk2Colors.CLIP_STOPPED;
      if (trackArmed)
         return LaunchpadMk2Colors.TRACK_ARMED;
      return LaunchpadMk2Colors.OFF;
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
      final int row = (note / 10) - 1;

      // Scene launch buttons (column 9 → col == 8)
      if (col == 8 && row >= 0 && row < GRID_SIZE)
      {
         sceneBank.getItemAt(row).launch();
         return;
      }

      // Grid pads
      if (col < 0 || col >= GRID_SIZE || row < 0 || row >= GRID_SIZE) return;

      final ClipLauncherSlotBank slotBank = trackBank.getItemAt(col).clipLauncherSlotBank();
      final ClipLauncherSlot slot = slotBank.getItemAt(row);

      if (slot.isPlaying().get())
      {
         trackBank.getItemAt(col).stop();
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
            trackBank.scrollBackwards();
            break;
         case LaunchpadMk2Colors.CC_RIGHT:
            trackBank.scrollForwards();
            break;
         // Session, User1, User2, Mixer — reserved, no-op
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
