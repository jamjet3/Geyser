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

import org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes;
import org.geysermc.geyser.entity.spawn.EntitySpawnContext;
import org.geysermc.geyser.inventory.GeyserItemStack;
import org.geysermc.mcprotocollib.protocol.data.game.entity.EquipmentSlot;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.type.IntEntityMetadata;

public class HorseEntity extends AbstractHorseEntity {

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
     * variant into Bedrock MARK_VARIANT. New blanket IDs are supplied by
     * minecraft:custom_model_data and legacy IDs are handled by the shared
     * mapper in AbstractHorseEntity.
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

    @Override
    protected boolean canUseSlot(EquipmentSlot slot) {
        return true;
    }
}
