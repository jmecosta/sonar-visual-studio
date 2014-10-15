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

import java.util.List;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class VisualStudioProjectTest {

  @Test
  public void test() {
    List<String> files = mock(List.class);
    List<String> propertyGroupConditions = mock(List.class);
    List<String> outputPaths = mock(List.class);

    VisualStudioProject project = new VisualStudioProject(files, "{guid}", "Library", null, propertyGroupConditions, outputPaths);

    assertThat(project.files()).isSameAs(files);
    assertThat(project.projectTypeGuids()).isEqualTo("{guid}");
    assertThat(project.outputType()).isEqualTo("Library");
    assertThat(project.assemblyName()).isNull();
    assertThat(project.propertyGroupConditions()).isSameAs(propertyGroupConditions);
    assertThat(project.outputPaths()).isSameAs(outputPaths);

    project = new VisualStudioProject(files, null, null, "MyLibrary", propertyGroupConditions, outputPaths);
    assertThat(project.projectTypeGuids()).isNull();
    assertThat(project.outputType()).isNull();
    assertThat(project.assemblyName()).isEqualTo("MyLibrary");
  }

}
