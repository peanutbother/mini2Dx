/*******************************************************************************
 * Copyright 2019 See AUTHORS file
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.mini2Dx.ui.xml;

import org.jmock.Expectations;
import org.junit.Test;
import org.mini2Dx.core.exception.MdxException;
import org.mini2Dx.core.files.FileHandle;
import org.mini2Dx.ui.element.Container;
import org.mini2Dx.ui.element.Visibility;

import java.io.IOException;
import java.io.StringReader;

import static org.junit.Assert.*;

public class UiXmlLoaderTest extends AbstractUiXmlLoaderTest {

    @Test
    public void unknown_tag() {
        String xml = "<does-not-exist/>";

        try {
            loadFile(xml);
            fail();
        } catch (MdxException e) {
            String error = e.getCause().getMessage();
            assertEquals("Ui xml tag (does-not-exist) is currently not supported. Did you make a typing error?", error);
        }
    }

    @Test
    public void namespace_can_be_aliased() {
        String xml =
                "<ui:container xmlns:ui=\"https://github.com/mini2Dx/mini2Dx\">" +
                        "</ui:container>";

        Container container = loadFile(xml);
        assertNotNull(container);
    }

    @Test
    public void nested_containers() {
        String xml =
                "<container id=\"1\">" +
                        "  <container id=\"2\">" +
                        "    <container id=\"3\"/>" +
                        "  </container>" +
                        "</container>";

        Container container = loadFile(xml);
        assertEquals("1", container.getId());
        assertEquals(1, container.getTotalChildren());

        container = (Container) container.get(0);
        assertEquals("2", container.getId());
        assertEquals(1, container.getTotalChildren());

        container = (Container) container.get(0);
        assertEquals("3", container.getId());
        assertEquals(0, container.getTotalChildren());
    }

    @Test
    public void fail_to_parse_file_contents() {
        String badFileFormat = "{\"hello\": \"world\"}";

        FileHandle fileHandle = mockery.mock(FileHandle.class);
        mockery.checking(new Expectations() {
            {
                oneOf(fileHandleResolver).resolve(filename);
                try {
                    oneOf(fileHandle).reader();
                    will(returnValue(new StringReader(badFileFormat)));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });

        try {
            loader.load(filename);
            fail();
        } catch (Exception e) {
            assertEquals("Failed to load UI file: " + filename, e.getMessage());
            assertNotNull(e.getCause());
        }
    }

    @Test
    public void fail_to_resolve_file() {
        RuntimeException realError = new RuntimeException("BOOM");
        mockery.checking(new Expectations() {
            {
                oneOf(fileHandleResolver).resolve(filename);
                will(throwException(realError));
            }
        });

        try {
            loader.load(filename);
            fail();
        } catch (Exception e) {
            assertEquals("Failed to load UI file: " + filename, e.getMessage());
            assertSame(e.getCause(), realError);
        }
    }

    @Test
    public void load_with_model() {
        String xml = "<container id=\"testContainer\" visibility=\"HIDDEN\" />";
        UiModel model = loadFileWithModel(xml, UiModel.class);

        assertNotNull(model.testContainer);
        assertNotNull(model.testContainerCustom);
        assertEquals(model.testContainer.getVisibility(), Visibility.HIDDEN);
        assertEquals(model.testContainerCustom.getVisibility(), Visibility.HIDDEN);
    }

    @Test
    public void fail_to_load_with_model_null() {
        String xml = "<container id=\"testContainer\" visibility=\"HIDDEN\" />";

        try {
            loadFileWithModel(xml, null);
        } catch (MdxException e) {
            assertEquals("Failed to populate Ui file " + filename, e.getMessage());
            assertEquals("container or model is null", e.getCause().getMessage());
        }
    }
}