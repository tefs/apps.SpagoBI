/**

SpagoBI - The Business Intelligence Free Platform

Copyright (C) 2005-2009 Engineering Ingegneria Informatica S.p.A.

This library is free software; you can redistribute it and/or
modify it under the terms of the GNU Lesser General Public
License as published by the Free Software Foundation; either
version 2.1 of the License, or (at your option) any later version.

This library is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
Lesser General Public License for more details.

You should have received a copy of the GNU Lesser General Public
License along with this library; if not, write to the Free Software
Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA

**/
package it.eng.spagobi.kpi.model.service;

import it.eng.spago.error.EMFUserError;
import it.eng.spagobi.analiticalmodel.document.x.AbstractSpagoBIAction;
import it.eng.spagobi.chiron.serializer.SerializerFactory;
import it.eng.spagobi.commons.dao.DAOFactory;
import it.eng.spagobi.kpi.model.bo.Model;
import it.eng.spagobi.kpi.model.bo.ModelExtended;
import it.eng.spagobi.kpi.model.dao.IModelDAO;
import it.eng.spagobi.utilities.exceptions.SpagoBIServiceException;
import it.eng.spagobi.utilities.service.JSONSuccess;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class ManageModelsAction extends AbstractSpagoBIAction {
	/**
	 * 
	 */
	private static final long serialVersionUID = -8920524215721282986L;
	// logger component
	private static Logger logger = Logger.getLogger(ManageModelsAction.class);
	private final String MESSAGE_DET = "MESSAGE_DET";
	// type of service
	private final String MODELS_LIST = "MODELS_LIST";
	private final String MODEL_NODES_LIST = "MODEL_NODES_LIST";
	private final String MODEL_NODES_SAVE = "MODEL_NODES_SAVE";
	private final String MODEL_NODE_DELETE = "MODEL_NODE_DELETE";
	private final String MODEL_NODES_LIST_WITH_KPI = "MODEL_NODES_LIST_WITH_KPI";

	private final String KPI_DOMAIN_TYPE = "KPI_TYPE";
	private final String METRIC_SCALE_DOMAIN_TYPE = "METRIC_SCALE_TYPE";
	private final String MEASURE_DOMAIN_TYPE = "MEASURE_TYPE";
	private final String THRESHOLD_DOMAIN_TYPE = "THRESHOLD_TYPE";
	private final String THRESHOLD_SEVERITY_TYPE = "SEVERITY";
	
	private final String MODEL_DOMAIN_TYPE_ROOT = "MODEL_ROOT";
	private final String MODEL_DOMAIN_TYPE_NODE = "MODEL_NODE";
	

	
	private final String NODES_TO_SAVE = "nodes";



	@Override
	public void doService() {
		logger.debug("IN");
		IModelDAO modelDao;
		try {
			modelDao = DAOFactory.getModelDAO();
		} catch (EMFUserError e1) {
			logger.error(e1.getMessage(), e1);
			throw new SpagoBIServiceException(SERVICE_NAME,	"Error occurred");
		}
		Locale locale = getLocale();

		String serviceType = this.getAttributeAsString(MESSAGE_DET);
		logger.debug("Service type "+serviceType);
		
		if (serviceType != null && serviceType.equalsIgnoreCase(MODELS_LIST)) {
			
			try {				
				List modelRootsList = modelDao.loadModelsRoot();
				
				logger.debug("Loaded models list");
				JSONArray modelsListJSON = (JSONArray) SerializerFactory.getSerializer("application/json").serialize(modelRootsList,locale);
				JSONObject modelsResponseJSON = createJSONResponseModelsList(modelsListJSON, new Integer(6));

				writeBackToClient(new JSONSuccess(modelsResponseJSON));

			} catch (Throwable e) {
				logger.error("Exception occurred while retrieving model tree", e);
				throw new SpagoBIServiceException(SERVICE_NAME,
						"Exception occurred while retrieving model tree", e);
			}
		  }else if (serviceType != null && serviceType.equalsIgnoreCase(MODEL_NODES_LIST)) {
			
			try {	
				
				String parentId = (String)getAttributeAsString("modelId");
				if(parentId == null || parentId.startsWith("xnode")){
					writeBackToClient(new JSONSuccess("OK"));
					return;
				}
				Model aModel = modelDao.loadModelWithChildrenById(Integer.parseInt(parentId));
				
				logger.debug("Loaded model tree");
				JSONArray modelChildrenJSON = (JSONArray) SerializerFactory.getSerializer("application/json").serialize(aModel.getChildrenNodes(),	locale);
				writeBackToClient(new JSONSuccess(modelChildrenJSON));

			} catch (Throwable e) {
				logger.error("Exception occurred while retrieving model tree", e);
				throw new SpagoBIServiceException(SERVICE_NAME,
						"Exception occurred while retrieving model tree", e);
			}
		}else if (serviceType != null && serviceType.equalsIgnoreCase(MODEL_NODES_LIST_WITH_KPI)) {
			
			try {	
				
				String parentId = (String)getAttributeAsString("modelId");
				if(parentId == null || parentId.startsWith("xnode")){
					writeBackToClient(new JSONSuccess("OK"));
					return;
				}
				Model dbModel = modelDao.loadModelWithChildrenById(Integer.parseInt(parentId));
				
				ModelExtended aModel = new ModelExtended(dbModel);
				
				aModel.setExtendedChildrenNodes(dbModel.getChildrenNodes());
				
				logger.debug("Loaded model tree");
				JSONArray modelChildrenJSON = (JSONArray) SerializerFactory.getSerializer("application/json").serialize(aModel.getExtendedChildrenNodes(),	locale);
				writeBackToClient(new JSONSuccess(modelChildrenJSON));

			} catch (Throwable e) {
				logger.error("Exception occurred while retrieving model tree", e);
				throw new SpagoBIServiceException(SERVICE_NAME,
						"Exception occurred while retrieving model tree", e);
			}
		} else if (serviceType != null	&& serviceType.equalsIgnoreCase(MODEL_NODES_SAVE)) {
			JSONArray nodesToSaveJSON = getAttributeAsJSONArray(NODES_TO_SAVE);
			List<Model> modelNodes = null;
			if(nodesToSaveJSON != null){
				try {
					modelNodes = deserializeNodesJSONArray(nodesToSaveJSON);
					
					//save them
					JSONObject response = saveModelNodes(modelNodes);
					writeBackToClient(new JSONSuccess(response));
					
				} catch (Exception e) {
					logger.error(e.getMessage(), e);
					writeErrorsBackToClient();
					throw new SpagoBIServiceException(SERVICE_NAME,
							"Exception saving model nodes", e);
				}
			}
			
		} else if (serviceType != null	&& serviceType.equalsIgnoreCase(MODEL_NODE_DELETE)) {
			
			Integer modelId = getAttributeAsInteger("modelId");
			try {
				boolean result = DAOFactory.getModelDAO().deleteModel(modelId);
				logger.debug("Model deleted");
				writeBackToClient( new JSONSuccess("Operation succeded") );
			} catch (Throwable e) {
				logger.error("Exception occurred while retrieving model to delete", e);
				throw new SpagoBIServiceException(SERVICE_NAME,
						"Exception occurred while retrieving model to delete", e);
			}
			
			
		}else if(serviceType == null){
			try {
				List nodeTypesNodes = DAOFactory.getDomainDAO().loadListDomainsByType(MODEL_DOMAIN_TYPE_NODE);
				List nodeTypesRoot = DAOFactory.getDomainDAO().loadListDomainsByType(MODEL_DOMAIN_TYPE_ROOT);
				List nodeTypes = new ArrayList();
				nodeTypes.addAll(nodeTypesNodes);
				nodeTypes.addAll(nodeTypesRoot);
				getSessionContainer().setAttribute("nodeTypesList", nodeTypes);
				List kpiTypesList = DAOFactory.getDomainDAO().loadListDomainsByType(KPI_DOMAIN_TYPE);
				getSessionContainer().setAttribute("kpiTypesList", kpiTypesList);
				List thrSeverityTypes = DAOFactory.getDomainDAO().loadListDomainsByType(THRESHOLD_SEVERITY_TYPE);
				getSessionContainer().setAttribute("thrSeverityTypes", thrSeverityTypes);
				List measureTypesList = DAOFactory.getDomainDAO().loadListDomainsByType(MEASURE_DOMAIN_TYPE);
				getSessionContainer().setAttribute("measureTypesList", measureTypesList);
				List metricScaleTypesList = DAOFactory.getDomainDAO().loadListDomainsByType(METRIC_SCALE_DOMAIN_TYPE);
				getSessionContainer().setAttribute("metricScaleTypesList", metricScaleTypesList);
				List thrTypesList = DAOFactory.getDomainDAO().loadListDomainsByType(THRESHOLD_DOMAIN_TYPE);
				getSessionContainer().setAttribute("thrTypesList", thrTypesList);
				
			} catch (EMFUserError e) {
				logger.error(e.getMessage(), e);
				throw new SpagoBIServiceException(SERVICE_NAME,
						"Exception retrieving model types", e);
			}
		}
		logger.debug("OUT");

	}

	/**
	 * Creates a json array with children users informations
	 * 
	 * @param rows
	 * @return
	 * @throws JSONException
	 */
	private JSONObject createJSONResponseModelsList(JSONArray rows, Integer totalModelsNumber)throws JSONException {
		JSONObject results;
		results = new JSONObject();
		results.put("total", totalModelsNumber);
		results.put("title", "ModelsList");
		results.put("rows", rows);
		return results;
	}
	private List<Model> deserializeNodesJSONArray(JSONArray rows) throws JSONException{
		List<Model> toReturn = new ArrayList<Model>();
		for(int i=0; i< rows.length(); i++){
			
			JSONObject obj = (JSONObject)rows.get(i);

			Model model = new Model();
			//always present guiId
			String guiId = obj.getString("id");
			model.setGuiId(guiId);

			try{
				model.setId(obj.getInt("modelId"));
			}catch(Throwable t){
				//nothing
				model.setId(null);
			}
			
			try{
				model.setParentId(obj.getInt("parentId"));
			}catch(Throwable t){
				//nothing
				model.setParentId(null);
			}
			try{
				model.setCode(obj.getString("code"));
				model.setDescription(obj.getString("description"));
				model.setLabel(obj.getString("label"));
				model.setName(obj.getString("name"));
				model.setTypeCd(obj.getString("type"));
				model.setTypeId(obj.getInt("typeId"));
				try{
					model.setTypeDescription(obj.getString("typeDescr"));
				}catch(Throwable t){
					//nothing
					model.setTypeDescription(null);
				}
				try{
					model.setKpiId(obj.getInt("kpiId"));
				}catch(Throwable t){
					//nothing
					model.setKpiId(null);
				}
				String value = obj.getString("toSave");
			}catch(Throwable t){
				logger.debug("Deserialization error on node: "+guiId);
			}
			toReturn.add(model);
		}	
		return toReturn;
	}
	
	private JSONObject saveModelNodes(List<Model> nodesToSave) throws JSONException{
		JSONArray errorNodes = new JSONArray();
		
		JSONObject respObj = new JSONObject();
		
		//loop over nodes and order them ascending
		TreeMap<Integer, Model> treeMap = new TreeMap<Integer, Model>();
		for(int i= 0; i<nodesToSave.size(); i++){
			
			Model model = (Model)nodesToSave.get(i);
			//loads all nodes guiid with type error
			
			respObj.put(model.getGuiId(), "OK");
			
			if(model.getParentId() != null){
				//look up for its id: if null --> newly created node
				Integer id = model.getId();
				if(id == null){
					treeMap.put(Integer.valueOf("-"+i+1), model);
				}else{
				//else to modify node
					treeMap.put(model.getId(), model);
				}
				
			}else{
				//root node --> save first
				try {
					if(model.getId()  != null){
						DAOFactory.getModelDAO().modifyModel(model);
						respObj.put(model.getGuiId(), model.getId());
					}else{
						Integer index = DAOFactory.getModelDAO().insertModel(model);
						respObj.put(model.getGuiId(), index);
					}
				} catch (Exception e) {
					//send error!!!		
					respObj.put(model.getGuiId(), "KO");
					
				}
			}
		}
		
		Set set = treeMap.entrySet();
		// Get an iterator
		Iterator it = set.iterator(); 
		//loop again over treemap
		while(it.hasNext()) {
			Map.Entry orderedEntry = (Map.Entry)it.next();
			//check that parent exists
			Model orderedNode = (Model)orderedEntry.getValue();
			
			//GET JSON OBJECT VALUE
			Integer parentId = orderedNode.getParentId();
			try {
				Model parent = DAOFactory.getModelDAO().loadModelWithoutChildrenById(parentId);
				if(parent != null){						
					//if parent exists--> save					
					//if node id is negative --> insert
					if(orderedNode.getId() == null){
						Integer newId = DAOFactory.getModelDAO().insertModel(orderedNode);
						if (newId != null){
							orderedNode.setId(newId);
							respObj.put(orderedNode.getGuiId(), newId);
						}else{						
							respObj.put(orderedNode.getGuiId(), "KO");
						}
					}else{
					//else update
						DAOFactory.getModelDAO().modifyModel(orderedNode);
						respObj.put(orderedNode.getGuiId(), orderedNode.getId());
					}
					
				}
			} catch (Exception e) {
				//if parentId != null but no parent node stored on db --> exception
				respObj.put(orderedNode.getGuiId(), "KO");
			}

		} 
		return respObj;
	}
}
