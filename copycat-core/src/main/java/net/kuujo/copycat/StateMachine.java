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

/**
 * A state machine.
 *
 * @author <a href="http://github.com/kuujo">Jordan Halterman</a>
 */
public interface StateMachine {

  /**
   * Returns a snapshot of the state machine state.
   *
   * @return The state machine snapshot.
   */
  Snapshot takeSnapshot();

  /**
   * Installs a snapshot of the state machine state.
   *
   * @param snapshot The snapshot to install.
   */
  void installSnapshot(Snapshot snapshot);

  /**
   * Exceutes a state machine command.
   *
   * @param name The name of the command to execute.
   * @param args The command arguments.
   * @return The command return value.
   */
  Object applyCommand(String name, Arguments args);

}
