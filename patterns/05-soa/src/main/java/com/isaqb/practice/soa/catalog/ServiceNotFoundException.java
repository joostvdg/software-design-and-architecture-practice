package com.isaqb.practice.soa.catalog;

/** Thrown when a consumer looks up a (name, version) the catalog has nothing registered under. */
public class ServiceNotFoundException extends RuntimeException{
    public ServiceNotFoundException(String message) {
        super(message);
    }
}
