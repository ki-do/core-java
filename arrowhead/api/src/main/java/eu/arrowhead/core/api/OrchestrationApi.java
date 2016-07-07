package eu.arrowhead.core.api;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import eu.arrowhead.common.configuration.DatabaseManager;
import eu.arrowhead.common.database.OrchestrationStore;
import eu.arrowhead.common.exception.BadPayloadException;
import eu.arrowhead.common.exception.DataNotFoundException;
import eu.arrowhead.common.model.ArrowheadCloud;
import eu.arrowhead.common.model.ArrowheadService;
import eu.arrowhead.common.model.ArrowheadSystem;
import eu.arrowhead.common.model.messages.StorePayload;

@Path("orchestration")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class OrchestrationApi {

	DatabaseManager dm = DatabaseManager.getInstance();
	HashMap<String, Object> restrictionMap = new HashMap<String, Object>();
	
	@GET
	@Produces(MediaType.TEXT_PLAIN)
	public String getIt() {
		return "Got it";
	}
	
	/**
	 * Returns an Orchestration Store entry from the database specified by the
	 * name of the entry.
	 * 
	 * @param {String} - name
	 * @return OrchestrationStore
	 * @throws DataNotFoundException
	 */
	@GET
	@Path("store/{name}")
	public Response getStoreEntry(@PathParam("name") String name){
		restrictionMap.put("name", name);
		OrchestrationStore entry = dm.get(OrchestrationStore.class, restrictionMap);
		if(entry == null){
			throw new DataNotFoundException("Requested store entry was not found in the database.");
		}
		else{
			return Response.ok(entry).build();
		}
	}
	
	/**
	 * Returns the Orchestration Store entries from the database specified by
	 * the consumer and/or the service. If the payload is empty ({}), all entries
	 * will be returned.
	 * 
	 * @param {StorePayload}
	 *            payload
	 * @return List<OrchestrationStore>
	 * @throws DataNotFoundException
	 */
	@PUT
	@Path("/store")
	public Response getStoreEntries(StorePayload payload) {
		
		List<OrchestrationStore> store = new ArrayList<OrchestrationStore>();
		HashMap<String, Object> rm = new HashMap<String, Object>();
		if(payload.getConsumer() != null){
			if (payload.getConsumer().getSystemGroup() != null &&
					payload.getConsumer().getSystemName() != null) {
				rm.put("systemGroup", payload.getConsumer().getSystemGroup());
				rm.put("systemName", payload.getConsumer().getSystemName());
				ArrowheadSystem consumer = dm.get(ArrowheadSystem.class, rm);
				restrictionMap.put("consumer", consumer);
			}
		}
		
		rm.clear();
		
		if(payload.getService() != null){
			if (payload.getService().getServiceGroup() != null &&
					payload.getService().getServiceDefinition() != null) {
				rm.put("serviceGroup", payload.getService().getServiceGroup());
				rm.put("serviceDefinition", payload.getService().getServiceDefinition());
				ArrowheadService service = dm.get(ArrowheadService.class, rm);
				restrictionMap.put("service", service);
			}
		}
		
		store = dm.getAll(OrchestrationStore.class, restrictionMap);
		if(store.isEmpty()){
			throw new DataNotFoundException("Store entries specified by the payload "
					+ "were not found in the database.");
		}

		GenericEntity<List<OrchestrationStore>> entity = 
				new GenericEntity<List<OrchestrationStore>>(store) {};
		return Response.ok(entity).build();
	}
	
	/**
	 * 
	 * @param {StorePayload} - payload
	 * @return 
	 * @throws BadPayloadException, DataNotFoundException
	 */
	@PUT
	@Path("store/active")
	public Response setActiveEntry(StorePayload payload){
		
		if(!payload.isPayloadUsable()){
			throw new BadPayloadException("Bad payload: missing/incomplete consumer or service.");
		}
		
		HashMap<String, Object> rm = new HashMap<String, Object>();
		rm.put("systemGroup", payload.getConsumer().getSystemGroup());
		rm.put("systemName", payload.getConsumer().getSystemName());
		ArrowheadSystem consumer = dm.get(ArrowheadSystem.class, rm);
		restrictionMap.put("consumer", consumer);
		
		rm.clear();
		rm.put("serviceGroup", payload.getService().getServiceGroup());
		rm.put("serviceDefinition", payload.getService().getServiceDefinition());
		ArrowheadService service = dm.get(ArrowheadService.class, rm);
		restrictionMap.put("service", service);
		
		OrchestrationStore entry = dm.get(OrchestrationStore.class, restrictionMap);
		if(entry == null){
			throw new DataNotFoundException("Requested entry was not found in the database.");
		}
		
		restrictionMap.remove("service");
		restrictionMap.put("isActive", true);
		OrchestrationStore activeEntry = dm.get(OrchestrationStore.class, restrictionMap);
		if(activeEntry != null){
			activeEntry.setIsActive(false);
			dm.merge(activeEntry);
		}
		
		entry.setIsActive(true);
		dm.merge(entry);
		
		return Response.ok(entry).build();
	}

	/**
	 * Adds a list of Orchestration Store entries to the database. Elements which would throw
	 * BadPayloadException (caused by missing/incomplete consumer, service, serialNumber or 
	 * provider) are being skipped. 
	 * The returned list only contains the elements which was saved in the process.
	 *
	 * @param {List<OrchestrationStore>}
	 *            serviceList
	 * @return List<OrchestrationStore>
	 * @throws DataNotFoundException
	 */
	@POST
	@Path("/store")
	public List<OrchestrationStore> addStoreEntries(List<OrchestrationStore> storeEntries) {
		List<OrchestrationStore> store = new ArrayList<OrchestrationStore>();
		for (OrchestrationStore entry : storeEntries) {
			if(entry.isPayloadUsable()){
				restrictionMap.clear();
				restrictionMap.put("serviceGroup", entry.getService().getServiceGroup());
				restrictionMap.put("serviceDefinition", entry.getService().getServiceDefinition());
				ArrowheadService service = dm.get(ArrowheadService.class, restrictionMap);
				if (service == null) {
					throw new DataNotFoundException("Invalid Orchestration Store entry: "
							+ "missing ArrowheadService in the database.");
				}
				restrictionMap.clear();
				restrictionMap.put("systemGroup", entry.getConsumer().getSystemGroup());
				restrictionMap.put("systemName", entry.getConsumer().getSystemName());
				ArrowheadSystem consumer = dm.get(ArrowheadSystem.class, restrictionMap);
				if (consumer == null) {
					throw new DataNotFoundException("Invalid Orchestration Store entry: "
							+ "missing consumer ArrowheadSystem in the database.");
				}
				
				ArrowheadSystem providerSystem = null;
				if(entry.getProviderSystem() != null){
					if(entry.getProviderSystem().getSystemGroup() != null &&
							entry.getProviderSystem().getSystemName() != null){
						restrictionMap.clear();
						restrictionMap.put("systemGroup", entry.getProviderSystem().getSystemGroup());
						restrictionMap.put("systemName", entry.getProviderSystem().getSystemName());
						providerSystem = dm.get(ArrowheadSystem.class, restrictionMap);
					}
					else{
						entry.setProviderSystem(providerSystem);
					}
				}
				
				
				ArrowheadCloud providerCloud = null;
				if(entry.getProviderCloud() != null){
					if(entry.getProviderCloud().getOperator() != null &&
							entry.getProviderCloud().getCloudName() != null){
						restrictionMap.clear();
						restrictionMap.put("operator", entry.getProviderCloud().getOperator());
						restrictionMap.put("cloudName", entry.getProviderCloud().getCloudName());
						providerCloud = dm.get(ArrowheadCloud.class, restrictionMap);
					}
					else{
						entry.setProviderCloud(providerCloud);
					}
				}
				
				entry.setConsumer(consumer);
				entry.setService(service);
				if(providerSystem != null){
					entry.setProviderSystem(providerSystem);
				}
				if(providerCloud != null){
					entry.setProviderCloud(providerCloud);
				}
				
				entry.setSerialNumber(0);
				entry.setLastUpdated(new Date());
				dm.merge(entry);
				store.add(entry);
			}
		}

		return store;
	}
	
	/**
	 * Updates an Orchestration Store entry specified by the unique fields of the payload.
	 * (Therefore only the non-unique fields will be updated.)
	 * 
	 * @param {StorePayload} - payload
	 * @return OrchestrationStore
	 * @throws BadPayloadException, DataNotFoundException
	 */
	@PUT
	@Path("/store/update")
	public Response updateEntry(OrchestrationStore payload){
		
		//TODO itt elszál, ha payloadban nincs benne a name, de igazából nem szükséges ennél a függvénynél
		if(!payload.isPayloadUsable()){
			throw new BadPayloadException("Bad payload: one of the mandatory fields is "
					+ "missing from the payload.");
		}
		
		HashMap<String, Object> rm = new HashMap<String, Object>();
		
		rm.put("systemGroup", payload.getConsumer().getSystemGroup());
		rm.put("systemName", payload.getConsumer().getSystemName());
		ArrowheadSystem consumer = dm.get(ArrowheadSystem.class, rm);
		restrictionMap.put("consumer", consumer);
		rm.clear();
		
		rm.put("serviceGroup", payload.getService().getServiceGroup());
		rm.put("serviceDefinition", payload.getService().getServiceDefinition());
		ArrowheadService service = dm.get(ArrowheadService.class, rm);
		restrictionMap.put("service", service);
		
		OrchestrationStore storeEntry = dm.get(OrchestrationStore.class, restrictionMap);
		if(storeEntry == null){
			throw new DataNotFoundException("Store entry specified by the payload "
					+ "not found in the database.");
		}
		else{
			storeEntry.setIsInterCloud(payload.getIsInterCloud());
			storeEntry.setOrchestrationRule(payload.getOrchestrationRule());
			storeEntry.setProviderCloud(payload.getProviderCloud());
			storeEntry.setProviderSystem(payload.getProviderSystem());
			storeEntry.setSerialNumber(storeEntry.getSerialNumber() + 1);
			storeEntry.setLastUpdated(new Date());
			storeEntry = dm.merge(storeEntry);
			
			return Response.status(Status.ACCEPTED).entity(storeEntry).build();
		}
	}

	/**
	 * Deletes the Orchestration Store entry with the name specified by
	 * the path parameter. Returns 200 if the delete is succesful, 204 (no
	 * content) if the entry was not in the database to begin with.
	 * 
	 * @param {String}
	 *            name
	 */
	@DELETE
	@Path("/store/{name}")
	public Response deleteEntry(@PathParam("name") String name) {
		restrictionMap.put("name", name);
		OrchestrationStore entry = dm.get(OrchestrationStore.class, restrictionMap);
		if (entry == null) {
			return Response.noContent().build();
		} else {
			dm.delete(entry);
			return Response.ok().build();
		}
	}

	
}
