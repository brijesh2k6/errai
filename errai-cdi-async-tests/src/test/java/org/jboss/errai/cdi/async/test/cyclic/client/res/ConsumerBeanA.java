/*
 * Copyright 2012 JBoss, by Red Hat, Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.errai.cdi.async.test.cyclic.client.res;


import org.jboss.errai.ioc.client.api.AfterInitialization;
import org.jboss.errai.ioc.client.api.LoadAsync;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

/**
 * @author Mike Brock
 */
@ApplicationScoped @LoadAsync
public class ConsumerBeanA {
  @Inject Baz baz;
  @Inject Foo foo;
  @Inject ProducerBeanA producerBeanA;

  private Bar bar = new Bar("fooz");

  @AfterInitialization
  public void afterInit() {
  }

  @Produces
  public Bar getBar() {
    return bar;
  }

  public Foo getFoo() {
    return foo;
  }

  public Baz getBaz() {
    return baz;
  }

  public ProducerBeanA getProducerBeanA() {
    return producerBeanA;
  }
}