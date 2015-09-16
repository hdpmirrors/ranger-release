/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

 package com.xasecure.service;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.xasecure.biz.XABizUtil;
import com.xasecure.common.ContextUtil;
import com.xasecure.common.SearchCriteria;
import com.xasecure.common.SearchField;
import com.xasecure.common.UserSessionBase;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.xasecure.common.AppConstants;
import com.xasecure.common.view.VTrxLogAttr;
import com.xasecure.db.XADaoManager;
import com.xasecure.entity.*;
import com.xasecure.util.XAEnumUtil;
import com.xasecure.view.*;

@Service
@Scope("singleton")
public class XAuditMapService extends
		XAuditMapServiceBase<XXAuditMap, VXAuditMap> {

	@Autowired
	XAEnumUtil xaEnumUtil;

	@Autowired
	XADaoManager xADaoManager;

	@Autowired
	XABizUtil xaBizUtil;

	@Autowired
	XResourceService xResourceService;

	static HashMap<String, VTrxLogAttr> trxLogAttrs = new HashMap<String, VTrxLogAttr>();
	static {
//		trxLogAttrs.put("groupId", new VTrxLogAttr("groupId", "Group Audit", false));
//		trxLogAttrs.put("userId", new VTrxLogAttr("userId", "User Audit", false));
		trxLogAttrs.put("auditType", new VTrxLogAttr("auditType", "Audit Type", true));
	}

	public XAuditMapService() {
		searchFields.add(new SearchField("resourceId", "obj.resourceId",
				SearchField.DATA_TYPE.INTEGER, SearchField.SEARCH_TYPE.FULL));
	}

	@Override
	protected void validateForCreate(VXAuditMap vObj) {
		// TODO Auto-generated method stub

	}

	@Override
	protected void validateForUpdate(VXAuditMap vObj, XXAuditMap mObj) {
		// TODO Auto-generated method stub

	}

	public List<XXTrxLog> getTransactionLog(VXAuditMap vXAuditMap, String action){
		return getTransactionLog(vXAuditMap, null, action);
	}

	public List<XXTrxLog> getTransactionLog(VXAuditMap vObj, VXAuditMap mObj, String action){
		if(vObj == null && (action == null || !action.equalsIgnoreCase("update"))){
			return null;
		}
		
		List<XXTrxLog> trxLogList = new ArrayList<XXTrxLog>();
		Field[] fields = vObj.getClass().getDeclaredFields();
		
		try {
			for(Field field : fields){
				field.setAccessible(true);
				String fieldName = field.getName();
				if(!trxLogAttrs.containsKey(fieldName)){
					continue;
				}
				
				VTrxLogAttr vTrxLogAttr = trxLogAttrs.get(fieldName);
				
				XXTrxLog xTrxLog = new XXTrxLog();
				xTrxLog.setAttributeName(vTrxLogAttr.getAttribUserFriendlyName());
			
				String value = null;
				boolean isEnum = vTrxLogAttr.isEnum();
				if(isEnum){
					String enumName = XXAuditMap.getEnumName(fieldName);
					int enumValue = field.get(vObj) == null ? 0 : Integer.parseInt(""+field.get(vObj));
					value = xaEnumUtil.getLabel(enumName, enumValue);
				} else {
					value = ""+field.get(vObj);
					XXUser xUser = xADaoManager.getXXUser().getById(Long.parseLong(value));
					value = xUser.getName();
				}
				
				if(action.equalsIgnoreCase("create")){
					xTrxLog.setNewValue(value);
				} else if(action.equalsIgnoreCase("delete")){
					xTrxLog.setPreviousValue(value);
				} else if(action.equalsIgnoreCase("update")){
					// Not Changed.
					xTrxLog.setNewValue(value);
					xTrxLog.setPreviousValue(value);
				}
				
				xTrxLog.setAction(action);
				xTrxLog.setObjectClassType(AppConstants.CLASS_TYPE_XA_AUDIT_MAP);
				xTrxLog.setObjectId(vObj.getId());
				xTrxLog.setParentObjectClassType(AppConstants.CLASS_TYPE_XA_RESOURCE);
				xTrxLog.setParentObjectId(vObj.getResourceId());
//				xTrxLog.setParentObjectName(vObj.get);
//				xTrxLog.setObjectName(objectName);
				trxLogList.add(xTrxLog);
				
			}
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		}
		
		return trxLogList;
	}

	@Override
	protected XXAuditMap mapViewToEntityBean(VXAuditMap vObj, XXAuditMap mObj, int OPERATION_CONTEXT) {
		super.mapViewToEntityBean(vObj, mObj, OPERATION_CONTEXT);
		if(vObj!=null && mObj!=null){
			XXPortalUser xXPortalUser=null;
			if(mObj.getAddedByUserId()==null || mObj.getAddedByUserId()==0){
				if(!stringUtil.isEmpty(vObj.getOwner())){
					xXPortalUser=xADaoManager.getXXPortalUser().findByLoginId(vObj.getOwner());	
					if(xXPortalUser!=null){
						mObj.setAddedByUserId(xXPortalUser.getId());
					}
				}
			}
			if(mObj.getUpdatedByUserId()==null || mObj.getUpdatedByUserId()==0){
				if(!stringUtil.isEmpty(vObj.getUpdatedBy())){
					xXPortalUser= xADaoManager.getXXPortalUser().findByLoginId(vObj.getUpdatedBy());			
					if(xXPortalUser!=null){
						mObj.setUpdatedByUserId(xXPortalUser.getId());
					}		
				}
			}
		}
		return mObj;
	}

	@Override
	protected VXAuditMap mapEntityToViewBean(VXAuditMap vObj, XXAuditMap mObj) {
		super.mapEntityToViewBean(vObj, mObj);
		if(mObj!=null && vObj!=null){
			XXPortalUser xXPortalUser=null;
			if(stringUtil.isEmpty(vObj.getOwner())){
				xXPortalUser= xADaoManager.getXXPortalUser().getById(mObj.getAddedByUserId());	
				if(xXPortalUser!=null){
					vObj.setOwner(xXPortalUser.getLoginId());
				}
			}
			if(stringUtil.isEmpty(vObj.getUpdatedBy())){
				xXPortalUser= xADaoManager.getXXPortalUser().getById(mObj.getUpdatedByUserId());		
				if(xXPortalUser!=null){
					vObj.setUpdatedBy(xXPortalUser.getLoginId());
				}
			}
		}
		return vObj;
	}

	@Override
	public VXAuditMapList searchXAuditMaps(SearchCriteria searchCriteria) {

		VXAuditMapList returnList;
		UserSessionBase currentUserSession = ContextUtil.getCurrentUserSession();
		// If user is system admin
		if (currentUserSession.isUserAdmin()) {
			returnList = super.searchXAuditMaps(searchCriteria);
		} else {
			returnList = new VXAuditMapList();
			int startIndex = searchCriteria.getStartIndex();
			int pageSize = searchCriteria.getMaxRows();
			searchCriteria.setStartIndex(0);
			searchCriteria.setMaxRows(Integer.MAX_VALUE);
			List<XXAuditMap> resultList = (List<XXAuditMap>) searchResources(searchCriteria, searchFields, sortFields, returnList);

			List<XXAuditMap> adminAuditResourceList = new ArrayList<XXAuditMap>();
			for (XXAuditMap xXAuditMap : resultList) {
				XXResource xRes = daoManager.getXXResource().getById(xXAuditMap.getResourceId());
				VXResponse vXResponse = xaBizUtil.hasPermission(xResourceService.populateViewBean(xRes), AppConstants.XA_PERM_TYPE_ADMIN);
				if (vXResponse.getStatusCode() == VXResponse.STATUS_SUCCESS) {
					adminAuditResourceList.add(xXAuditMap);
				}
			}

			if (adminAuditResourceList.size() > 0) {
				populatePageList(adminAuditResourceList, startIndex, pageSize, returnList);
			}
		}

		return returnList;
	}

	private void populatePageList(List<XXAuditMap> auditMapList, int startIndex, int pageSize, VXAuditMapList vxAuditMapList) {
		List<VXAuditMap> onePageList = new ArrayList<VXAuditMap>();
		for (int i = startIndex; i < pageSize + startIndex && i < auditMapList.size(); i++) {
			VXAuditMap vXAuditMap = populateViewBean(auditMapList.get(i));
			onePageList.add(vXAuditMap);
		}
		vxAuditMapList.setVXAuditMaps(onePageList);
		vxAuditMapList.setStartIndex(startIndex);
		vxAuditMapList.setPageSize(pageSize);
		vxAuditMapList.setResultSize(onePageList.size());
		vxAuditMapList.setTotalCount(auditMapList.size());
	}

}
