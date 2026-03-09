package dev.gregross.gig.extension;

import com.bitwig.extension.api.PlatformType;
import com.bitwig.extension.controller.AutoDetectionMidiPortNamesList;
import com.bitwig.extension.controller.ControllerExtension;
import com.bitwig.extension.controller.ControllerExtensionDefinition;
import com.bitwig.extension.controller.api.ControllerHost;

import java.util.UUID;

public class GigMaestroDefinition extends ControllerExtensionDefinition {

    private static final UUID EXTENSION_UUID = UUID.fromString("a1b2c3d4-e5f6-7890-abcd-ef1234567890");

    @Override
    public String getName() {
        return "Gig Maestro";
    }

    @Override
    public String getAuthor() {
        return "gregross";
    }

    @Override
    public String getVersion() {
        return "0.1.0";
    }

    @Override
    public UUID getId() {
        return EXTENSION_UUID;
    }

    @Override
    public int getRequiredAPIVersion() {
        return 25;
    }

    @Override
    public String getHardwareVendor() {
        return "gregross";
    }

    @Override
    public String getHardwareModel() {
        return "Gig Maestro";
    }

    @Override
    public int getNumMidiInPorts() {
        return 1;
    }

    @Override
    public int getNumMidiOutPorts() {
        return 0;
    }

    @Override
    public void listAutoDetectionMidiPortNames(AutoDetectionMidiPortNamesList list, PlatformType platformType) {
        // No MIDI ports — pure network extension
    }

    @Override
    public ControllerExtension createInstance(ControllerHost host) {
        return new GigMaestroExtension(this, host);
    }
}
