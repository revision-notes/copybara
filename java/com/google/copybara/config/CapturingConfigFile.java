/*
 * Copyright (C) 2016 Google Inc.
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

package com.google.copybara.config;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * A config file that records the children created from it. Useful for collecting dependencies in
 * dry runs.
 */
class CapturingConfigFile<T> extends ConfigFile<T> {
  private final Set<CapturingConfigFile<T>> children = new LinkedHashSet<>();
  private final ConfigFile<T> wrapped;

  CapturingConfigFile(ConfigFile<T> config) {
    super(config.path());
    this.wrapped = Preconditions.checkNotNull(config);
  }

  @Override
  public byte[] content() throws IOException {
    return wrapped.content();
  }

  @Override
  protected T relativeToRoot(String label) throws CannotResolveLabel {
    return wrapped.relativeToRoot(label);
  }

  @Override
  protected T relativeToCurrentPath(String label) throws CannotResolveLabel {
    return wrapped.relativeToCurrentPath(label);
  }

  /**
   * Retrieve collected dependencies.
   * @return A Map mapping the path to the wrapped ConfigFile for each ConfigFile created by this or
   *     one of its descendants. Includes this.
   */
  ImmutableMap<String, ConfigFile<T>> getAllLoadedFiles() throws IOException {
    Map<String, ConfigFile<T>> map = new HashMap<>();
    getAllLoadedFiles(map);
    return ImmutableMap.<String, ConfigFile<T>>copyOf(map);
  }

  private void getAllLoadedFiles(Map<String, ConfigFile<T>> map) throws IOException {
    map.put(path(), this.wrapped);
    for (CapturingConfigFile<T> child : children) {
      child.getAllLoadedFiles(map);
    }
  }

  @Override
  protected ConfigFile<T> createConfigFile(String label, T resolved) throws CannotResolveLabel {
    CapturingConfigFile<T> child =
        new CapturingConfigFile<T>(wrapped.createConfigFile(label, resolved));
    children.add(child);
    return child;
  }

  @Override
  public boolean equals(Object otherObject) {
    if (otherObject instanceof CapturingConfigFile) {
      CapturingConfigFile other = (CapturingConfigFile) otherObject;
      return other.wrapped.equals(this.wrapped) && this.children.equals(other.children);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return this.path().hashCode();
  }
}
