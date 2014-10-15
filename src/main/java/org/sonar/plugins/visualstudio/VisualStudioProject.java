/*
 * Analysis Bootstrapper for Visual Studio Projects
 * Copyright (C) 2014 SonarSource
 * dev@sonar.codehaus.org
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.plugins.visualstudio;

import javax.annotation.Nullable;

import java.util.List;

/**
 * All information related to Visual Studio projects which can be extracted only from a project file.
 * Should not be mixed with information gathered from solution files.
 */
public class VisualStudioProject {

  private final List<String> files;
  private final String projectTypeGuids;
  private final String outputType;
  private final String assemblyName;
  private final List<String> propertyGroupConditions;
  private final List<String> outputPaths;

  public VisualStudioProject(List<String> files, @Nullable String projectTypeGuids, @Nullable String outputType, @Nullable String assemblyName,
    List<String> propertyGroupConditions, List<String> outputPaths) {
    this.files = files;
    this.projectTypeGuids = projectTypeGuids;
    this.outputType = outputType;
    this.assemblyName = assemblyName;
    this.propertyGroupConditions = propertyGroupConditions;
    this.outputPaths = outputPaths;
  }

  public List<String> files() {
    return files;
  }

  @Nullable
  public String projectTypeGuids() {
    return projectTypeGuids;
  }

  @Nullable
  public String outputType() {
    return outputType;
  }

  @Nullable
  public String assemblyName() {
    return assemblyName;
  }

  public List<String> propertyGroupConditions() {
    return propertyGroupConditions;
  }

  public List<String> outputPaths() {
    return outputPaths;
  }

}
