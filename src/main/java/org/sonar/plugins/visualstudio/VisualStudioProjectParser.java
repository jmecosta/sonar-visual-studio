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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VisualStudioProjectParser {
  public static final String PROPERTYGROUP = "PropertyGroup";
  public static final String ITEMGROUP = "ItemGroup";
  static final Logger logger = LoggerFactory.getLogger(VisualStudioProjectParser.class);

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

      logger.debug("FileName = " + file.getPath());
      InputStreamReader reader = null;
      currentElement = "";
      XMLInputFactory xmlFactory = XMLInputFactory.newInstance();

      try {
        reader = new InputStreamReader(new FileInputStream(file), Charsets.UTF_8);
        stream = xmlFactory.createXMLStreamReader(reader);

        while (stream.hasNext()) {

          if (stream.next() == XMLStreamConstants.START_ELEMENT) {
            String tagName = stream.getLocalName();
            logger.debug("tag = {} currentElement = {}", tagName, currentElement);
            if (PROPERTYGROUP.equals(tagName)) {
              currentElement = PROPERTYGROUP;
              handlePropertyGroupTag();
            } else if (ITEMGROUP.equals(tagName)) {
              currentElement = ITEMGROUP;
            } else if (currentElement.equals(ITEMGROUP) 
                       && ("Compile".equals(tagName) 
                       || "ClCompile".equals(tagName) 
                       || "ClInclude".equals(tagName)
                       || "Page".equals(tagName))) {
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
            // C++ "ItemDefinitionGroup" contains several preprocessor
            // definitions related to PropertyGroup "Condition"
            // ToDo: create list of definition for configurations e.g.
            // '$(Configuration)|$(Platform)'=='Release|Win32'
          }
//          logger.debug("eventXML = {} level = {}", eventXML, elementNesting);
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
      filesBuilder.add(include);
    }

    private void handleOutputTypeTag() throws XMLStreamException {
      outputType = stream.getElementText();
    }

    private void handleAssemblyNameTag() throws XMLStreamException {
      assemblyName = stream.getElementText();
      logger.debug("Assembly Name = {}", assemblyName);
    }

    private void handlePropertyGroupTag() throws XMLStreamException {
      currentCondition = Strings.nullToEmpty(getAttribute("Condition"));
      if (!currentCondition.isEmpty()) {
        logger.debug("add Condition = {}", currentCondition);
        // VC projects have additional attribute: Label="Configuration"
      }
    }

    private void handleOutputPathTag() throws XMLStreamException {
      // add condition to list if "OutputPath" element is found
      propertyGroupConditionsBuilder.add(currentCondition);
      outputPathsBuilder.add(stream.getElementText());
    }

    private String getRequiredAttribute(String name) {
      String value = getAttribute(name);
      if (value == null) {
        throw parseError("Missing attribute \"" + name + "\" in element <" + stream.getLocalName() + ">");
      }
      logger.debug("RequiredAttribute = {}", value);
      return value;
    }

    @Nullable
    private String getAttribute(String name) {
      for (int i = 0; i < stream.getAttributeCount(); i++) {
        if (name.equals(stream.getAttributeLocalName(i))) {
          logger.debug("attribute = {} value = {}", name, stream.getAttributeValue(i));
          return stream.getAttributeValue(i);
        }
      }

      return null;
    }

    private ParseErrorException parseError(String message) {
      return new ParseErrorException(message + " in " + file.getAbsolutePath() + " at line " + stream.getLocation().getLineNumber());
    }

  }

  private static class ParseErrorException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ParseErrorException(String message) {
      super(message);
    }

  }

}
