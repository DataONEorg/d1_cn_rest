package org.dataone.cn.rest.service.impl;

import java.util.Date;

import org.dataone.cn.rest.service.QueryService;
import org.dataone.ns.core.objects.Response;
import org.springframework.stereotype.Service;
@Service("queryServiceImpl")
public class QueryServiceImpl implements QueryService {

	@Override
	public Response getLogRecords(Object token, Date fromDate, Date toDate)
			throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Response search(Object token, Object query) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

}
