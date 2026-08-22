package com.isaqb.practice.portsandadapters.core.port;

import com.isaqb.practice.portsandadapters.core.ApprovalDecision;
import com.isaqb.practice.portsandadapters.core.ApprovalRequest;

/**
 * The driving (primary) port: the one entry point the core exposes to whatever calls
 * it - a CLI today, conceivably an HTTP handler or a chat-bot tomorrow. The core
 * defines this interface; driving adapters depend on it, never the other way around.
 * Any number of driving adapters can call the same port without the core knowing how
 * many there are or what they look like.
 */
public interface RequestApprovalUseCase {

    ApprovalDecision decide(ApprovalRequest request);
}
