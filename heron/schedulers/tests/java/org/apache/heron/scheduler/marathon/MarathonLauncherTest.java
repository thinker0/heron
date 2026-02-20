/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.heron.scheduler.marathon;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import org.apache.heron.scheduler.utils.LauncherUtils;
import org.apache.heron.spi.common.Config;
import org.apache.heron.spi.packing.PackingPlan;
import org.apache.heron.spi.scheduler.IScheduler;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"jdk.internal.reflect.*", "javax.net.ssl.*", "javax.xml.*", "javax.management.*", "org.w3c.dom.*", "org.xml.sax.*", "javax.xml.parsers.*", "javax.xml.datatype.*"})
@PrepareForTest(LauncherUtils.class)
public class MarathonLauncherTest {

  @Test
  public void testLaunch() throws Exception {
    Config config = Config.newBuilder().build();
    MarathonLauncher launcher = Mockito.spy(MarathonLauncher.class);
    launcher.initialize(config, config);

    LauncherUtils mockLauncherUtils = Mockito.mock(LauncherUtils.class);
    PowerMockito.mockStatic(LauncherUtils.class);
    PowerMockito.doReturn(mockLauncherUtils).when(LauncherUtils.class, "getInstance");

    // Launched successfully
    Mockito.when(mockLauncherUtils.onScheduleAsLibrary(
        Mockito.any(),
        Mockito.any(),
        Mockito.any(),
        Mockito.any())).thenReturn(true);

    Assert.assertTrue(launcher.launch(Mockito.mock(PackingPlan.class)));
    Mockito.verify(mockLauncherUtils).onScheduleAsLibrary(
        Mockito.any(),
        Mockito.any(),
        Mockito.any(),
        Mockito.any()
    );

    // Failed to launch
    Mockito.when(mockLauncherUtils.onScheduleAsLibrary(
        Mockito.any(),
        Mockito.any(),
        Mockito.any(),
        Mockito.any())).thenReturn(false);

    Assert.assertFalse(launcher.launch(Mockito.mock(PackingPlan.class)));
    Mockito.verify(mockLauncherUtils, Mockito.times(2)).onScheduleAsLibrary(
        Mockito.any(),
        Mockito.any(),
        Mockito.any(),
        Mockito.any()
    );

    launcher.close();
  }
}
