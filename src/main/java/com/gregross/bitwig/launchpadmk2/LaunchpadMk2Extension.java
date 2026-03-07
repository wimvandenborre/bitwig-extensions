package com.gregross.bitwig.launchpadmk2;

import com.bitwig.extension.controller.ControllerExtension;
import com.bitwig.extension.controller.api.ControllerHost;

public class LaunchpadMk2Extension extends ControllerExtension
{
   protected LaunchpadMk2Extension(
      final LaunchpadMk2ExtensionDefinition definition, final ControllerHost host)
   {
      super(definition, host);
   }

   @Override
   public void init()
   {
      final ControllerHost host = getHost();
      host.println("Launchpad MK2 initialized");
   }

   @Override
   public void exit()
   {
      getHost().println("Launchpad MK2 exited");
   }

   @Override
   public void flush()
   {
   }
}
