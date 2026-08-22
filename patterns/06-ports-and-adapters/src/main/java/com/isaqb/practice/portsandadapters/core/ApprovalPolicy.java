package com.isaqb.practice.portsandadapters.core;

import java.util.List;

/**
 * The core decision-rule logic: what makes an approval request acceptable at all,
 * independent of who is asking (CLI, HTTP, chat-bot) or where decisions end up stored
 * (memory, file, database). Returns every violated rule as a human-readable message;
 * an empty list means the request is policy-compliant.
 */
public interface ApprovalPolicy {

    List<String> violations(ApprovalRequest request);
}
