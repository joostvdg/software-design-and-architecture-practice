package com.isaqb.practice.portsandadapters.adapter.driving.memory;

import com.isaqb.practice.portsandadapters.adapter.ApprovalRepositoryContractTest;
import com.isaqb.practice.portsandadapters.core.port.ApprovalRepository;

class InMemoryApprovalRepositoryTest extends ApprovalRepositoryContractTest {

    @Override
    protected ApprovalRepository newRepository() {
        return new InMemoryApprovalRepository();
    }
}