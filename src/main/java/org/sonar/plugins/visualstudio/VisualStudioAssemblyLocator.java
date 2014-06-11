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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.config.Settings;

import javax.annotation.Nullable;

import java.io.File;
import java.io.Serializable;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class VisualStudioAssemblyLocator {

  private static final Logger LOG = LoggerFactory.getLogger(VisualStudioProjectBuilder.class);
  private static final Comparator<File> FILE_LAST_MODIFIED_COMPARATOR = new FileLastModifiedComparator();

  private final Settings settings;

  public VisualStudioAssemblyLocator(Settings settings) {
    this.settings = settings;
  }

  public File locateAssembly(String projectName, File projectFile, VisualStudioProject project) {
    LOG.info("Locating the assembly for the project: " + projectName + "...");
    if (project.outputType() == null || project.assemblyName() == null) {
      LOG.info("Unable to locate the assembly as either the output type or the assembly name is missing.");
      return null;
    }

    String extension = extension(projectFile, project.outputType());
    if (extension == null) {
      LOG.error("Unable to locate the assembly of the unsupported output type: " + project.outputType());
      return null;
    }

    String assemblyFileName = project.assemblyName() + "." + extension;
    List<File> candidates = candidates(assemblyFileName, projectFile, project);

    if (candidates.isEmpty()) {
      LOG.warn("Unable to locate the assembly of project " + projectFile.getAbsolutePath());
      return null;
    }

    Collections.sort(candidates, FILE_LAST_MODIFIED_COMPARATOR);

    if (candidates.size() > 1) {
      LOG.info("Picking the most recently generated assembly file: " + candidates.get(0).getAbsolutePath());
    }

    return candidates.get(0);
  }

  @VisibleForTesting
  @Nullable
  String extension(File projectFile, String outputType) {
    String result;

    if ("library".equals(outputType.toLowerCase())) {
      result = "dll";
    } else if ("exe".equals(outputType.toLowerCase())) {
      result = "exe";
    } else if ("winexe".equals(outputType.toLowerCase())) {
      result = "exe";
    } else {
      result = null;
      LOG.info("ProjectFile not Supported: " + projectFile + " : OutputType = " + outputType + " : Supported[Library, Exe, WinExe]");
    }

    return result;
  }

  private List<File> candidates(String assemblyFileName, File projectFile, VisualStudioProject project) {
    List<File> candidates = Lists.newArrayList();    
    String data = settings.getString(VisualStudioPlugin.VISUAL_STUDIO_OUTPUT_PATH_PROPERTY_KEY);
    if (data != null) {
      String[] paths = data.split(",");
      for(String path : paths)
      {
        File candidate = new File(new File(path.replace('\\', '/')), assemblyFileName);
        LOG.info("Trying to Locate : " + candidate.getAbsolutePath());
        if (candidate.isFile()) {
          LOG.info("The following candidate assembly was found: " + candidate.getAbsolutePath());
          candidates.add(candidate);
        }
      }
    } else {
      for (int i = 0; i < project.outputPaths().size(); i++) {
        String outputPath = project.outputPaths().get(i);
        File candidate = new File(projectFile.getParentFile(), outputPath.replace('\\', '/') + '/' + assemblyFileName);

        if (!candidate.isFile()) {
          LOG.info("The following candidate assembly was not built: " + candidate.getAbsolutePath());
        } else if (matchesBuildConfigurationAndPlatform(project.propertyGroupConditions().get(i))) {
          LOG.info("The following candidate assembly was found: " + candidate.getAbsolutePath());
          candidates.add(candidate);
        } else {
          LOG.info("The following candidate assembly was found, but rejected because it does not match the request build configuration and platform: " + candidate.getAbsolutePath());
        }
      }      
    }
    
    return candidates;
  }

  private void VerifyPresenceOfDll(File candidate, VisualStudioProject project, int i, List<File> candidates) {

  }

  private boolean matchesBuildConfigurationAndPlatform(String condition) {
    String buildConfiguration = settings.getString(VisualStudioPlugin.VISUAL_STUDIO_OLD_BUILD_CONFIGURATION_PROPERTY_KEY);
    String buildPlatform = settings.getString(VisualStudioPlugin.VISUAL_STUDIO_OLD_BUILD_PLATFORM_PROPERTY_KEY);

    if (buildConfiguration != null && buildPlatform != null) {
      LOG.warn("The properties \"" + VisualStudioPlugin.VISUAL_STUDIO_OLD_BUILD_CONFIGURATION_PROPERTY_KEY + "\" and \"" + VisualStudioPlugin.VISUAL_STUDIO_OLD_BUILD_PLATFORM_PROPERTY_KEY + "\" are deprecated");
      return condition.contains(buildConfiguration) && condition.contains(buildPlatform);
    }

    return true;
  }

  public static class FileLastModifiedComparator implements Comparator<File>, Serializable {

    private static final long serialVersionUID = 1L;

    @Override
    public int compare(File o1, File o2) {
      if (o1.lastModified() == o2.lastModified()) {
        return 0;
      }

      return o1.lastModified() > o2.lastModified() ? -1 : 1;
    }

  }

}
