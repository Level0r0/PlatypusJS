/* Data model license.
 * Exclusive rights on this code in any form
 * are belong to it's author. This code was
 * developed for commercial purposes only. 
 * For any questions and any actions with this
 * code in any form you have to contact to it's
 * author.
 * All rights reserved.
 */
package com.eas.client.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.bearsoft.rowset.CallbackAdapter;
import com.bearsoft.rowset.Cancellable;
import com.bearsoft.rowset.Rowset;
import com.bearsoft.rowset.Utils;
import com.bearsoft.rowset.Utils.JsObject;
import com.bearsoft.rowset.beans.PropertyChangeSupport;
import com.bearsoft.rowset.changes.Change;
import com.bearsoft.rowset.metadata.Field;
import com.bearsoft.rowset.metadata.Fields;
import com.bearsoft.rowset.metadata.Parameters;
import com.eas.client.application.AppClient;
import com.eas.client.form.published.HasPublished;
import com.eas.client.model.js.JsModel;
import com.google.gwt.core.client.Callback;
import com.google.gwt.core.client.JavaScriptObject;

/**
 * @author mg
 */
public class Model implements HasPublished {

	public static final String SCRIPT_MODEL_NAME = "model";
	public static final String PARAMETERS_SCRIPT_NAME = "params";
	public static final String DATASOURCE_METADATA_SCRIPT_NAME = "schema";
	public static final String DATASOURCE_NAME_TAG_NAME = "Name";
	public static final String DATASOURCE_TITLE_TAG_NAME = "Title";

	protected AppClient client;
	protected Set<Relation> relations = new HashSet<Relation>();
	protected Set<ReferenceRelation> referenceRelations = new HashSet<ReferenceRelation>();
	protected Map<String, Entity> entities = new HashMap<String, Entity>();
	protected ParametersEntity parametersEntity;
	protected Parameters parameters = new Parameters();
	protected PropertyChangeSupport changeSupport = new PropertyChangeSupport(this);
	protected List<Change> changeLog = new ArrayList<Change>();
	//
	protected RequeryProcess process;
	protected JavaScriptObject jsPublished;

	public static class RequeryProcess {
        public Collection<Entity> entities;
		public Map<Entity, String> errors = new HashMap<Entity, String>();
		public Callback<Rowset, String> callback;

		public RequeryProcess(Collection<Entity> aEntities, Callback<Rowset, String> aCallback) {
			super();
			entities = aEntities;
			callback = aCallback;
            assert callback != null : "aCallback argument is required.";
		}

		protected String assembleErrors() {
			if (errors != null && !errors.isEmpty()) {
				StringBuilder sb = new StringBuilder();
				for (Entity entity : errors.keySet()) {
					if (sb.length() > 0)
						sb.append("\n");
					sb.append(errors.get(entity)).append(" (").append(entity.getName()).append("[ ").append(entity.getTitle()).append("])");
				}
				return sb.toString();
			}
			return null;
		}

		public void cancel() throws Exception {
			callback.onFailure("Canceled");
		}

		public void success(){
			callback.onSuccess(null);
		}

		public void failure() {
			callback.onFailure(assembleErrors());
		}

		public void end() {
			if (errors.isEmpty()){
				success();
			}else{
				failure();
			}
		}
	}

	public static class SimpleEntry implements Entry<Entity, Integer> {

		protected Entity key;
		protected Integer value;

		private SimpleEntry(Entity aKey, Integer aValue) {
			super();
			key = aKey;
			value = aValue;
		}

		@Override
		public Entity getKey() {
			return key;
		}

		@Override
		public Integer getValue() {
			return value;
		}

		@Override
		public Integer setValue(Integer aValue) {
			Integer oldValue = value;
			value = aValue;
			return oldValue;
		}
	}

