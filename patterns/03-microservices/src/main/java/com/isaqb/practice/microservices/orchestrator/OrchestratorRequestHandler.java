package com.isaqb.practice.microservices.orchestrator;

/**
 * The Orchestrator's core request logic, kept free of any HttpExchange/HttpServer
 * detail, same as RegistryRequestHandler on the Registry side.
 */
public final class OrchestratorRequestHandler {

    private final PipelineRunStore store;
    private final RegistryClient registryClient;

    public OrchestratorRequestHandler(final PipelineRunStore store, final RegistryClient registryClient) {
        this.store = store;
        this.registryClient = registryClient;
    }

    /**
     * Handles "run `runId` finished". `body` is "name=...&version=...&digest=..."
     * describing the artifact that run produced (same wire format as the Registry's,
     * parsed independently here rather than importing ArtifactWireFormat - see
     * milestone 4's note on not sharing code between the two services).
     *
     * Must:
     *  - parse `body` into name/version/digest (split on '&', then each segment on the
     *    first '=' - the same idea as ArtifactWireFormat.decode, written again here)
     *  - if any of the three fields is missing or blank, return
     *    HttpResult(400, "status=error&message=<short reason>") - and do NOT call the
     *    registry in this case
     *  - otherwise: save a COMPLETED PipelineRun for `runId` (with those three fields)
     *    via `store`, then call registryClient.register(name, version, digest)
     *  - if the registry call succeeds: return
     *    HttpResult(200, "status=completed&runId=" + runId)
     *  - if registryClient.register throws RegistryUnavailableException: return
     *    HttpResult(502, "status=error&message=registry unavailable: " + e.getMessage())
     *    - note the run stays saved as COMPLETED in the Orchestrator's own store even
     *    though telling the Registry failed. That gap between "my own state says done"
     *    and "the other service actually knows" is the eventual-consistency trade-off
     *    from the README, not a bug to paper over.
     */
    public HttpResult handleComplete(String runId, String body) {
        // TODO: implement per the contract above.
        var artifactSegments = body.split("&");
        if  (artifactSegments.length != 3) {
            return new HttpResult(400, "status=error&message=incomplete artifact body");
        }
        // name/version/digest
        var nameSegment = artifactSegments[0].split("=");
        var versionSegment = artifactSegments[1].split("=");
        var digestSegment = artifactSegments[2].split("=");
        if (nameSegment.length != 2 || versionSegment.length != 2 || digestSegment.length != 2) {
            return new HttpResult(400, "status=error&message=incomplete artifact body");
        }
        String name = nameSegment[1];
        String version = versionSegment[1];
        String digest = digestSegment[1];

        var pipelineRun = new PipelineRun(runId, RunStatus.COMPLETED, name, version, digest);
        store.save(pipelineRun);

        try {
            registryClient.register(name, version, digest);
        } catch (RegistryUnavailableException e) {
            return new HttpResult(502, "status=error&message=registry unavailable");
        }
        return new HttpResult(200, "status=success");
    }

}

