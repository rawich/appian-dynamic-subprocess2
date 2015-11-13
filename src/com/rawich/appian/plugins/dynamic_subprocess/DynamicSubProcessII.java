package com.rawich.appian.plugins.dynamic_subprocess;

import org.apache.log4j.Logger;

import com.appiancorp.services.ServiceContext;
import com.appiancorp.services.ServiceContextFactory;
import com.appiancorp.suiteapi.common.Name;
import com.appiancorp.suiteapi.common.ServiceLocator;
import com.appiancorp.suiteapi.process.ProcessDesignService;
import com.appiancorp.suiteapi.process.ProcessExecutionService;
import com.appiancorp.suiteapi.process.ProcessStartConfig;
import com.appiancorp.suiteapi.process.ProcessVariable;
import com.appiancorp.suiteapi.process.ProcessVariableInstance;
import com.appiancorp.suiteapi.process.exceptions.SmartServiceException;
import com.appiancorp.suiteapi.process.framework.AppianSmartService;
import com.appiancorp.suiteapi.process.framework.Input;
import com.appiancorp.suiteapi.process.framework.Order;
import com.appiancorp.suiteapi.process.framework.Required;
import com.appiancorp.suiteapi.process.framework.SmartServiceContext;
import com.appiancorp.suiteapi.process.framework.Unattended;
import com.appiancorp.suiteapi.process.palette.PaletteInfo;
import com.appiancorp.suiteapi.type.AppianType;
import com.appiancorp.suiteapi.type.NamedTypedValue;

/**
 * 
 * @author Rawich Poomrin (November 2015)
 * @version 1.0.0
 * Dynamic Sub-Process II:
 * 	This is a derivative of and extension to Dynamic Subprocess (Async) shared component by Ryan Gates in Appian Forum.
 *  These are the 2 important changes:
 *   1) All errors that resulted in new sub-process not started will cause the node to paused by exception.
 *   2) New input "Sub-Process Initiator" is added for the flexibility to specify initiator of the sub-process, instead of limited to the process model designer or initiator of the parent process.
 *   
 *   Error conditions will be reported both in the log file, and Alert, and node will be paused by exception. 
 *   
 * Known issue:
 *	  - The node will fail to start sub-process if UUID is used to identify the sub-process and the Run-As user is not a system administrator.
 *
 * There are two main error scenarios, with appropriate error messages from resource bundle:
 * 	 - Looking up of process model ID from UUID failed (permission issue or process model with the specified UUID does not exist)
 *   - Starting the subprocess failed (most likely because the user who executes this node does not have enough security access to the target process model.
 */
@PaletteInfo(paletteCategory = "Standard Nodes", palette = "Activities") 
@Unattended
@Order({"modelId", "modelUUID", "subProcessInitiator"})
public class DynamicSubProcessII extends AppianSmartService {
	private static final String PARENT_PROCESS_MODEL_ID = "parentProcessModelId";
	private static final String PARENT_PROCESS_ID = "parentProcessId";

	private static final Logger LOG = Logger.getLogger(DynamicSubProcessII.class);
	
	private final SmartServiceContext smartServiceContext;
	private Long modelId;
	private String modelUUID;
	private String subProcessInitiator;
	
	private Long subProcessId;
	
	/**
	 * Constructor with SmartServiceContext
	 * @param smartServiceContext The context of process where this smart service is being called from.
	 */
	public DynamicSubProcessII(SmartServiceContext smartServiceContext) {
		super();
		this.smartServiceContext = smartServiceContext;
	}

	@Override
	public void run() throws SmartServiceException {
		// For lookup with Process Model UUID to work, the user context has to be an administrator
		ServiceContext _sc = ServiceContextFactory.getServiceContext(smartServiceContext.getUsername());
	      
		ProcessExecutionService _pes = ServiceLocator.getProcessExecutionService(_sc);
		ProcessDesignService _sscPds = ServiceLocator.getProcessDesignService(_sc);
	      
	    // If subProcessInitiator is not specified, use the same user from smart service context
		LOG.debug("smartServiceContext.username: " + smartServiceContext.getUsername() 
					+ " / subProcessInitiator: " + subProcessInitiator);
		ProcessDesignService _initiatorPds = isBlankOrNull(subProcessInitiator)
												? _sscPds
												: ServiceLocator.getProcessDesignService(ServiceContextFactory.getServiceContext(subProcessInitiator));

	    if(modelId == null) {
	    	try {
	    		modelId = _sscPds.getProcessModelByUuid(modelUUID).getId();
	        } catch (Exception _ex) {
	        	LOG.error(_ex);
	        	throw createException(_ex, "error.invalidUUID");
	        }
	      }
	      
	      
	    try {
	        ProcessVariable[] _subProcessVariables = _sscPds.getProcessModelParameters(modelId);
	        ProcessVariableInstance[] _parentProcessVariables = _pes.getRecursiveProcessVariables(this.smartServiceContext.getProcessProperties().getId(), true);

	        for(int j=0; j < _subProcessVariables.length; j++) {
	           NamedTypedValue _subPV = _subProcessVariables[j];
	          boolean set = false;
	          for(int i=0; i < _parentProcessVariables.length; i++) {
	            ProcessVariableInstance _parentPV = _parentProcessVariables[i];
	            if (_parentPV.getName().equalsIgnoreCase(_subPV.getName()) && 
	                _parentPV.getTypeRef().getId().equals(_subPV.getTypeRef().getId())) {
	              _subPV.setValue(_parentPV.getRunningValue());
	              set = true;
	              LOG.debug("Set SubPV name: " + _subPV.getName() + " / value:" + _parentPV.getRunningValue());
	              break;
	            }
	          }
	          if (!set) {
	            if (_subPV.getName().equalsIgnoreCase(PARENT_PROCESS_ID) && 
	                _subPV.getTypeRef().getId()==AppianType.INTEGER ) {
	              _subPV.setValue(smartServiceContext.getProcessProperties().getId());
	            } else if (_subPV.getName().equalsIgnoreCase(PARENT_PROCESS_MODEL_ID) && 
	                _subPV.getTypeRef().getId()==AppianType.INTEGER ) {
	              _subPV.setValue(smartServiceContext.getProcessModelProperties().getId());
	            }
	          }
	        }
	        
	        ProcessStartConfig _processStartConfig = new ProcessStartConfig(_subProcessVariables);
	        LOG.debug("Starting Process Model ID: " + modelId);
	        this.subProcessId = _initiatorPds.initiateProcess(modelId, _processStartConfig);
	        
	      } catch (Exception _ex) {
	        LOG.error(_ex);
	        throw createException(_ex, "error.initiateProcess", modelId, modelUUID);
	      }
	}

	@Name("SubProcessId")
	public Long getSubProcessId() {
		return subProcessId;
	}

	@Input(required = Required.OPTIONAL)
	@Name("modelId")
	public void setModelId(Long modelId) {
		this.modelId = modelId;
	}

	@Input(required = Required.OPTIONAL)
	@Name("modelUUID")
	public void setModelUUID(String modelUUID) {
		this.modelUUID = modelUUID;
	}

	@Input(required = Required.OPTIONAL)
	@Name("subProcessInitiator")
	public void setSubProcessInitiator(String subProcessInitiator) {
		this.subProcessInitiator = subProcessInitiator;
	}

	private SmartServiceException createException(Throwable t, String key, Object... args) { 
		return new SmartServiceException.Builder(getClass(), t).userMessage(key, args).build(); 
	} 
	
	private boolean isBlankOrNull(String value) {
		if(value == null || value.trim().length() == 0) {
			return true;
		}
		
		return false;
	}
}
