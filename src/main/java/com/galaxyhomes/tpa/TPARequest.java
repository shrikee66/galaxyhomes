package com.galaxyhomes.tpa;

import java.util.UUID;

public class TPARequest {

    public enum Type {
        TPA,
        TPAHERE
    }

    private final UUID sender;
    private final UUID target;
    private final Type type;
    private final long createdAt;
    private final long expiresAt;

    public TPARequest(UUID sender, UUID target, Type type, long expireSeconds) {
        this.sender = sender;
        this.target = target;
        this.type = type;
        this.createdAt = System.currentTimeMillis();
        this.expiresAt = this.createdAt + expireSeconds * 1000L;
    }

    public UUID getSender() { return sender; }
    public UUID getTarget() { return target; }
    public Type getType() { return type; }
    public long getCreatedAt() { return createdAt; }
    public long getExpiresAt() { return expiresAt; }

    public long getRemainingSeconds() {
        return Math.max(0, (expiresAt - System.currentTimeMillis()) / 1000 + 1);
    }

    public boolean isExpired() {
        return System.currentTimeMillis() >= expiresAt;
    }

    public boolean isTPA() { return type == Type.TPA; }
    public boolean isTPAHere() { return type == Type.TPAHERE; }
}
