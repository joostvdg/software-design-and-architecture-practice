package com.isaqb.practice.soa;

import com.isaqb.practice.soa.catalog.ServiceCatalog;
import com.isaqb.practice.soa.contract.DeploymentApprovalService;
import com.isaqb.practice.soa.contract.EnvironmentProvisioningService;
import com.isaqb.practice.soa.service.DeploymentApprovalServiceImpl;
import com.isaqb.practice.soa.service.EnvironmentProvisioningServiceImpl;

/**
 * Builds and populates the Service Catalog both callers use. In a real deployment this
 * catalog would be a long-running registry process reached over the network; here, every
 * caller obtains an identically-populated catalog from this one factory method instead of
 * constructing a service implementation directly.
 */
public final class CatalogFactory {

    public static final String DEPLOYMENT_APPROVAL = "deployment-approval";
    public static final String ENVIRONMENT_PROVISIONING = "environment-provisioning";
    public static final String V1 = "v1";

    private CatalogFactory() {}

    public static ServiceCatalog createDefault() {
        ServiceCatalog serviceCatalog = new ServiceCatalog();
        serviceCatalog.register(
                DEPLOYMENT_APPROVAL, V1, DeploymentApprovalService.class, new DeploymentApprovalServiceImpl()
        );
        serviceCatalog.register(
                ENVIRONMENT_PROVISIONING, V1, EnvironmentProvisioningService.class, new EnvironmentProvisioningServiceImpl()
        );
        return serviceCatalog;
    }
}
