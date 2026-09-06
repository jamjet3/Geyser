/*
 * Copyright (c) 2019-2022 GeyserMC. http://geysermc.org
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *
 * @author GeyserMC
 * @link https://github.com/GeyserMC/Geyser
 */

package org.geysermc.geyser.translator.protocol.java.inventory;

import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.nbt.NbtType;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag;
import org.cloudburstmc.protocol.bedrock.data.inventory.ContainerType;
import org.cloudburstmc.protocol.bedrock.packet.UpdateEquipPacket;
import org.geysermc.geyser.entity.type.Entity;
import org.geysermc.geyser.entity.type.living.animal.horse.CamelEntity;
import org.geysermc.geyser.entity.type.living.animal.horse.ChestedHorseEntity;
import org.geysermc.geyser.entity.type.living.animal.horse.LlamaEntity;
import org.geysermc.geyser.entity.type.living.animal.horse.SkeletonHorseEntity;
import org.geysermc.geyser.entity.type.living.animal.nautilus.NautilusEntity;
import org.geysermc.geyser.inventory.Container;
import org.geysermc.geyser.inventory.InventoryHolder;
import org.geysermc.geyser.item.GeyserCustomMappingData;
import org.geysermc.geyser.registry.type.ItemMapping;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.inventory.InventoryTranslator;
import org.geysermc.geyser.translator.inventory.horse.DonkeyInventoryTranslator;
import org.geysermc.geyser.translator.inventory.horse.LlamaInventoryTranslator;
import org.geysermc.geyser.translator.inventory.horse.MountInventoryTranslator;
import org.geysermc.geyser.translator.protocol.PacketTranslator;
import org.geysermc.geyser.translator.protocol.Translator;
import org.geysermc.geyser.util.InventoryUtils;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.inventory.ClientboundMountScreenOpenPacket;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Translator(packet = ClientboundMountScreenOpenPacket.class)
public class JavaMountScreenOpenTranslator extends PacketTranslator<ClientboundMountScreenOpenPacket> {

    private static final String[] ACCEPTED_HORSE_ARMORS = new String[] {
        "minecraft:horsearmorleather",
        "minecraft:horsearmoriron",
        "minecraft:horsearmorgold",
        "minecraft:horsearmordiamond",
        "minecraft:copper_horse_armor",
        "minecraft:netherite_horse_armor"
    };

    private static final String[] ACCEPTED_NAUTILUS_ARMORS = new String[] {
        "minecraft:copper_nautilus_armor",
        "minecraft:iron_nautilus_armor",
        "minecraft:golden_nautilus_armor",
        "minecraft:diamond_nautilus_armor",
        "minecraft:netherite_nautilus_armor"
    };

    private static final NbtMap CARPET_SLOT;
    private static final NbtMap NAUTILUS_ARMOR_SLOT;

    static {
        NAUTILUS_ARMOR_SLOT = buildAcceptedArmorSlot(
            ACCEPTED_NAUTILUS_ARMORS,
            "minecraft:nautilusarmor"
        );

        NbtMapBuilder carpetBuilder = NbtMap.builder();
        NbtMapBuilder carpetItem = NbtMap.builder()
            .putShort("Aux", Short.MAX_VALUE)
            .putString("Name", "minecraft:carpet");

        List<NbtMap> acceptedCarpet = Collections.singletonList(
            NbtMap.builder()
                .putCompound("slotItem", carpetItem.build())
                .build()
        );

        carpetBuilder.putList("acceptedItems", NbtType.COMPOUND, acceptedCarpet);
        carpetBuilder.putCompound("item", carpetItem.build());
        carpetBuilder.putInt("slotNumber", 1);
        CARPET_SLOT = carpetBuilder.build();
    }

    /**
     * Builds the Bedrock horse saddle slot definition.
     *
     * In addition to the vanilla saddle, all custom Geyser items registered
     * against minecraft:saddle are accepted.
     *
     * This allows custom saddle-slot items (for example horse blankets) to be
     * added through Geyser's custom-item mappings without requiring their
     * Bedrock identifiers to be hard-coded into Geyser.
     */
    private static NbtMap buildSaddleSlot(GeyserSession session) {
        Set<String> acceptedSaddles = new LinkedHashSet<>();
        acceptedSaddles.add("minecraft:saddle");

        ItemMapping saddle = session.getItemMappings().getMapping("minecraft:saddle");

        if (saddle != null && saddle.getCustomItemDefinitions() != null) {
            for (GeyserCustomMappingData customMapping : saddle.getCustomItemDefinitions().values()) {
                acceptedSaddles.add(customMapping.itemDefinition().getIdentifier());
            }
        }

        NbtMapBuilder saddleBuilder = NbtMap.builder();
        List<NbtMap> acceptedItems = new ArrayList<>(acceptedSaddles.size());

        for (String identifier : acceptedSaddles) {
            NbtMapBuilder acceptedItem = NbtMap.builder()
                .putShort("Aux", Short.MAX_VALUE)
                .putString("Name", identifier);

            acceptedItems.add(
                NbtMap.builder()
                    .putCompound("slotItem", acceptedItem.build())
                    .build()
            );
        }

        saddleBuilder.putList("acceptedItems", NbtType.COMPOUND, acceptedItems);

        NbtMapBuilder saddleItem = NbtMap.builder()
            .putShort("Aux", Short.MAX_VALUE)
            .putString("Name", "minecraft:saddle");

        saddleBuilder.putCompound("item", saddleItem.build());
        saddleBuilder.putInt("slotNumber", 0);
        return saddleBuilder.build();
    }

