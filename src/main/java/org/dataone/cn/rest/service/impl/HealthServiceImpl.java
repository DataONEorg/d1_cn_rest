package org.dataone.cn.rest.service.impl;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.dataone.cn.rest.service.HealthService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
@Service("healthServiceImpl")
@Qualifier("healthService")
public class HealthServiceImpl implements HealthService {
	
	/**
	 * <p>Returns a status report for all of the registered Member Nodes. 
    The information returned is used to give administrators information 
    necessary to maintain the infrastructure.</p
    
    	<p>  Used by Use Case 10 “MN Status Reports” for V0.3. 
    However instead of a complete set of detailed status reports, 
    the V0.3 implementation of the use case will utilize the MN_
    health_0_3.heartbeat() method to compile very basic information 
    about the MNs. Later versions of this method will use the richer 
    MN_health.getStatus(token) method.</p>



	 * @param token
	 * @return Status report, ideally in XML + stylsheet or at least XHTML.
	 * @throws Exception
	 */
	@Override
	public InputStream generateReport(Object token) throws Exception {
		// TODO Auto-generated method stub
		return new ByteArrayInputStream("generateReport Not implemented Yet!".getBytes("UTF8"));
		//throw new Exception("generateReport Not implemented Yet!");
	}

}
