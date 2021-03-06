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
package net.kuujo.copycat.serializer;

/**
 * Serializer interface.<p>
 *
 * The serializer is used by CopyCat primarily to serialize log entries.
 * Users can provide custom log serialization by registering the
 * serializer as a service. To do this, simply add a file containing
 * the serializer factory class at
 * <code>META-INF/services/net/kuujo/copycat/Serializer</code>. CopyCat
 * will pick up the new serializer and use it for all internal serialization.
 *
 * @author <a href="http://github.com/kuujo">Jordan Halterman</a>
 */
public interface Serializer {

  /**
   * Serializes a value to a byte array.
   *
   * @param value The value to serialize.
   * @return The serialized value.
   */
  byte[] writeValue(Object value);

  /**
   * Deserializes a value from a byte array.
   *
   * @param bytes The bytes to deserialize.
   * @param type The type to which to deserialize the given value.
   * @return
   */
  <T> T readValue(byte[] bytes, Class<T> type);

}
