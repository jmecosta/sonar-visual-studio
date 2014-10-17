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

import com.google.common.collect.ImmutableList;
import org.sonar.api.PropertyType;
import org.sonar.api.SonarPlugin;
import org.sonar.api.config.PropertyDefinition;
import org.sonar.api.resources.Qualifiers;

import java.util.List;

public class VisualStudioPlugin extends SonarPlugin {

  public static final String VISUAL_STUDIO_SOLUTION_PROPERTY_KEY = "sonar.visualstudio.solution";
  public static final String VISUAL_STUDIO_ENABLE_PROPERTY_KEY = "sonar.visualstudio.enable";
  public static final String VISUAL_STUDIO_OUTPUT_PATHS_PROPERTY_KEY = "sonar.visualstudio.outputPaths";
  public static final String VISUAL_STUDIO_TEST_PROJECT_PATTERN = "sonar.visualstudio.testProjectPattern";
  public static final String VISUAL_STUDIO_SKIPPED_PROJECT_PATTERN = "sonar.visualstudio.skippedProjectPattern";
  public static final String VISUAL_STUDIO_SKIP_IF_NOT_BUILT = "sonar.visualstudio.skipIfNotBuilt";

  public static final String VISUAL_STUDIO_OLD_SKIPPED_PROJECTS = "sonar.visualstudio.skippedProjects";
  public static final String VISUAL_STUDIO_OLD_OUTPUT_PATH_PROPERTY_KEY = "sonar.visualstudio.outputPath";
  public static final String VISUAL_STUDIO_OLD_SOLUTION_PROPERTY_KEY = "sonar.dotnet.visualstudio.solution.file";
  public static final String VISUAL_STUDIO_OLD_BUILD_CONFIGURATION_PROPERTY_KEY = "sonar.dotnet.buildConfiguration";
  public static final String VISUAL_STUDIO_OLD_BUILD_PLATFORM_PROPERTY_KEY = "sonar.dotnet.buildPlatform";

  public static final String VISUAL_STUDIO_PROJECT_KEY_STRATEGY_PROPERTY_KEY = "sonar.visualstudio.projectKeyStrategy";

  private static final String CATEGORY = "Visual Studio Bootstrapper";

  @Override
  public List getExtensions() {
    return ImmutableList.of(
      VisualStudioProjectBuilder.class,

      PropertyDefinition
        .builder(VISUAL_STUDIO_SOLUTION_PROPERTY_KEY)
        .deprecatedKey(VISUAL_STUDIO_OLD_SOLUTION_PROPERTY_KEY)
        .category(CATEGORY)
        .name("Solution file")
        .description("Absolute or relative path to the solution file to use. If not set or empty, a solution (\"*.sln\") file will be looked up.")
        .onlyOnQualifiers(Qualifiers.PROJECT)
        .build(),
      PropertyDefinition
        .builder(VISUAL_STUDIO_ENABLE_PROPERTY_KEY)
        .category(CATEGORY)
        .name("Turn on")
        .defaultValue("false")
        .type(PropertyType.BOOLEAN)
        .description("Whether or not the analysis should be bootstrapped from Visual Studio files. Can be enabled (or disabled) globally and on a per-project basis.")
        .onQualifiers(Qualifiers.PROJECT)
        .build(),
      PropertyDefinition
        .builder(VISUAL_STUDIO_OUTPUT_PATHS_PROPERTY_KEY)
        .deprecatedKey(VISUAL_STUDIO_OLD_OUTPUT_PATH_PROPERTY_KEY)
        .category(CATEGORY)
        .name("Assemblies output paths")
        .description("Overrides the assemblies output paths. Useful for Team Foundation Server builds. The paths may be absolute or relative to the solution directory.")
        .onlyOnQualifiers(Qualifiers.PROJECT)
        .build(),
      PropertyDefinition
        .builder(VISUAL_STUDIO_TEST_PROJECT_PATTERN)
        .category(CATEGORY)
        .name("Test project pattern")
        .defaultValue(".*[Tt]est.*")
        .type(PropertyType.REGULAR_EXPRESSION)
        .description("Regular expression matched by test project names.")
        .onQualifiers(Qualifiers.PROJECT)
        .build(),
      PropertyDefinition
        .builder(VISUAL_STUDIO_SKIPPED_PROJECT_PATTERN)
        .category(CATEGORY)
        .name("Skipped project pattern")
        .defaultValue("")
        .description("Regular expression matched by skipped project names.")
        .onQualifiers(Qualifiers.PROJECT)
        .build(),
      PropertyDefinition
        .builder(VISUAL_STUDIO_SKIP_IF_NOT_BUILT)
        .category(CATEGORY)
        .name("Skip not built projects")
        .defaultValue("false")
        .description("Skip the analysis of projects for which the assemblies are not found.")
        .onQualifiers(Qualifiers.PROJECT)
        .build(),

      deprecatedPropertyDefinition(VISUAL_STUDIO_OLD_SKIPPED_PROJECTS),
      deprecatedPropertyDefinition(VISUAL_STUDIO_OLD_SOLUTION_PROPERTY_KEY),
      deprecatedPropertyDefinition(VISUAL_STUDIO_OLD_BUILD_CONFIGURATION_PROPERTY_KEY),
      deprecatedPropertyDefinition(VISUAL_STUDIO_OLD_BUILD_PLATFORM_PROPERTY_KEY));
  }

  private static PropertyDefinition deprecatedPropertyDefinition(String oldKey) {
    return PropertyDefinition
      .builder(oldKey)
      .name(oldKey)
      .description("This property is deprecated and will be removed in a future version.<br />"
        + "You should stop using it as soon as possible.<br />"
        + "Consult the migration guide for guidance.")
      .category(CATEGORY)
      .subCategory("Deprecated")
      .onQualifiers(Qualifiers.PROJECT, Qualifiers.MODULE)
      .build();
  }

}
