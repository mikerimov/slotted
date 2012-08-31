/*
 * Copyright 2012 NPC Unlimited
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.npc.slotted.client;

import com.google.gwt.activity.shared.Activity;

abstract public class SlottedPlace {
    abstract public Slot getParentSlot();
    abstract public Slot[] getChildSlots();

    abstract public Activity getActivity();

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override public String toString() {
        return this.getClass().getName().substring(this.getClass().getName().lastIndexOf(".")+1);
    }
}
