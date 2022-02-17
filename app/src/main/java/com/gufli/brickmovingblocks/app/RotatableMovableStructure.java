package com.gufli.brickmovingblocks.app;

import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Metadata;
import net.minestom.server.entity.metadata.minecart.MinecartMeta;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;
import net.minestom.server.tag.Tag;
import org.jetbrains.annotations.NotNull;
import org.jglrxavpok.hephaistos.nbt.mutable.MutableNBTCompound;

@Deprecated
public class RotatableMovableStructure extends MovableStructure {

    public RotatableMovableStructure(Instance instance, Point position) {
        super(instance, position);
    }

    public void rotate(double angle) {
        angle = (angle % 360);
        double rad = Math.toRadians(angle);

        float yaw = (float) angle - 180;

        for ( Stack stack : stacks ) {
//            double x = Math.cos(rad);
//            double z = Math.sin(rad);
            Pos absolute = new Pos(position/*.add(x, 0, z)*/).withView(yaw, 0);
            stack.boat().refreshPosition(absolute);

            for ( Entity passenger : stack.passengers() ) {
                passenger.setView(yaw, 0);
            }
        }
    }

    @Override
    protected Entity spawnBlock(Point position, Block block) {
        CustomMinecart minecart = new CustomMinecart();
        minecart.setCustomBlockVisible(true);
        minecart.setCustomBlock(block);

        minecart.setNoGravity(true);
        minecart.setInstance(instance, position);
        return minecart;
    }

    public static class CustomMinecart extends Entity {

        public CustomMinecart() {
            super(EntityType.MINECART);
        }

        public void setCustomBlock(Block block) {
            metadata.setIndex(11, Metadata.VarInt(block.id()));
        }

        public void setCustomBlockOffset(int offset) {
            metadata.setIndex(12, Metadata.VarInt(offset));
        }

        public void setCustomBlockVisible(boolean value) {
            metadata.setIndex(13, Metadata.Boolean(value));
        }

    }

}
