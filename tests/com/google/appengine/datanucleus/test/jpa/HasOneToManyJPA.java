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
package com.google.appengine.datanucleus.test.jpa;

import java.io.Serializable;
import java.util.Collection;

/**
 * @author Max Ross <maxr@google.com>
 */
public interface HasOneToManyJPA extends Serializable {

  Collection<BidirectionalChildJPA> getBidirChildren();

  Collection<Book> getBooks();

  Collection<HasKeyPkJPA> getHasKeyPks();

  void setVal(String s);

  String getId();

  void nullBooks();

  void nullHasKeyPks();

  void nullBidirChildren();
}