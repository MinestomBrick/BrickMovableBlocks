package org.minestombrick.movablestructures.app;

import net.minestom.server.extensions.Extension;

public class BrickMovableStructures extends Extension {

    @Override
    public void initialize() {
        getLogger().info("Enabling " + nameAndVersion() + ".");

        // nothing to see here

        getLogger().info("Enabled " + nameAndVersion() + ".");
    }

    @Override
    public void terminate() {
        getLogger().info("Disabled " + nameAndVersion() + ".");
    }

    private String nameAndVersion() {
        return getOrigin().getName() + " v" + getOrigin().getVersion();
    }

}
