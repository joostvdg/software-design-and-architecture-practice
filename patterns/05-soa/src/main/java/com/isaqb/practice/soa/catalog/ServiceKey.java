package com.isaqb.practice.soa.catalog;

/** The catalog's lookup key: a service is identified by name AND version, not name alone. */
record ServiceKey(String name, String version) {
}
