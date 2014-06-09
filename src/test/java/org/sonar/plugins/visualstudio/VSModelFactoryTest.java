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

import org.junit.Test;

import java.io.File;
import java.util.Collection;
import java.util.List;

import static org.fest.assertions.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.matchers.JUnitMatchers.containsString;
import static org.mockito.Mockito.mock;

/**
 * Tests for visual studio utilities.
 * 
 * @author Fabrice BELLINGARD
 * @author Jose CHILLAN Sep 1, 2009
 */
public class VSModelFactoryTest {

  private static final String SOLUTIONS_PREFIX = "src/test/resources/solution/";
  private static final String PROJECT_CORE_PATH = SOLUTIONS_PREFIX + "Example/Example.Core/Example.Core.vcxproj";
//  private static final String SAMPLE_FILE_PATH = SOLUTIONS_PREFIX + "Example/Example.Core/Money.cpp";
  private static final String SOLUTION_EXAMPLE_PATH = SOLUTIONS_PREFIX + "Example/";
//  private static final String MESSY_SOLUTION_PATH = SOLUTIONS_PREFIX + "MessyTestSolution/MessyTestSolution.sln";
//  private static final String INVALID_SOLUTION_PATH = SOLUTIONS_PREFIX + "InvalidSolution/InvalidSolution.sln";
  private static final String SOLUTION_WITH_DUP_PATH = SOLUTIONS_PREFIX + "DuplicatesExample/";
  private static final String SOLUTION_WITH_CUSTOM_BUILD_PATH = SOLUTIONS_PREFIX + "CustomBuild/";
  
  @Test
  public void testReadFiles() {
    VisualStudioProject project = new VisualStudioProjectParser().parse(new File(PROJECT_CORE_PATH));
    assertEquals("Bad number of files extracted", 7, project.files().size());
  }

  @Test
  public void testSolutionWithCustomBuild() throws Exception {
    VisualStudioSolution solution = new VisualStudioSolutionParser().parse(new File(SOLUTION_WITH_CUSTOM_BUILD_PATH + "CustomBuild.sln"));
    assertEquals(1, solution.projects().size());
    VisualStudioSolutionProject project = solution.projects().get(0);
    VisualStudioProject VCproject = new VisualStudioProjectParser().parse(new File(SOLUTION_WITH_CUSTOM_BUILD_PATH + project.path()));
    List<String> buildConfigurations = VCproject.propertyGroupConditions();
    assertEquals(3, buildConfigurations.size());
    assertTrue(buildConfigurations.contains("'$(Configuration)|$(Platform)'=='Debug|Win32'"));
    assertTrue(buildConfigurations.contains("'$(Configuration)|$(Platform)'=='Release|Win32'"));
    assertTrue(buildConfigurations.contains("'$(Configuration)|$(Platform)'=='CustomCompil|Win32'"));
  }

  @Test
  public void testReadSolution() throws Exception {
    VisualStudioSolution solution = new VisualStudioSolutionParser().parse(new File(SOLUTION_EXAMPLE_PATH + "Example.sln"));
    // read "Example.Core.Tests"
    VisualStudioSolutionProject project = solution.projects().get(2);
    VisualStudioProject VCproject = new VisualStudioProjectParser().parse(new File(SOLUTION_EXAMPLE_PATH + project.path()));
    Collection<String> files = VCproject.files();
    for (String sourceFile : files) {
      assertThat(sourceFile.startsWith("Source("));
      assertThat(sourceFile.endsWith(")"));
    }
    assertEquals("Bad number of files extracted", 4, files.size());
  }

  @Test
  public void testProjecFiles() throws Exception {
    VisualStudioProject project = new VisualStudioProjectParser().parse(new File(PROJECT_CORE_PATH));
    assertNotNull("Could not retrieve a project ", project);
    Collection<String> sourceFiles = project.files();
    assertEquals("Bad number of files extracted", 7, sourceFiles.size());
  }

  @Test
  public void testSolutionWithAssemblyNameDuplicates() throws Exception {
    VisualStudioSolution solution = new VisualStudioSolutionParser().parse(new File(SOLUTION_WITH_DUP_PATH + "Example.sln"));
    List<VisualStudioSolutionProject> projects = solution.projects();
    assertEquals(2, projects.size());
    VisualStudioProject project1 = new VisualStudioProjectParser().parse(new File(SOLUTION_WITH_DUP_PATH + projects.get(0).path()));
    VisualStudioProject project2 = new VisualStudioProjectParser().parse(new File(SOLUTION_WITH_DUP_PATH + projects.get(1).path()));
  
    assertFalse(project1.assemblyName().equals(project2.assemblyName()));
  }
  

  @Test
  public void integTestPatterns() {
    List<String> files = mock(List.class);
    List<String> propertyGroupConditions = mock(List.class);
    List<String> outputPaths = mock(List.class);

    VisualStudioProject testProject = new VisualStudioProject(files, null, "MyProjectTest", propertyGroupConditions, outputPaths);
    VisualStudioProject secondTestProject = new VisualStudioProject(files, null, "MyProject.IT", propertyGroupConditions, outputPaths);
    VisualStudioProject project = new VisualStudioProject(files, null, "MyProject", propertyGroupConditions, outputPaths);
    
    String unitPatterns = "*Test";
    String integPatterns = "*.IT";
    
    project.assessTestProject(unitPatterns, integPatterns);
    testProject.assessTestProject(unitPatterns, integPatterns);
    secondTestProject.assessTestProject(unitPatterns, integPatterns);
    assertFalse(project.isTest());
    assertTrue(testProject.isTest());
    assertTrue(secondTestProject.isTest());
    assertTrue(testProject.isUnitTest());
    assertFalse(secondTestProject.isUnitTest());
    assertFalse(testProject.isIntegTest());
    assertTrue(secondTestProject.isIntegTest());
  }



}
