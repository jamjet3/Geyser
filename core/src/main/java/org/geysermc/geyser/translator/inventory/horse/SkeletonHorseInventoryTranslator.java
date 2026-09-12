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

package org.geysermc.geyser.translator.inventory.horse;

import org.geysermc.geyser.inventory.Container;
import org.geysermc.geyser.session.GeyserSession;

/**
 * Skeleton horses only expose the saddle equipment slot to Bedrock.
 *
 * Java's mount container still reserves two equipment indices (saddle + body/armor),
 * but Bedrock only has one visible horse-equipment slot for a skeleton horse.
 *
 * Using the normal MountInventoryTranslator causes the hidden Java slot to be
 * treated as a second Bedrock HORSE_EQUIP slot. That can leave the visible
 * saddle slot empty/stale even while the entity is actually wearing a saddle
 * or custom saddle-slot item.
 */
public final class SkeletonHorseInventoryTranslator extends ChestedHorseInventoryTranslator {

    public SkeletonHorseInventoryTranslator(int size) {
        // Java saddle slot is index 0. Bedrock exposes only this one equipment slot.
        super(size, 0);
    }

    @Override
    public void updateSlot(GeyserSession session, Container container, int slot) {
        /*
         * Java slot 1 is the hidden body/armor equipment index.
         * ChestedHorseInventoryTranslator intentionally collapses the two Java
         * equipment indices into one Bedrock-visible equipment layout. If the
         * hidden slot is forwarded as a slot update it can overwrite the visible
         * saddle slot with EMPTY, producing the skeleton-horse "ghost saddle"
         * desync.
         */
        if (slot == 1) {
            return;
        }

        super.updateSlot(session, container, slot);
    }
}
