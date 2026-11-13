package com.galaxyhomes.data;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TPAData {

    private boolean toggle = false;
    private final List<UUID> blocked = new ArrayList<>();

    public boolean isToggle() {
        return toggle;
    }

    public void setToggle(boolean toggle) {
        this.toggle = toggle;
    }

    public List<UUID> getBlocked() {
        return blocked;
    }

    public void addBlocked(UUID uuid) {
        if (!blocked.contains(uuid)) {
            blocked.add(uuid);
        }
    }

    public void removeBlocked(UUID uuid) {
        blocked.remove(uuid);
    }

    public boolean isBlocked(UUID uuid) {
        return blocked.contains(uuid);
    }
}
