package org.dataone.cn.rest.service.impl;

import org.dataone.cn.rest.service.HealthService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
@Service("healthServiceImpl")
@Qualifier("healthService")
public class HealthServiceImpl implements HealthService {

	@Override
	public Object generateReport(Object token) throws Exception {
		// TODO Auto-generated method stub
		throw new Exception("generateReport Not implemented Yet!");
	}

}
