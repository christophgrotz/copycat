/*
 * Copyright 2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.kuujo.copycat;

import junit.framework.Assert;
import net.kuujo.copycat.registry.Registry;
import net.kuujo.copycat.registry.impl.BasicRegistry;
import net.kuujo.copycat.registry.impl.ConcurrentRegistry;

import org.junit.Test;

/**
 * Registry test.
 *
 * @author <a href="http://github.com/kuujo">Jordan Halterman</a>
 */
public class RegistryTest {

  @Test
  public void testBasicRegistry() {
    Registry registry = new BasicRegistry();
    registry.bind("foo", "bar");
    Assert.assertEquals("bar", registry.lookup("foo"));
    registry.unbind("foo");
    Assert.assertNull(registry.lookup("foo"));
  }

  @Test
  public void testConcurrentRegistry() {
    Registry registry = new ConcurrentRegistry();
    registry.bind("foo", "bar");
    Assert.assertEquals("bar", registry.lookup("foo"));
    registry.unbind("foo");
    Assert.assertNull(registry.lookup("foo"));
  }

}
