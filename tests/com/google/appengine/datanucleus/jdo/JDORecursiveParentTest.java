/**********************************************************************
 Copyright (c) 2011 Google Inc.

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
package com.google.appengine.datanucleus.jdo;

import com.google.appengine.datanucleus.test.jdo.HasRecursiveParentJDO.HasRecursiveParent;

/**
 * Test recursive relations (Issue 80)
 */
public class JDORecursiveParentTest extends JDOTestCase {

  public void testCreateParentAndChildDifferentTxn() {

    HasRecursiveParent p = new HasRecursiveParent();
    p.setName("yam");
    beginTxn();
    pm.makePersistent(p);
    String pid = p.getId();
    commitTxn();

    beginTxn();
    HasRecursiveParent c = new HasRecursiveParent();
    c.setName("yum");
    HasRecursiveParent p2 = pm.getObjectById(HasRecursiveParent.class, pid);
    c.setParent(p2);
    p2.getChildren().add(c);
    pm.makePersistent(c);
    String cid = c.getId();
    commitTxn();

    beginTxn();
    HasRecursiveParent c2 = pm.getObjectById(HasRecursiveParent.class, cid);
    HasRecursiveParent p3 = c2.getParent();
    assertEquals(1, p3.getChildren().size());
    assertEquals("yam", p3.getName());
    assertEquals(p3.getChildren().iterator().next(), c2);
    commitTxn();
  }

  public void testCreateParentAndChildSameTxn() {

    HasRecursiveParent p = new HasRecursiveParent();
    p.setName("yam");
    HasRecursiveParent c = new HasRecursiveParent();
    c.setName("yum");
    c.setParent(p);
    p.getChildren().add(c);
    beginTxn();
    pm.makePersistent(p);
    String pid = p.getId();
    String cid = c.getId();
    commitTxn();

    beginTxn();
    HasRecursiveParent p2 = pm.getObjectById(HasRecursiveParent.class, pid);
    assertEquals(1, p2.getChildren().size());
    HasRecursiveParent c2 = p2.getChildren().iterator().next();
    assertEquals(cid, c2.getId());
    assertEquals("yum", c2.getName());
    assertEquals(c2.getParent(), p2);
    commitTxn();

  }

  public void testCreateParentHierarchie() {

    HasRecursiveParent p, p0 = p = new HasRecursiveParent();
    p.setName("yam4");
    for (int i = 3; i > 0; i--) {
      HasRecursiveParent c = new HasRecursiveParent();
      c.setName("yam" + i);
      c.setParent(p);
      p.getChildren().add(c);
      p = c;
    }

    beginTxn();
    pm.makePersistent(p0);
    String cid = p.getId();
    commitTxn();

    beginTxn();
    HasRecursiveParent c = pm.getObjectById(HasRecursiveParent.class, cid);
    int i = 1;
    while (c.getParent() != null) {
      assertEquals("yam" + (i++), c.getName());
      c = c.getParent();
    }

    commitTxn();

  }

}
