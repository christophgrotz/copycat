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
package net.kuujo.copycat.protocol.impl;

import net.kuujo.copycat.CopyCatContext;
import net.kuujo.copycat.protocol.Protocol;
import net.kuujo.copycat.protocol.ProtocolClient;
import net.kuujo.copycat.protocol.ProtocolServer;
import net.kuujo.copycat.uri.UriAuthority;
import net.kuujo.copycat.uri.UriInject;
import net.kuujo.copycat.uri.UriSchemeSpecificPart;

/**
 * Direct protocol implementation.
 *
 * @author <a href="http://github.com/kuujo">Jordan Halterman</a>
 */
public class DirectProtocol implements Protocol {
  private String address;
  private CopyCatContext context;

  @UriInject
  public DirectProtocol(@UriAuthority @UriSchemeSpecificPart String address) {
    this.address = address;
  }

  @Override
  public void init(CopyCatContext context) {
    this.context = context;
  }

  @Override
  public ProtocolServer createServer() {
    return new DirectProtocolServer(address, context);
  }

  @Override
  public ProtocolClient createClient() {
    return new DirectProtocolClient(address, context);
  }

}
