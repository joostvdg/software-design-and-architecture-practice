package com.isaqb.practice.soa.catalog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class ServiceCatalogTest {

    interface Greeter {
        String greet();
    }

    @Test
    void lookupReturnsWhatWasRegistered() {
        ServiceCatalog catalog = new ServiceCatalog();
        Greeter greeter = () -> "hi";

        catalog.register("greeter", "v1", Greeter.class, greeter);

        assertEquals("hi", catalog.lookup("greeter", "v1", Greeter.class).greet());
    }

    @Test
    void lookupThrowsWhenNothingRegisteredUnderKey() {
        ServiceCatalog catalog = new ServiceCatalog();

        assertThrows(
                ServiceNotFoundException.class, () -> catalog.lookup("missing", "v1", Greeter.class));
    }

    @Test
    void differentVersionsOfSameNameAreDistinctEntries() {
        ServiceCatalog catalog = new ServiceCatalog();
        catalog.register("greeter", "v1", Greeter.class, () -> "v1-hi");
        catalog.register("greeter", "v2", Greeter.class, () -> "v2-hi");

        assertEquals("v1-hi", catalog.lookup("greeter", "v1", Greeter.class).greet());
        assertEquals("v2-hi", catalog.lookup("greeter", "v2", Greeter.class).greet());
    }

    @Test
    void registeringSameKeyTwiceThrows() {
        ServiceCatalog catalog = new ServiceCatalog();
        catalog.register("greeter", "v1", Greeter.class, () -> "hi");

        assertThrows(
                IllegalStateException.class,
                () -> catalog.register("greeter", "v1", Greeter.class, () -> "again"));
    }
}