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

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.bootstrap.ProjectBuilder;
import org.sonar.api.batch.bootstrap.ProjectDefinition;
import org.sonar.api.config.Settings;
import org.sonar.api.utils.SonarException;

import javax.annotation.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

public class VisualStudioProjectBuilder extends ProjectBuilder {

  private static final String SONAR_MODULES_PROPERTY_KEY = "sonar.modules";
  private static final Logger LOG = LoggerFactory.getLogger(VisualStudioProjectBuilder.class);

  private final Settings settings;

  public VisualStudioProjectBuilder(Settings settings) {
    this.settings = settings;
  }

  @Override
  public void build(Context context) {
    build(context, new VisualStudioAssemblyLocator(settings));
  }

  public void build(Context context, VisualStudioAssemblyLocator assemblyLocator) {
    ProjectDefinition solutionProject = context.projectReactor().getRoot();

    if (!settings.getBoolean(VisualStudioPlugin.VISUAL_STUDIO_ENABLE_PROPERTY_KEY)) {
      LOG.info("To enable the analysis bootstraper for Visual Studio projects, set the property \"" + VisualStudioPlugin.VISUAL_STUDIO_ENABLE_PROPERTY_KEY + "\" to \"true\"");
      return;
    }

    File solutionFile = getSolutionFile(solutionProject.getBaseDir());
    if (solutionFile == null) {
      LOG.info("No Visual Studio solution file found.");
      return;
    }

    LOG.info("Using the following Visual Studio solution: " + solutionFile.getAbsolutePath());

    if (settings.hasKey(SONAR_MODULES_PROPERTY_KEY)) {
      throw new SonarException("Do not use the Visual Studio bootstrapper and set the \"" + SONAR_MODULES_PROPERTY_KEY + "\" property at the same time.");
    }

    solutionProject.resetSourceDirs();

    boolean hasModules = false;

    VisualStudioSolution solution = new VisualStudioSolutionParser().parse(solutionFile);
    VisualStudioProjectParser projectParser = new VisualStudioProjectParser();
    for (VisualStudioSolutionProject project : solution.projects()) {
      File projectFile = relativePathFile(solutionFile.getParentFile(), project.path());
      if (!projectFile.isFile()) {
        LOG.warn("Unable to find the Visual Studio project file " + projectFile.getAbsolutePath());
      } else {
        hasModules = true;
        buildModule(solutionProject, project.name(), projectFile, projectParser.parse(projectFile), assemblyLocator, solutionFile);
      }
    }

    Preconditions.checkState(hasModules, "No Visual Studio projects were found.");
  }

  private void buildModule(ProjectDefinition solutionProject, String projectName, File projectFile, VisualStudioProject project, VisualStudioAssemblyLocator assemblyLocator,
    File solutionFile) {
    ProjectDefinition module = ProjectDefinition.create()
      .setKey(projectKey(solutionProject.getKey()) + ":" + projectName)
      .setName(projectName);
    solutionProject.addSubProject(module);

    module.setBaseDir(projectFile.getParentFile());
    module.setSourceDirs(projectFile.getParentFile());
    module.setWorkDir(new File(solutionProject.getWorkDir(), solutionProject.getKey().replace(':', '_') + "_" + projectName));

    for (String filePath : project.files()) {
      File file = relativePathFile(projectFile.getParentFile(), filePath);
      if (!file.isFile()) {
        LOG.warn("Cannot find the file " + file.getAbsolutePath() + " of project " + projectName);
      } else if (!isInSourceDir(file, projectFile.getParentFile())) {
        LOG.warn("Skipping the file " + file.getAbsolutePath() + " of project " + projectName + " located outside of the source directory.");
      } else {
        module.addSourceFiles(file);
      }
    }

    setFxCopProperties(module, projectFile, project, assemblyLocator);
    setReSharperProperties(module, projectName, solutionFile);
    setStyleCopProperties(module, projectFile);
  }

  private void setFxCopProperties(ProjectDefinition module, File projectFile, VisualStudioProject project, VisualStudioAssemblyLocator assemblyLocator) {
    File assembly = assemblyLocator.locateAssembly(module.getName(), projectFile, project);
    if (assembly == null) {
      return;
    }

    module.setProperty("sonar.cs.fxcop.assembly", assembly.getAbsolutePath());
    module.setProperty("sonar.vbnet.fxcop.assembly", assembly.getAbsolutePath());
  }

  private void setReSharperProperties(ProjectDefinition module, String projectName, File solutionFile) {
    module.setProperty("sonar.resharper.solutionFile", solutionFile.getAbsolutePath());
    module.setProperty("sonar.resharper.projectName", projectName);
  }

  private void setStyleCopProperties(ProjectDefinition module, File projectFile) {
    module.setProperty("sonar.stylecop.projectFilePath", projectFile.getAbsolutePath());
  }

  private static boolean isInSourceDir(File file, File folder) {
    try {
      return file.getCanonicalPath().replace('\\', '/').startsWith(folder.getCanonicalPath().replace('\\', '/') + "/");
    } catch (IOException e) {
      throw Throwables.propagate(e);
    }
  }

  @Nullable
  private File getSolutionFile(File projectBaseDir) {
    File result;

    String solutionPath = settings.getString(VisualStudioPlugin.VISUAL_STUDIO_SOLUTION_PROPERTY_KEY);
    if (!Strings.nullToEmpty(solutionPath).isEmpty()) {
      result = new File(projectBaseDir, solutionPath);
    } else {
      Collection<File> solutionFiles = FileUtils.listFiles(projectBaseDir, new String[] {"sln"}, false);
      if (solutionFiles.isEmpty()) {
        result = null;
      } else if (solutionFiles.size() == 1) {
        result = solutionFiles.iterator().next();
      } else {
        throw new SonarException("Found several .sln files in " + projectBaseDir.getAbsolutePath() +
          ". Please set \"" + VisualStudioPlugin.VISUAL_STUDIO_SOLUTION_PROPERTY_KEY + "\" to explicitly tell which one to use.");
      }
    }

    return result;
  }

  private static File relativePathFile(File file, String relativePath) {
    return new File(file, relativePath.replace('\\', '/'));
  }

  private String projectKey(String projectKey) {
    if ("unsafe".equals(settings.getString(VisualStudioPlugin.VISUAL_STUDIO_PROJECT_KEY_STRATEGY_PROPERTY_KEY))) {
      int i = projectKey.indexOf(':');

      if (i == -1) {
        LOG.warn("Unset the deprecated uncessary property \"" + VisualStudioPlugin.VISUAL_STUDIO_PROJECT_KEY_STRATEGY_PROPERTY_KEY + "\" used to analyze this project. "
          + "This property support will soon be removed, and unsetting it will *NOT* affect this particular project.");
      } else {
        String unsafeProjectKey = projectKey.substring(0, i);

        LOG.warn("Unset the deprecated uncessary property \"" + VisualStudioPlugin.VISUAL_STUDIO_PROJECT_KEY_STRATEGY_PROPERTY_KEY + "\" used to analyze this project. "
          + "You will need to update the project key from the unsafe \"" + unsafeProjectKey + "\" value to \"" + projectKey + "\".");

        return unsafeProjectKey;
      }
    }

    return projectKey;
  }

}
