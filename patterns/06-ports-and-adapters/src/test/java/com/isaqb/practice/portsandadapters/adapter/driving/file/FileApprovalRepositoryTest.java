package com.isaqb.practice.portsandadapters.adapter.driving.file;

import com.isaqb.practice.portsandadapters.adapter.ApprovalRepositoryContractTest;
import com.isaqb.practice.portsandadapters.core.port.ApprovalRepository;

import java.nio.file.Path;

import org.junit.jupiter.api.io.TempDir;

class FileApprovalRepositoryTest extends ApprovalRepositoryContractTest {

    @TempDir
    Path tempDir;

    @Override
    protected ApprovalRepository newRepository() {
        return new FileApprovalRepository(tempDir.resolve("decisions-" + System.nanoTime() + ".log"));
    }
}