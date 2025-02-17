package com.alantaru.territorywars;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Clan {
    private String name;
    private List<UUID> members;

    public Clan(String name) {
        this.name = name;
        this.members = new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<UUID> getMembers() {
        return members;
    }

    public void addMember(UUID member) {
        members.add(member);
    }

    public void removeMember(UUID member) {
        members.remove(member);
    }
}