    /**
     * Builds the Bedrock horse armor slot definition.
     *
     * In addition to vanilla horse armor, all custom Geyser items registered
     * against minecraft:copper_horse_armor are accepted.
     *
     * This allows custom horse accessories to be added through Geyser's
     * custom-item mappings without requiring their Bedrock identifiers to be
     * hard-coded into Geyser.
     */
    private static NbtMap buildHorseArmorSlot(GeyserSession session) {
        Set<String> acceptedArmors = new LinkedHashSet<>();

        Collections.addAll(acceptedArmors, ACCEPTED_HORSE_ARMORS);

        ItemMapping copperHorseArmor =
            session.getItemMappings().getMapping("minecraft:copper_horse_armor");

        if (copperHorseArmor != null
            && copperHorseArmor.getCustomItemDefinitions() != null) {

            for (GeyserCustomMappingData customMapping
                : copperHorseArmor.getCustomItemDefinitions().values()) {

                acceptedArmors.add(
                    customMapping.itemDefinition().getIdentifier()
                );
            }
        }

        return buildAcceptedArmorSlot(
            acceptedArmors.toArray(String[]::new),
            "minecraft:horsearmoriron"
        );
    }

    private static NbtMap buildAcceptedArmorSlot(String[] accepted, String name) {
        NbtMapBuilder armorBuilder = NbtMap.builder();
        List<NbtMap> acceptedArmors = new ArrayList<>(accepted.length);

        for (String identifier : accepted) {
            NbtMapBuilder acceptedItemBuilder = NbtMap.builder()
                .putShort("Aux", Short.MAX_VALUE)
                .putString("Name", identifier);

            acceptedArmors.add(
                NbtMap.builder()
                    .putCompound("slotItem", acceptedItemBuilder.build())
                    .build()
            );
        }

        armorBuilder.putList(
            "acceptedItems",
            NbtType.COMPOUND,
            acceptedArmors
        );

        NbtMapBuilder armorItem = NbtMap.builder()
            .putShort("Aux", Short.MAX_VALUE)
            .putString("Name", name);

        armorBuilder.putCompound("item", armorItem.build());
        armorBuilder.putInt("slotNumber", 1);

        return armorBuilder.build();
    }

    @Override
    public void translate(
        GeyserSession session,
        ClientboundMountScreenOpenPacket packet
    ) {
        Entity entity =
            session.getEntityCache().getEntityByJavaId(packet.getEntityId());

        if (entity == null) {
            return;
        }

        UpdateEquipPacket updateEquipPacket = new UpdateEquipPacket();
        updateEquipPacket.setWindowId((short) packet.getContainerId());
        updateEquipPacket.setWindowType(
            (short) ContainerType.HORSE.getId()
        );
        updateEquipPacket.setUniqueEntityId(entity.geyserId());

        NbtMapBuilder builder = NbtMap.builder();
        List<NbtMap> slots = new ArrayList<>();

        // Since 1.20.5, the armor slot is not included in the container size,
        // but everything is still indexed the same.
        int slotCount = 2; // Don't depend on slot count sent from server

        InventoryTranslator<Container> inventoryTranslator;

        switch (entity) {
            case LlamaEntity llamaEntity -> {
                if (entity.getFlag(EntityFlag.CHESTED)) {
                    slotCount += llamaEntity.getStrength() * 3;
                }

                inventoryTranslator =
                    new LlamaInventoryTranslator(slotCount);

                slots.add(CARPET_SLOT);
            }

            case ChestedHorseEntity ignored -> {
                if (entity.getFlag(EntityFlag.CHESTED)) {
                    slotCount += 15;
                }

                inventoryTranslator =
                    new DonkeyInventoryTranslator(slotCount);

                slots.add(buildSaddleSlot(session));
            }

            case CamelEntity ignored -> {
                if (entity.getFlag(EntityFlag.CHESTED)) {
                    slotCount += 15;
                }

                // The camel has an invisible armor slot and needs special
                // handling, same as the donkey.
                inventoryTranslator =
                    new DonkeyInventoryTranslator(slotCount);

                slots.add(buildSaddleSlot(session));
            }

            default -> {
                inventoryTranslator =
                    new MountInventoryTranslator(slotCount);

                slots.add(buildSaddleSlot(session));

                if (entity instanceof NautilusEntity) {
                    slots.add(NAUTILUS_ARMOR_SLOT);
                } else if (!(entity instanceof SkeletonHorseEntity)) {
                    slots.add(buildHorseArmorSlot(session));
                }
            }
        }

        // Build the NbtMap that sets the icons for Bedrock
        // (e.g. sets the saddle outline on the saddle slot).
        builder.putList("slots", NbtType.COMPOUND, slots);

        updateEquipPacket.setTag(builder.build());
        session.sendUpstreamPacket(updateEquipPacket);

        Container container = new Container(
            session,
            entity.getNametag(),
            packet.getContainerId(),
            slotCount,
            null
        );

        InventoryUtils.openInventory(
            new InventoryHolder<>(
                session,
                container,
                inventoryTranslator
            )
        );
    }
}
