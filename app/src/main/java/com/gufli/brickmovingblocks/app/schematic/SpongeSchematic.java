package com.gufli.brickmovingblocks.app.schematic;

import kotlin.Pair;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.instance.block.Block;
import org.jetbrains.annotations.NotNull;
import org.jglrxavpok.hephaistos.nbt.NBT;
import org.jglrxavpok.hephaistos.nbt.NBTCompound;
import org.jglrxavpok.hephaistos.nbt.NBTReader;

import java.io.File;
import java.util.*;
import java.util.regex.Pattern;

public class SpongeSchematic {

    private final List<RegionBlock> blocks = new ArrayList<>();

    private int width;
    private int height;
    private int length;

    private Point offset = new Vec(0, 0, 0);

    public static record RegionBlock(Point position, Block blockState) {
    }

    public SpongeSchematic(@NotNull File schematicFile) {
        load(schematicFile);
    }

    // https://github.com/SpongePowered/Schematic-Specification/blob/master/versions/schematic-2.md
    private void load(File schematicFile) {
        try (NBTReader reader = new NBTReader(schematicFile)) {
            Pair<String, NBT> pair = reader.readNamed();
            NBTCompound nbt = (NBTCompound) pair.getSecond();

            if (!pair.getFirst().equals("Schematic")) {
                return;
            }

            if (!nbt.containsKey("BlockData")) {
                return;
            }

            width = nbt.getShort("Width");
            height = nbt.getShort("Height");
            length = nbt.getShort("Length");

            if ( nbt.contains("Offset") ) {
                int[] offset = nbt.getIntArray("Offset").copyArray();
                this.offset = new Vec(offset[0], offset[1], offset[2]);
            }

            byte[] blocks = nbt.getByteArray("BlockData").copyArray();
            List<Block> palette = parsePalette(nbt.getCompound("Palette"));

            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    for (int z = 0; z < length; z++) {
                        int index = x + z * width + y * width * length;
                        int stateId = blocks[index];
                        if (stateId < 0 || stateId >= palette.size()) {
                            continue;
                        }

                        this.blocks.add(new RegionBlock(new Vec(x, y, z), palette.get(stateId)));
                    }
                }
            }
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public List<RegionBlock> blocks() {
        return Collections.unmodifiableList(blocks);
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    public int length() {
        return length;
    }

    public Point offset() {
        return offset;
    }

    //

    private List<Block> parsePalette(NBTCompound nbt) {
        return nbt.getKeys().stream()
                .sorted(Comparator.comparingInt(nbt::getInt))
                .map(this::parseState)
                .toList();
    }

    private Block parseState(String state) {
        int bracket = state.indexOf("[");

        if (bracket > 0) {
            Block block = Block.fromNamespaceId(state.substring(0, bracket));

            String data = state.substring(bracket + 1, state.length() - 1);
            String[] properties = data.split(Pattern.quote(","));
            for (String property : properties) {
                String[] pair = property.split(Pattern.quote("="));
                block = block.withProperty(pair[0], pair[1]);
            }

            return block;
        }

        return Block.fromNamespaceId(state);
    }

}