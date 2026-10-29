package io.github.eskopp.wayfinder;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class TpaRequests {

    private record Request(UUID requester, long expiresAt) {
    }

    private final Map<UUID, Request> pending = new HashMap<>();

    public void create(UUID requester, UUID target, long timeoutMillis) {
        pending.put(target, new Request(requester, System.currentTimeMillis() + timeoutMillis));
    }

    public Optional<UUID> take(UUID target) {
        Request request = pending.remove(target);
        if (request == null || request.expiresAt() < System.currentTimeMillis()) {
            return Optional.empty();
        }
        return Optional.of(request.requester());
    }

    public void clear(UUID player) {
        pending.remove(player);
        pending.values().removeIf(r -> r.requester().equals(player));
    }
}
