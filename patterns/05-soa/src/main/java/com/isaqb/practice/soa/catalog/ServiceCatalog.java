package com.isaqb.practice.soa.catalog;

import java.util.HashMap;
import java.util.Map;

/**
 * PipelineForge's central place to register and discover service implementations by a
 * stable (name, version) contract key. Callers never construct a service directly - they
 * ask the catalog for e.g. "deployment-approval v1" and get back whatever's registered
 * under that key. That indirection is what lets multiple, unrelated callers reuse the
 * same service instance without depending on its concrete class - the whole point of SOA's
 * contract-first reuse, exercised here without any networking involved.
 */
public final class ServiceCatalog {

    private final Map<ServiceKey, Object> services = new HashMap<>();

    /**
     * Registers {@code implementation} under the given name and version, checked against
     * {@code contract}.
     *
     * @throws IllegalStateException if something is already registered under this exact
     *     (name, version) - registering the same key twice is almost always a bug (e.g. two
     *     different implementations of "v1" accidentally both wired up), so this must fail
     *     loudly rather than silently overwrite.
     */
    public <T> void register(String name, String version, Class<T> contract, T implementation) {
        // TODO:
        //  1. Build a ServiceKey from name and version.
        //  2. If `services` already has an entry for that key, throw IllegalStateException
        //     with a message that names the offending (name, version).
        //  3. Otherwise store `implementation` under that key. `contract.cast(implementation)`
        //     is a good habit here even though the compiler already guarantees the type -
        //     it documents that `contract` is meaningful, not just a formality.
        var serviceKey = new ServiceKey(name, version);
        if (services.containsKey(serviceKey)) {
            throw new IllegalStateException(String.format("Service %s already exists", serviceKey));
        } else {
            services.put(serviceKey, contract.cast(implementation));
        }
    }

    /**
     * Looks up whatever was registered under (name, version), cast to {@code contract}.
     *
     * @throws ServiceNotFoundException if nothing is registered under that exact
     *     (name, version).
     */
    public <T> T lookup(String name, String version, Class<T> contract) {
        // TODO:
        //  1. Build a ServiceKey from name and version.
        //  2. Look it up in `services`.
        //  3. If absent, throw ServiceNotFoundException with a message that names the
        //     missing (name, version) - this is the failure a consumer sees when the
        //     catalog was never populated, or they typo'd the version string.
        //  4. If present, return it cast to `contract`. `contract.cast(...)` does this
        //     safely and gives a clear ClassCastException message if it's ever wrong,
        //     instead of a confusing one further downstream.

        var serviceKey =  new  ServiceKey(name, version);

        if (services.isEmpty() || !services.containsKey(serviceKey)) {
            throw new ServiceNotFoundException(String.format("service not found: %s", serviceKey));
        }
        return contract.cast(services.get(serviceKey));
    }

}
