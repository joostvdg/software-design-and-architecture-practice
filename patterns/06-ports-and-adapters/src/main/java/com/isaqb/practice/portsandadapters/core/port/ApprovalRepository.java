package com.isaqb.practice.portsandadapters.core.port;

import com.isaqb.practice.portsandadapters.core.ApprovalDecision;
import com.isaqb.practice.portsandadapters.core.ApprovalRequest;

import java.util.List;

/**
 * The driven (secondary) port: something the core needs done but does not know how to
 * do itself. The core defines this interface too - driven adapters (in-memory, a
 * file, a real database) implement it. Notice the dependency arrow points the same
 * way as the driving port: adapters depend on core's interface, core never depends on
 * an adapter, regardless of which side of the hexagon we're talking about.
 */
public interface ApprovalRepository {

    void save(ApprovalDecision decision);

    List<ApprovalDecision> findByRequester(String requester);
}
