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

import org.apache.commons.lang.StringUtils;
import org.sonar.api.utils.WildcardPattern;

import java.util.List;

/**
 * All information related to Visual Studio projects which can be extracted only from a project file.
 * Should not be mixed with information gathered from solution files.
 */
public class VisualStudioProject {

  private final List<String> files;
  private final String outputType;
  private final String assemblyName;
  private final List<String> propertyGroupConditions;
  private final List<String> outputPaths;
  private boolean unitTest;
  private boolean integTest;

  public VisualStudioProject(List<String> files, @Nullable String outputType, @Nullable String assemblyName, List<String> propertyGroupConditions, List<String> outputPaths) {
    this.files = files;
    this.outputType = outputType;
    this.assemblyName = assemblyName;
    this.propertyGroupConditions = propertyGroupConditions;
    this.outputPaths = outputPaths;
    this.unitTest = false;
    this.integTest = false;
  }

  public List<String> files() {
    return files;
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

  public boolean isTest() {
    return unitTest || integTest;
  }

  public boolean isUnitTest() {
    return this.unitTest;
  }

  public boolean isIntegTest() {
    return this.integTest;
  }
  
  void setUnitTest(boolean test) {
    this.unitTest = test;
  }

  void setIntegTest(boolean test) {
    this.integTest = test;
  }
  
  private boolean nameMatchPatterns(String testProjectPatterns) {
    if (StringUtils.isEmpty(testProjectPatterns)) {
      return false;
    }
    String[] patterns = StringUtils.split(testProjectPatterns, ";");
    boolean testFlag = false;

    for (int i = 0; i < patterns.length; i++) {
      if (WildcardPattern.create(patterns[i]).match(this.assemblyName)) {
        testFlag = true;
        break;
      }
    }
    return testFlag;
  }
  
  public boolean assessTestProject(String  patternUT, String patternIT) {
    boolean testFlag = nameMatchPatterns(patternUT);
    setUnitTest(testFlag);
    boolean integTestFlag = nameMatchPatterns(patternIT);
    setIntegTest(integTestFlag);
    return testFlag || integTestFlag;
  }
  
}
