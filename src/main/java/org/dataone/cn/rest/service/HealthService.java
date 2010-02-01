package org.dataone.cn.rest.service;

public interface HealthService {
	
	/**
	 * 

    Used by Use Case 10 “MN Status Reports” for V0.3. 
    However instead of a complete set of detailed status reports, 
    the V0.3 implementation of the use case will utilize thee the MN_
    health_0_3.heartbeat() method to compile very basic information 
    about the MNs. Later versions of this method will use the richer 
    MN_health.getStatus(token) method.

    Returns a status report for all of the registered Member Nodes. 
    The information returned is used to give administrators information 
    necessary to maintain the infrastructure.

	 * @param token
	 * @return Status report, ideally in XML + stylsheet or at least XHTML.
	 * @throws Exception
	 */
	public Object generateReport(Object token) throws Exception;


}
