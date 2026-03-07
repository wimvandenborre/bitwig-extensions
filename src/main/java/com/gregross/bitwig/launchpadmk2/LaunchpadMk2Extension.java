package com.gregross.bitwig.launchpadmk2;

import com.bitwig.extension.controller.ControllerExtension;
import com.bitwig.extension.controller.api.ClipLauncherSlot;
import com.bitwig.extension.controller.api.ClipLauncherSlotBank;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extension.controller.api.MidiOut;
import com.bitwig.extension.controller.api.Track;
import com.bitwig.extension.controller.api.TrackBank;

public class LaunchpadMk2Extension extends ControllerExtension
{
   private static final int GRID_SIZE = 8;

   private MidiOut midiOut;
   private TrackBank trackBank;

   // Cached LED states to avoid redundant MIDI sends
   private final int[][] gridLedState = new int[GRID_SIZE][GRID_SIZE];
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

      // Set up MIDI input callback for pad presses
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

            // Request flush whenever any state changes
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

      // Reset all LEDs on startup
      midiOut.sendSysex(toHexString(LaunchpadMk2Colors.resetLeds()));
      ledsDirty = true;

      host.println("Launchpad MK2 initialized");
   }

   @Override
   public void exit()
   {
      // Turn off all LEDs
      midiOut.sendSysex(toHexString(LaunchpadMk2Colors.resetLeds()));
      getHost().println("Launchpad MK2 exited");
   }

   @Override
   public void flush()
   {
      if (!ledsDirty) return;
      ledsDirty = false;

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
                  // Use pulse mode for queued clips
                  midiOut.sendSysex(toHexString(
                     LaunchpadMk2Colors.pulseLed(note, color)));
               }
               else
               {
                  // Standard note-on for steady LED
                  midiOut.sendMidi(0x90, note, color);
               }
            }
         }
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
      // Only respond to note-on with velocity > 0 (pad press)
      if ((status & 0xF0) != 0x90 || data2 == 0) return;

      // Check if this is a grid pad (notes 11-88, excluding column 9)
      final int col = (data1 % 10) - 1;
      final int row = (data1 / 10) - 1;

      if (col < 0 || col >= GRID_SIZE || row < 0 || row >= GRID_SIZE) return;

      // col = track index, row = scene index
      final ClipLauncherSlotBank slotBank = trackBank.getItemAt(col).clipLauncherSlotBank();
      final ClipLauncherSlot slot = slotBank.getItemAt(row);

      if (slot.isPlaying().get())
      {
         // If playing, stop the track's clips
         trackBank.getItemAt(col).stop();
      }
      else
      {
         slot.launch();
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
