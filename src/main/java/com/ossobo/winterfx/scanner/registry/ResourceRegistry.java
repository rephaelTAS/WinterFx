package com.ossobo.winterfx.scanner.registry;

import com.ossobo.winterfx.resources.descriptor.ResourceDescriptor;
import com.ossobo.winterfx.resources.enums.ResourceOrigin;
import com.ossobo.winterfx.resources.enums.ResourceType;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * Catálogo central de recursos do WinterFX.
 * Thread-safe: usa {@link ConcurrentHashMap}.
 */
public final class ResourceRegistry {

    private final Map<String, ResourceDescriptor> descriptors = new ConcurrentHashMap<>();
    private final AtomicLong registrationCount = new AtomicLong(0);
    private final AtomicLong unregistrationCount = new AtomicLong(0);
    private final AtomicLong overwriteCount = new AtomicLong(0);

    public void register(ResourceDescriptor descriptor) {
        Objects.requireNonNull(descriptor, "Descriptor não pode ser nulo");

        String id = descriptor.id();
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("ID do descriptor não pode ser nulo ou vazio");
        }

        boolean wasOverwritten = descriptors.put(id, descriptor) != null;
        registrationCount.incrementAndGet();

        if (wasOverwritten) {
            overwriteCount.incrementAndGet();
        }
    }

    public void registerAll(ResourceDescriptor... descriptors) {
        for (var descriptor : descriptors) {
            register(descriptor);
        }
    }

    public void registerAll(Collection<? extends ResourceDescriptor> descriptors) {
        for (var descriptor : descriptors) {
            register(descriptor);
        }
    }

    public Optional<ResourceDescriptor> findById(String id) {
        return Optional.ofNullable(descriptors.get(id));
    }

    public Optional<ResourceDescriptor> findByIdAndType(String id, ResourceType type) {
        return findById(id).filter(d -> d.resourceType() == type);
    }

    public Optional<ResourceDescriptor> findByIdTypeAndOrigin(String id, ResourceType type, ResourceOrigin origin) {
        return findByIdAndType(id, type).filter(d -> d.origin() == origin);
    }

    public boolean contains(String id) {
        return descriptors.containsKey(id);
    }

    public boolean contains(String id, ResourceType type) {
        return findByIdAndType(id, type).isPresent();
    }

    public List<ResourceDescriptor> findAll() {
        return List.copyOf(descriptors.values());
    }

    public List<ResourceDescriptor> findAllByType(ResourceType type) {
        return descriptors.values().stream()
                .filter(d -> d.resourceType() == type)
                .collect(Collectors.toUnmodifiableList());
    }

    public Set<String> getAllIds() {
        return Collections.unmodifiableSet(descriptors.keySet());
    }

    public boolean unregister(String id) {
        var removed = descriptors.remove(id);
        if (removed != null) {
            unregistrationCount.incrementAndGet();
            return true;
        }
        return false;
    }

    public void clear() {
        descriptors.clear();
    }

    public int count() {
        return descriptors.size();
    }

    public boolean isEmpty() {
        return descriptors.isEmpty();
    }

    public Map<ResourceType, Long> getStatistics() {
        return descriptors.values().stream()
                .collect(Collectors.groupingBy(
                        ResourceDescriptor::resourceType,
                        Collectors.counting()
                ));
    }

    public long getRegistrationCount() {
        return registrationCount.get();
    }

    public long getUnregistrationCount() {
        return unregistrationCount.get();
    }

    public long getOverwriteCount() {
        return overwriteCount.get();
    }
}