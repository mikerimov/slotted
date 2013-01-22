package com.npc.slotted.example.client;

import org.npc.slotted.client.MappedSlottedPlace;
import org.npc.slotted.client.Slot;
import org.npc.slotted.client.SlottedController;

public class HelloPlace extends MappedSlottedPlace {
    public HelloPlace(String token) {
        setParameter("helloName", token);
    }

    public String getHelloName() {
        return getParameter("helloName");
    }

    @Override public Slot getParentSlot() {
        return SlottedController.RootSlot;
    }

    @Override public Slot[] getChildSlots() {
        return null;
    }
}