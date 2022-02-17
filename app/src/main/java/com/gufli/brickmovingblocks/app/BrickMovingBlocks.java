package com.gufli.brickmovingblocks.app;

import com.gufli.brickmovingblocks.app.schematic.SpongeSchematic;
import com.gufli.brickworlds.WorldAPI;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.extensions.Extension;
import net.minestom.server.instance.Instance;

import java.io.File;
import java.time.temporal.ChronoUnit;
import java.util.List;

public class BrickMovingBlocks extends Extension {

    private final static Point point = new Vec(-5115, 77, 2415);

    private double x = 0;
    private double y = 0;
    private int direction = 1;

    @Override
    public void initialize() {
        getLogger().info("Enabling " + nameAndVersion() + ".");

        Instance world = (Instance) WorldAPI.worldByName("vulcano").orElse(null);
        world.loadChunk(point).join();

        MovableStructure structure = new MovableStructure(world, point);

        SpongeSchematic schem = new SpongeSchematic(new File(getDataDirectory().toFile(), "hotairballoon1.schem"));
        structure.load(schem);

        final Point spoint = new Vec(-5086, 69, 2396);
        List<SpongeSchematic.RegionBlock> blocks = schem.blocks();
        blocks.forEach(b -> {
            world.setBlock(spoint.add(b.position()), b.blockState());
        });

        MinecraftServer.getSchedulerManager().buildTask(() -> {
                    structure.teleport(point.add(x, y, 0));
                    x += .2 * direction;
                    y += .1 * direction;

                    if ( x >= 10 || x <= 0 ) {
                        direction = -direction;
                    }
                })
                .repeat(100, ChronoUnit.MILLIS)
                .schedule();

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
