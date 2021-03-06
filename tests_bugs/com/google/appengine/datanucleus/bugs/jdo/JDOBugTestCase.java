/**********************************************************************
Copyright (c) 2009 Google Inc.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
**********************************************************************/
package com.google.appengine.datanucleus.bugs.jdo;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.datanucleus.DatastoreManager;
import com.google.appengine.datanucleus.bugs.DatastoreTestCase;
import com.google.appengine.datanucleus.EntityUtils;
import com.google.appengine.datanucleus.Utils;

import org.datanucleus.api.jdo.JDOPersistenceManager;
import org.datanucleus.metadata.MetaDataManager;
import org.datanucleus.ExecutionContext;
import org.datanucleus.util.NucleusLogger;

import java.util.Map;

import javax.jdo.JDOHelper;
import javax.jdo.PersistenceManager;
import javax.jdo.PersistenceManagerFactory;

/**
 * Base testcase for tests that need a {@link PersistenceManagerFactory}.
 *
 * @author Max Ross <maxr@google.com>
 */
public class JDOBugTestCase extends DatastoreTestCase {

  private static
  ThreadLocal<Map<PersistenceManagerFactoryName, PersistenceManagerFactory>> pmfCache =
      new ThreadLocal<Map<PersistenceManagerFactoryName, PersistenceManagerFactory>>() {
        @Override
        protected Map<PersistenceManagerFactoryName, PersistenceManagerFactory> initialValue() {
          return Utils.newHashMap();
        }
      };

  protected PersistenceManagerFactory pmf;
  protected PersistenceManager pm;

  protected DatastoreService ds;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    ds = DatastoreServiceFactory.getDatastoreService();
    pmf = pmfCache.get().get(getPersistenceManagerFactoryName());
    if (pmf == null) {
      pmf = JDOHelper.getPersistenceManagerFactory(getPersistenceManagerFactoryName().name());
      if (cacheManagers()) {
        pmfCache.get().put(getPersistenceManagerFactoryName(), pmf);
      }
    }
    pm = pmf.getPersistenceManager();
  }

  public enum PersistenceManagerFactoryName { transactional, nontransactional }

  /**
   * By default we use a datasource that requires txns.
   * Override this if your test needs to use a different instance.
   */
  protected PersistenceManagerFactoryName getPersistenceManagerFactoryName() {
    return PersistenceManagerFactoryName.transactional;
  }

  @Override
  protected void tearDown() throws Exception {
    try {
      if (!pm.isClosed()) {
        if (pm.currentTransaction().isActive()) {
          pm.currentTransaction().rollback();
        }
        pm.close();
      }
      pm = null;
      // see if anybody closed any of our pmfs and if so just remove them from the cache -
      // we'll rebuild it the next time it's needed.
      for (Map.Entry<PersistenceManagerFactoryName, PersistenceManagerFactory> entry : pmfCache.get().entrySet()) {
        if (entry.getValue().isClosed()) {
          pmfCache.get().remove(entry.getKey());
        }
      }
      if (!cacheManagers() && !pmf.isClosed()) {
        pmf.close();
      }
      pmf = null;
    } finally {
      super.tearDown();
    }
  }


  protected void beginTxn() {
    pm.currentTransaction().begin();
  }

  protected void commitTxn() {
    pm.currentTransaction().commit();
  }

  protected void rollbackTxn() {
    pm.currentTransaction().rollback();
  }

  protected <T> T makePersistentInTxn(T obj, StartEnd startEnd) {
    boolean success = false;
    NucleusLogger.GENERAL.info(">> JDOTestCase.makePersistentInTxn");
    NucleusLogger.GENERAL.info(">> test START");
    startEnd.start();
    try {
        NucleusLogger.GENERAL.info(">> test makePersistent");
      pm.makePersistent(obj);
      NucleusLogger.GENERAL.info(">> test END");
      startEnd.end();
      success = true;
    } finally {
      if (!success && pm.currentTransaction().isActive()) {
          NucleusLogger.GENERAL.info(">> test ROLLBACK");
        rollbackTxn();
      }
    }
    return obj;
  }

  protected void switchDatasource(PersistenceManagerFactoryName name) {
    switchDatasource(null, name);
  }

  protected void switchDatasource(Map<String, String> props, PersistenceManagerFactoryName name) {
    if (!pm.isClosed()) {
      pm.close();
    }
    if (!cacheManagers() && !pmf.isClosed()) {
      pmf.close();
    }
    if (props == null) {
      pmf = JDOHelper.getPersistenceManagerFactory(name.name());
    } else {
      pmf = JDOHelper.getPersistenceManagerFactory(props, name.name());      
    }
    pm = pmf.getPersistenceManager();
  }

  @SuppressWarnings("deprecation")
  protected int countForClass(Class<?> clazz) {
    String kind = kindForClass(clazz);
    return ds.prepare(
        new com.google.appengine.api.datastore.Query(kind)).countEntities();
  }

  protected String kindForClass(Class<?> clazz) {
    ExecutionContext om = getExecutionContext();
    MetaDataManager mdm = om.getMetaDataManager();
    return EntityUtils.determineKind(
        mdm.getMetaDataForClass(clazz, om.getClassLoaderResolver()), om);
  }

  protected String kindForObject(Object obj) {
    return kindForClass(obj.getClass());
  }

  protected ExecutionContext getExecutionContext() {
    return ((JDOPersistenceManager)pm).getExecutionContext();
  }

  private boolean cacheManagers() {
    return !Boolean.valueOf(System.getProperty("do.not.cache.managers"));
  }

  interface StartEnd {
    void start();
    void end();
    PersistenceManagerFactoryName getPmfName();
  }

  public final StartEnd TXN_START_END = new StartEnd() {
    public void start() {
      beginTxn();
    }

    public void end() {
      commitTxn();
    }

    public PersistenceManagerFactoryName getPmfName() {
      return PersistenceManagerFactoryName.transactional;
    }
  };

  public final StartEnd NEW_PM_START_END = new StartEnd() {
    public void start() {
      if (pm.isClosed()) {
        pm = pmf.getPersistenceManager();
      }
    }

    public void end() {
      pm.close();
    }

    public PersistenceManagerFactoryName getPmfName() {
      return PersistenceManagerFactoryName.nontransactional;
    }
  };

  protected DatastoreManager getStoreManager() {
    return (DatastoreManager) getExecutionContext().getStoreManager();
  }
}
