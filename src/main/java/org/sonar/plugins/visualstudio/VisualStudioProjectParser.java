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

import com.google.common.base.Charsets;
import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.io.Closeables;

import javax.annotation.Nullable;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

public class VisualStudioProjectParser {
  public static final String PROPERTYGROUP = "PropertyGroup";
  public static final String ITEMGROUP = "ItemGroup";

  public VisualStudioProject parse(File file) {
    return new Parser().parse(file);
  }

  private static class Parser {

    private File file;
    private XMLStreamReader stream;
    private final ImmutableList.Builder<String> filesBuilder = ImmutableList.builder();
    private String outputType;
    private String assemblyName;
    private String currentCondition;
    private String currentElement;
    private final ImmutableList.Builder<String> propertyGroupConditionsBuilder = ImmutableList.builder();
    private final ImmutableList.Builder<String> outputPathsBuilder = ImmutableList.builder();

    public VisualStudioProject parse(File file) {
      this.file = file;

      InputStreamReader reader = null;
      currentElement = "";
      XMLInputFactory xmlFactory = XMLInputFactory.newInstance();

      try {
        reader = new InputStreamReader(new FileInputStream(file), Charsets.UTF_8);
        stream = xmlFactory.createXMLStreamReader(reader);

        while (stream.hasNext()) {
          int next = stream.next();
          if (next == XMLStreamConstants.START_ELEMENT) {
            String tagName = stream.getLocalName();
            if (PROPERTYGROUP.equals(tagName)) {
              currentElement = PROPERTYGROUP;
              handlePropertyGroupTag();
            } else if (ITEMGROUP.equals(tagName)) {
              currentElement = ITEMGROUP;
            } else if (currentElement.equals(ITEMGROUP)
                       && ("Compile".equals(tagName)
                       || "ClCompile".equals(tagName)
                       || "ClInclude".equals(tagName))) {
              handleCompileTag();
            } else if ("OutputType".equals(tagName)) {
              handleOutputTypeTag();
            } else if (currentElement.equals(PROPERTYGROUP)
                       && ("AssemblyName".equals(tagName)
                       || "ProjectName".equals(tagName))) {
              handleAssemblyNameTag();
            } else if (currentElement.equals(PROPERTYGROUP)
                       && ("OutputPath".equals(tagName)
                       || "ConfigurationType".equals(tagName))) {
              handleOutputPathTag();
              }
            }
          }
      } catch (IOException e) {
        throw Throwables.propagate(e);
      } catch (XMLStreamException e) {
        throw Throwables.propagate(e);
      } finally {
        closeXmlStream();
        Closeables.closeQuietly(reader);
      }

      return new VisualStudioProject(filesBuilder.build(), outputType, assemblyName, propertyGroupConditionsBuilder.build(), outputPathsBuilder.build());
    }

    private void closeXmlStream() {
      if (stream != null) {
        try {
          stream.close();
        } catch (XMLStreamException e) {
          throw Throwables.propagate(e);
        }
      }
    }

    private void handleCompileTag() {
      String include = getRequiredAttribute("Include");
      if (include != null) {
        filesBuilder.add(include);
      }
    }

    private void handleOutputTypeTag() throws XMLStreamException {
      outputType = stream.getElementText();
    }

    private void handleAssemblyNameTag() throws XMLStreamException {
      assemblyName = stream.getElementText();
    }

    private void handlePropertyGroupTag() throws XMLStreamException {
      currentCondition = Strings.nullToEmpty(getAttribute("Condition"));
      if (!currentCondition.isEmpty()) {
      }
    }

    private void handleOutputPathTag() throws XMLStreamException {
      propertyGroupConditionsBuilder.add(currentCondition);
      outputPathsBuilder.add(stream.getElementText());
    }

    private String getRequiredAttribute(String name) {
      String value = getAttribute(name);
      if (value != null) {
      }

      return value;
    }

    @Nullable
    private String getAttribute(String name) {
      for (int i = 0; i < stream.getAttributeCount(); i++) {
        if (name.equals(stream.getAttributeLocalName(i))) {
          return stream.getAttributeValue(i);
        }
      }

      return null;
    }

  }

}
