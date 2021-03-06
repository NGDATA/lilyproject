/*
 * Copyright 2013 NGDATA nv
 * Copyright 2008 Outerthought bvba and Schaubroeck nv
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.lilyproject.runtime.test;

import java.io.File;

import org.lilyproject.runtime.model.LilyRuntimeModel;
import org.lilyproject.runtime.model.ModuleDefinition;
import org.lilyproject.runtime.model.ModuleSourceType;
import org.lilyproject.runtime.testfw.AbstractRuntimeTest;

/**
 * Test an 'empty' module.
 */
public class EmptyModuleTest extends AbstractRuntimeTest {
    protected LilyRuntimeModel getRuntimeModel() throws Exception {
        File moduleDir = createModule("org.lilyproject.runtime.test.testmodules.emptymodule");

        LilyRuntimeModel model = new LilyRuntimeModel();

        ModuleDefinition module = new ModuleDefinition("foo", moduleDir, ModuleSourceType.EXPANDED_JAR);
        model.addModule(module);

        return model;
    }

    public void testEmptyModule() throws Exception {
    }
}
