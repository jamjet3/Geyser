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

package org.geysermc.geyser.entity.type.living.animal.horse;

import net.kyori.adventure.key.Key;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes;
import org.geysermc.geyser.entity.spawn.EntitySpawnContext;
import org.geysermc.geyser.inventory.GeyserItemStack;
import org.geysermc.mcprotocollib.protocol.data.game.entity.EquipmentSlot;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.type.IntEntityMetadata;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentTypes;

public class HorseEntity extends AbstractHorseEntity {

    private static final String[] MODN_BLANKET_PATTERNS = {
        "solid", "quilted", "zebra", "bandedzebra", "fullbandedzebra"
    };

    private static final String[] MODN_BLANKET_COLORS = {
        "black", "darkgrey", "lightgrey", "white",
        "brown", "red", "orange", "yellow",
        "lime", "green", "cyan", "lightblue",
        "blue", "purple", "magenta", "pink"
    };

    private int javaMarkVariant;
    private int modnBlanketVariant;

    public HorseEntity(EntitySpawnContext context) {
        super(context);
    }

    public void setHorseVariant(IntEntityMetadata entityMetadata) {
        int value = entityMetadata.getPrimitiveValue();
        metadata.put(EntityDataTypes.VARIANT, value & 255);
        javaMarkVariant = (value >> 8) % 5;
        updatePackedMarkVariant(false);
    }

    @Override
    public void setSaddle(GeyserItemStack stack) {
        super.setSaddle(stack);
        modnBlanketVariant = getModnBlanketVariant(stack);
        updatePackedMarkVariant(true);
    }

    /**
     * Packs the normal Java horse marking (0-4) together with the ModN blanket
     * variant (0-80) into Bedrock MARK_VARIANT.
     *
     * Bedrock resource-pack decoding:
     *   marking    = mark_variant % 5
     *   blanket id = floor(mark_variant / 5)
     *
     * This preserves the vanilla horse marking while exposing custom saddle-slot
     * blanket state to the Bedrock resource pack, whose Molang renderer cannot
     * reliably inspect custom tags in slot.saddle.
     */
    private void updatePackedMarkVariant(boolean sendImmediately) {
        int packed = javaMarkVariant + (modnBlanketVariant * 5);
        metadata.put(EntityDataTypes.MARK_VARIANT, packed);
        if (sendImmediately) {
            updateBedrockMetadata();
        }
    }

    private static int getModnBlanketVariant(GeyserItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return 0;
        }

        Key model = stack.getComponent(DataComponentTypes.ITEM_MODEL);
        if (model == null || !"blankets".equals(model.namespace())) {
            return 0;
        }

        String value = model.value();
        if (!value.startsWith("item_")) {
            return 0;
        }

        String blanketKey = value.substring("item_".length());
        for (int patternIndex = 0; patternIndex < MODN_BLANKET_PATTERNS.length; patternIndex++) {
            String prefix = MODN_BLANKET_PATTERNS[patternIndex] + "_";
            if (!blanketKey.startsWith(prefix)) {
                continue;
            }

            String color = blanketKey.substring(prefix.length());
            for (int colorIndex = 0; colorIndex < MODN_BLANKET_COLORS.length; colorIndex++) {
                if (MODN_BLANKET_COLORS[colorIndex].equals(color)) {
                    return (patternIndex * MODN_BLANKET_COLORS.length) + colorIndex + 1;
                }
            }
        }

        return 0;
    }

    @Override
    protected boolean canUseSlot(EquipmentSlot slot) {
        return true;
    }
}