	public Model copy() throws Exception {
		Model copied = new Model(client);
		for (Entity entity : entities.values()) {
			copied.addEntity(entity.copy());
		}
		if (parameters != null) {
			copied.setParameters(parameters.copy());
		}
		if (getParametersEntity() != null) {
			copied.setParametersEntity((ParametersEntity) getParametersEntity().copy());
		}
		for (Relation relation : relations) {
			Relation rcopied = relation.copy();
			resolveCopiedRelation(rcopied, copied);
			copied.addRelation(rcopied);
		}
		for (ReferenceRelation relation : referenceRelations) {
			ReferenceRelation rcopied = (ReferenceRelation) relation.copy();
			resolveCopiedRelation(rcopied, copied);
			copied.addRelation(rcopied);
		}
		return copied;
	}

	protected void resolveCopiedRelation(Relation aRelation, Model aModel) throws Exception {
		if (aRelation.getLeftEntity() != null) {
			aRelation.setLeftEntity(aModel.getEntityById(aRelation.getLeftEntity().getEntityId()));
		}
		if (aRelation.getRightEntity() != null) {
			aRelation.setRightEntity(aModel.getEntityById(aRelation.getRightEntity().getEntityId()));
		}
		if (aRelation.getLeftField() != null) {
			if (aRelation.getLeftEntity() != null) {
				if (aRelation.isLeftParameter() && aRelation.getLeftEntity().getQueryName() != null) {
					aRelation.setLeftField(aRelation.getLeftEntity().getQuery().getParameters().get(aRelation.getLeftField().getName()));
				} else {
					aRelation.setLeftField(aRelation.getLeftEntity().getFields().get(aRelation.getLeftField().getName()));
				}
			} else {
				aRelation.setLeftField(null);
			}
		}
		if (aRelation.getRightField() != null) {
			if (aRelation.getRightEntity() != null) {
				if (aRelation.isRightParameter() && aRelation.getRightEntity().getQueryName() != null) {
					aRelation.setRightField(aRelation.getRightEntity().getQuery().getParameters().get(aRelation.getRightField().getName()));
				} else {
					aRelation.setRightField(aRelation.getRightEntity().getFields().get(aRelation.getRightField().getName()));
				}
			} else {
				aRelation.setRightField(null);
			}
		}
	}

	public void checkRelationsIntegrity() {
		List<Relation> toDel = new ArrayList<Relation>();
		for (Relation rel : relations) {
			if (rel.getLeftEntity() == null || (rel.getLeftField() == null && rel.getLeftParameter() == null) || rel.getRightEntity() == null
			        || (rel.getRightField() == null && rel.getRightParameter() == null)) {
				toDel.add(rel);
			}
		}
		for (Relation rel : toDel) {
			removeRelation(rel);
		}
		checkReferenceRelationsIntegrity();
	}

	protected void checkReferenceRelationsIntegrity() {
		List<ReferenceRelation> toDel = new ArrayList<ReferenceRelation>();
		for (ReferenceRelation rel : referenceRelations) {
			if (rel.getLeftEntity() == null || (rel.getLeftField() == null && rel.getLeftParameter() == null) || rel.getRightEntity() == null
			        || (rel.getRightField() == null && rel.getRightParameter() == null)) {
				toDel.add(rel);
			}
		}
		for (ReferenceRelation rel : toDel) {
			referenceRelations.remove(rel);
		}
	}

	/**
	 * Base model constructor.
	 */
	protected Model() {
		super();
		parametersEntity = new ParametersEntity();
		parametersEntity.setModel(this);
	}

	/**
	 * Constructor of datamodel. Used in designers.
	 * 
	 * @param aClient
	 *            C instance all queries to be sent to.
	 * @see AppClient
	 */
	public Model(AppClient aClient) {
		this();
		client = aClient;
	}

	public PropertyChangeSupport getChangeSupport() {
		return changeSupport;
	}

	public AppClient getClient() {
		return client;
	}

	public void setClient(AppClient aValue) {
		client = aValue;
	}

	public Set<ReferenceRelation> getReferenceRelations() {
		return Collections.unmodifiableSet(referenceRelations);
	}

