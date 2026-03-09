package com.gregross.bitwig.launchpadmk2;

import java.util.UUID;

import com.bitwig.extension.api.PlatformType;
import com.bitwig.extension.controller.AutoDetectionMidiPortNamesList;
import com.bitwig.extension.controller.ControllerExtensionDefinition;
import com.bitwig.extension.controller.api.ControllerHost;

public class LaunchpadMk2ExtensionDefinition extends ControllerExtensionDefinition
{
   private static final UUID DRIVER_ID = UUID.fromString("a1b2c3d4-e5f6-7890-abcd-ef1234567890");

   @Override
   public String getName()
   {
      return "Launchpad MK2";
   }

   @Override
   public String getAuthor()
   {
      return "Greg Ross";
   }

   @Override
   public String getVersion()
   {
      return "0.1.0";
   }

   @Override
   public UUID getId()
   {
      return DRIVER_ID;
   }

   @Override
   public String getHardwareVendor()
   {
      return "Greg Ross";
   }

   @Override
   public String getHardwareModel()
   {
      return "Launchpad MK2";
   }

   @Override
   public int getRequiredAPIVersion()
   {
      return 25;
   }

   @Override
   public int getNumMidiInPorts()
   {
      return 1;
   }

   @Override
   public int getNumMidiOutPorts()
   {
      return 1;
   }

   @Override
   public void listAutoDetectionMidiPortNames(
      final AutoDetectionMidiPortNamesList list, final PlatformType platformType)
   {
      switch (platformType)
      {
         case WINDOWS:
            list.add(new String[]{"Launchpad MK2"}, new String[]{"Launchpad MK2"});
            break;
         case MAC:
            list.add(new String[]{"Launchpad MK2"}, new String[]{"Launchpad MK2"});
            break;
         case LINUX:
            list.add(new String[]{"Launchpad MK2 MIDI 1"}, new String[]{"Launchpad MK2 MIDI 1"});
            break;
      }
   }

   @Override
   public String getHelpFilePath()
   {
      return "README.md";
   }

   @Override
   public LaunchpadMk2Extension createInstance(final ControllerHost host)
   {
      return new LaunchpadMk2Extension(this, host);
   }
}