	public boolean isPending() {
		for (Entity entity : entities.values()) {
			if (entity.isPending())
				return true;
		}
		return false;
	}

	public ParametersEntity getParametersEntity() {
		return parametersEntity;
	}

	public Parameters getParameters() {
		return parameters;
	}

	public void addRelation(Relation aRel) {
		if (aRel instanceof ReferenceRelation) {
			referenceRelations.add((ReferenceRelation) aRel);
		} else {
			relations.add(aRel);
			Entity lEntity = aRel.getLeftEntity();
			Entity rEntity = aRel.getRightEntity();
			if (lEntity != null && rEntity != null) {
				lEntity.addOutRelation(aRel);
				rEntity.addInRelation(aRel);
			}
		}
	}

	public Set<Relation> collectRelationsByEntity(Entity aEntity) {
		return aEntity.getInOutRelations();
	}

	@Override
	public JavaScriptObject getPublished() {
		return jsPublished;
	}

	@Override
	public void setPublished(JavaScriptObject aValue) {
		if (jsPublished != aValue) {
			jsPublished = aValue;
			publish();
		}
	}

	private void publish() {
		try {
			publishTopLevelFacade(jsPublished, this);
			publishRowsets();
		} catch (Exception ex) {
			Logger.getLogger(Model.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	private void publishRowsets() throws Exception {
		assert jsPublished != null : "JavaScript facade object has to be already installed while publishing rowsets facades.";
		validateQueries();
		for (Entity entity : entities.values()) {
			JavaScriptObject publishedEntity = JsModel.publish(entity);
			if (entity instanceof ParametersEntity) {
				ParametersEntity.publishTopLevelFacade(publishedEntity);
				jsPublished.<JsObject> cast().inject("params", publishedEntity);
			} else {
				Entity.publishRows(publishedEntity);
				if (entity.getName() != null && !entity.getName().isEmpty()) {
					jsPublished.<JsObject> cast().inject(entity.getName(), publishedEntity);
				}
			}
		}
		//
		for (ReferenceRelation aRelation : referenceRelations) {
			String scalarPropertyName = aRelation.getScalarPropertyName();
			if (scalarPropertyName == null || scalarPropertyName.isEmpty()) {
				scalarPropertyName = aRelation.getRightEntity().getName();
			}
			if (scalarPropertyName != null && !scalarPropertyName.isEmpty()) {
				aRelation.getLeftEntity().putOrmDefinition(scalarPropertyName,
				        ormPropertiedDefiner.scalar(aRelation.getRightEntity().getPublished(), aRelation.getRightField().getName(), aRelation.getLeftField().getName()));
			}
			String collectionPropertyName = aRelation.getCollectionPropertyName();
			if (collectionPropertyName == null || collectionPropertyName.isEmpty()) {
				collectionPropertyName = aRelation.getLeftEntity().getName();
			}
			if (collectionPropertyName != null && !collectionPropertyName.isEmpty()) {
				aRelation.getRightEntity().putOrmDefinition(collectionPropertyName,
				        ormPropertiedDefiner.collection(aRelation.getLeftEntity().getPublished(), aRelation.getRightField().getName(), aRelation.getLeftField().getName()));
			}
		}
		// ////////////////
	}

	private static DefinitionsContainer ormPropertiedDefiner = DefinitionsContainer.init();

	private static final class DefinitionsContainer extends JavaScriptObject {

		protected DefinitionsContainer() {
		}

		public native static DefinitionsContainer init()/*-{
			return {
				scalarDef : function(targetEntity, targetFieldName, sourceFieldName) {
					var _self = this;
					_self.enumerable = true;
					_self.configurable = false;
					_self.get = function() {
						var found = targetEntity.find(targetEntity.schema[targetFieldName], this[sourceFieldName]);
						return found.length == 0 ? null : (found.length == 1 ? found[0] : found);
					};
					_self.set = function(aValue) {
						this[sourceFieldName] = aValue ? aValue[targetFieldName] : null;
					};
				},
				collectionDef : function(sourceEntity, targetFieldName, sourceFieldName) {
					var _self = this;
					_self.enumerable = true;
					_self.configurable = false;
					_self.get = function() {
						var res = sourceEntity.find(sourceEntity.schema[sourceFieldName], this[targetFieldName]);
						if (res && res.length > 0) {
							return res;
						} else {
							return [];
						}
					};
				}
			}
		}-*/;

		public native JavaScriptObject scalar(JavaScriptObject targetEntity, String targetFieldName, String sourceFieldName)/*-{
			var constr = this.scalarDef;
			return new constr(targetEntity, targetFieldName, sourceFieldName);
		}-*/;

		public native JavaScriptObject collection(JavaScriptObject sourceEntity, String targetFieldName, String sourceFieldName)/*-{
			var constr = this.collectionDef;
			return new constr(sourceEntity, targetFieldName, sourceFieldName);
		}-*/;
	}

	public native static void publishTopLevelFacade(JavaScriptObject aTarget, Model aModel) throws Exception/*-{
		var publishedModel = aTarget;
		Object.defineProperty(publishedModel, "createQuery", {
			get : function() {
				return function(aQueryId) {
					$wnd.P.Logger.warning("createQuery deprecated call detected. Use loadEntity() instead.");
					return aModel.@com.eas.client.model.Model::jsLoadEntity(Ljava/lang/String;)(aQueryId);
				}
			}
		});
		Object.defineProperty(publishedModel, "loadEntity", {
			get : function() {
				return function(aQueryId) {
					return aModel.@com.eas.client.model.Model::jsLoadEntity(Ljava/lang/String;)(aQueryId);
				}
			}
		});
		Object.defineProperty(publishedModel, "save", {
			get : function() {
				return function(onScuccess, onFailure) {
					aModel.@com.eas.client.model.Model::save(Lcom/google/gwt/core/client/JavaScriptObject;Lcom/google/gwt/core/client/JavaScriptObject;)(onScuccess, onFailure);
				}
			}
		});
		Object.defineProperty(publishedModel, "revert", {
			get : function() {
				return function() {
					aModel.@com.eas.client.model.Model::revert()();
				}
			}
		});
		Object.defineProperty(publishedModel, "requery", {
			get : function() {
				return function(onSuccess, onFailure) {
					aModel.@com.eas.client.model.Model::requery(Lcom/google/gwt/core/client/JavaScriptObject;Lcom/google/gwt/core/client/JavaScriptObject;)(onSuccess, onFailure);
				}
			}
		});
		Object.defineProperty(publishedModel, "unwrap", {
			get : function() {
				return function() {
					return aModel;
				}
			}
		});
		Object.defineProperty(publishedModel, "modified", {
			get : function() {
				return aModel.@com.eas.client.model.Model::isModified()();
			}
		});
		Object.defineProperty(publishedModel, "pending", {
			get : function() {
				return aModel.@com.eas.client.model.Model::isPending()();
			}
		});
	}-*/;

	public void removeRelation(Relation aRelation) {
		relations.remove(aRelation);
		Entity lEntity = aRelation.getLeftEntity();
		Entity rEntity = aRelation.getRightEntity();
		if (lEntity != null && rEntity != null) {
			lEntity.removeOutRelation(aRelation);
			rEntity.removeInRelation(aRelation);
		}
	}

	public void addEntity(Entity aEntity) {
		entities.put(aEntity.getEntityId(), aEntity);
		aEntity.setModel(this);
	}

	public boolean removeEntity(Entity aEnt) {
		if (aEnt != null) {
			return (removeEntity(aEnt.getEntityId()) != null);
		}
		return false;
	}

	public Entity removeEntity(String aEntId) {
		Entity lent = entities.get(aEntId);
		if (lent != null) {
			entities.remove(aEntId);
		}
		return lent;
	}

	public Map<String, Entity> getEntities() {
		return entities;
	}

	public Map<String, Entity> getAllEntities() {
		Map<String, Entity> allEntities = new HashMap<>();
		allEntities.putAll(entities);
		if (parametersEntity != null) {
			allEntities.put(parametersEntity.getEntityId(), parametersEntity);
		}
		return allEntities;
	}

	public Entity getEntityById(String aId) {
		if (aId != null && ParametersEntity.PARAMETERS_ENTITY_ID.equals(aId)) {
			return parametersEntity;
		} else {
			return entities.get(aId);
		}
	}

	public void setEntities(Map<String, Entity> aValue) {
		entities = aValue;
	}

	public Set<Relation> getRelations() {
		return relations;
	}

	public void setParameters(Parameters aParams) {
		parameters = aParams;
	}

	public void setParametersEntity(ParametersEntity aParamsEntity) {
		if (parametersEntity != null)
			parametersEntity.setModel(null);
		parametersEntity = aParamsEntity;
		if (parametersEntity != null)
			parametersEntity.setModel(this);
	}

	public void setRelations(Set<Relation> aRelations) {
		relations = aRelations;
	}

    public void executeEntities(boolean refresh, Set<Entity> toExecute) throws Exception {
        if (refresh) {
            for(Entity entity : toExecute) {
                entity.invalidate();
            };
        }
        for (Entity entity : toExecute) {
            if (!entity.getQuery().isManual()) {
                entity.internalExecute(null);
            }
        }
    }

	private Set<Entity> rootEntities() {
		final Set<Entity> rootEntities = new HashSet<>();
		for (Entity entity : entities.values()) {
			Set<Relation> dependanceRels = new HashSet<>();
			for (Relation inRel : entity.getInRelations()) {
				if (!(inRel.getLeftEntity() instanceof ParametersEntity)) {
					dependanceRels.add(inRel);
				}
			}
			if (dependanceRels.isEmpty()) {
				rootEntities.add(entity);
			}
		}
		return rootEntities;
	}

	public RequeryProcess getProcess() {
		return process;
	}

	public void setProcess(RequeryProcess aValue) {
		process = aValue;
	}

	public void terminateProcess(Entity aSource, String aErrorMessage) {
		if (process != null) {
			if (aErrorMessage != null) {
				process.errors.put(aSource, aErrorMessage);
			}
			if (!isPending()) {
				RequeryProcess pr = process;
				process = null;
				pr.end();
			}
		}
	}

	public boolean isTypeSupported(int type) throws Exception {
		return true;
	}

	public boolean isNamePresent(String aName, Collection<Entity> aEntities, Entity toExclude, Field field2Exclude) {
		if (aEntities != null && aName != null) {
			for (Entity ent : aEntities) {
				if (ent != null && ent != toExclude) {
					String lName = ent.getName();
					if (lName != null && aName.equals(lName)) {
						return true;
					}
					if (ent instanceof ParametersEntity) {
						Fields params = ent.getFields();
						if (params != null && params.isNameAlreadyPresent(aName, field2Exclude)) {
							return true;
						}
					}
				}
			}
		}
		return false;
	}

	public List<com.bearsoft.rowset.changes.Change> getChangeLog() {
		return changeLog;
	}

	public void accept(ModelVisitor visitor) {
		visitor.visit(this);
	}

	public boolean isModified() throws Exception {
		if (entities != null) {
			for (Entity ent : entities.values()) {
				if (ent != null && ent.getRowset() != null) {
					if (ent.getRowset().isModified()) {
						return true;
					}
				}
			}
		}
		return false;
	}

	public Cancellable save(final JavaScriptObject onSuccess, final JavaScriptObject onFailure) throws Exception {
		return client.commit(changeLog, new CallbackAdapter<Void, String>() {

			@Override
			protected void doWork(Void aVoid) throws Exception {
				commited();
				if (onSuccess != null)
					Utils.invokeJsFunction(onSuccess);
			}

			@Override
			public void onFailure(String aReason) {
				try {
					rolledback();
					if (onFailure != null)
						Utils.executeScriptEventVoid(jsPublished, onFailure, aReason);
				} catch (Exception ex) {
					Logger.getLogger(Model.class.getName()).log(Level.SEVERE, null, ex);
				}
			}

		});
	}

	public void commited() throws Exception {
		changeLog.clear();
		for (Entity aEntity : entities.values()) {
			try {
				Rowset rowset = aEntity.getRowset();
				if (rowset != null) {
					rowset.commited();
				}
			} catch (Exception ex) {
				Logger.getLogger(Model.class.getName()).log(Level.SEVERE, null, ex);
			}
		}
	}

	public void revert() throws Exception {
		changeLog.clear();
		for (Entity aEntity : entities.values()) {
			try {
				Rowset rowset = aEntity.getRowset();
				if (rowset != null) {
					rowset.rolledback();
				}
			} catch (Exception ex) {
				Logger.getLogger(Model.class.getName()).log(Level.SEVERE, null, ex);
			}
		}
	}

	public void rolledback() throws Exception {
		Logger.getLogger(Model.class.getName()).log(Level.SEVERE, "rolled back");
	}

	public void requery(final JavaScriptObject onSuccess, final JavaScriptObject onFailure) throws Exception {
		requery(new CallbackAdapter<Rowset, String>() {
			@Override
			protected void doWork(Rowset aRowset) throws Exception {
				if (onSuccess != null)
					Utils.invokeJsFunction(onSuccess);
			}

			@Override
			public void onFailure(String reason) {
				if (onFailure != null) {
					try {
						Utils.executeScriptEventVoid(jsPublished, onFailure, reason);
					} catch (Exception ex) {
						Logger.getLogger(Model.class.getName()).log(Level.SEVERE, null, ex);
					}
				}
			}
		});
	}

	public void requery(Callback<Rowset, String> aCallback) throws Exception {
        changeLog.clear();
        if (process != null) {
            process.cancel();
        }
        if (aCallback != null) {
            process = new RequeryProcess(entities.values(), aCallback);
        }
        revert();
        executeEntities(true, rootEntities());
        if (!isPending() && process != null) {
            process.end();
            process = null;
        }
	}

	public void execute(final JavaScriptObject onSuccess, final JavaScriptObject onFailure) throws Exception {
		execute(new CallbackAdapter<Rowset, String>() {
			@Override
			protected void doWork(Rowset aRowset) throws Exception {
				if (onSuccess != null)
					Utils.invokeJsFunction(onSuccess);
			}

			@Override
			public void onFailure(String reason) {
				if (onFailure != null) {
					try {
						Utils.executeScriptEventVoid(jsPublished, onFailure, reason);
					} catch (Exception ex) {
						Logger.getLogger(Model.class.getName()).log(Level.SEVERE, null, ex);
					}
				}
			}
		});
	}

	public void execute(Callback<Rowset, String> aCallback) throws Exception {
        if (process != null) {
            process.cancel();
        }
        if (aCallback != null) {
            process = new RequeryProcess(entities.values(), aCallback);
        }
        executeEntities(false, rootEntities());
        if (!isPending() && process != null) {
            process.end();
            process = null;
        }
	}

	public void validateQueries() throws Exception {
		for (Entity entity : entities.values()) {
			entity.validateQuery();
		}
	}

	protected static final String USER_DATASOURCE_NAME = "userEntity";

	public synchronized Object jsLoadEntity(String aQueryName) throws Exception {
		if (client == null) {
			throw new NullPointerException("Null client detected while creating an entity");
		}
		Entity entity = new Entity(this);
		entity.setName(USER_DATASOURCE_NAME);
		entity.setQueryName(aQueryName);
		entity.validateQuery();
		// addEntity(entity); To avoid memory leaks you should not add the
		// entity to the model!
		return JsModel.publish(entity);
	}
}
