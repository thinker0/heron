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

package org.apache.heron.scheduler.slurm;

import java.util.HashSet;
import java.util.Set;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import org.apache.heron.api.generated.TopologyAPI;
import org.apache.heron.common.basics.PackageType;
import org.apache.heron.proto.scheduler.Scheduler;
import org.apache.heron.spi.common.Config;
import org.apache.heron.spi.common.Key;
import org.apache.heron.spi.packing.PackingPlan;
import org.apache.heron.spi.utils.PackingTestUtils;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"jdk.internal.reflect.*", "javax.net.ssl.*", "javax.xml.*", "javax.management.*", "org.w3c.dom.*", "org.xml.sax.*", "javax.xml.parsers.*", "javax.xml.datatype.*"})
@PrepareForTest(SlurmContext.class)
public class SlurmSchedulerTest {
  private static final String TOPOLOGY_NAME = "topologyName";
  private static final String TOPOLOGY_ID = "topologyId";

  private SlurmScheduler scheduler;

  @BeforeClass
  public static void beforeClass() throws Exception {
  }

  @AfterClass
  public static void afterClass() throws Exception {
  }

  @Before
  public void setUp() throws Exception {
    scheduler = Mockito.spy(new SlurmScheduler());
  }

  @After
  public void after() throws Exception {
  }

  @Test
  public void testOnKill() throws Exception {
    SlurmController controller = Mockito.mock(SlurmController.class);
    Mockito.doReturn(controller).when(scheduler).getController();
    Mockito.doReturn(true).when(controller).killJob(Matchers.anyString());

    Config config = Config.newBuilder()
        .put(Key.TOPOLOGY_NAME, TOPOLOGY_NAME)
        .build();
    Config runtime = Config.newBuilder()
        .put(Key.TOPOLOGY_NAME, TOPOLOGY_NAME)
        .build();
    scheduler.initialize(config, runtime);

    Assert.assertTrue(scheduler.onKill(Scheduler.KillTopologyRequest.getDefaultInstance()));
    Mockito.verify(controller).killJob(Matchers.anyString());
  }

  @Test
  public void testOnSchedule() throws Exception {
    SlurmController controller = Mockito.mock(SlurmController.class);
    Mockito.doReturn(controller).when(scheduler).getController();

    Config config = Config.newBuilder()
        .put(Key.TOPOLOGY_NAME, TOPOLOGY_NAME)
        .put(Key.TOPOLOGY_ID, TOPOLOGY_ID)
        .put(Key.TOPOLOGY_DEFINITION_FILE, "topology.defn")
        .put(Key.STATEMGR_CONNECTION_STRING, "127.0.0.1")
        .put(Key.STATEMGR_ROOT_PATH, "/heron")
        .put(Key.TMANAGER_BINARY, "tmanager")
        .put(Key.STMGR_BINARY, "stmgr")
        .put(Key.METRICSMGR_CLASSPATH, "classpath")
        .put(Key.TOPOLOGY_BINARY_FILE, "topology.jar")
        .put(Key.SYSTEM_YAML, "system.yaml")
        .put(Key.OVERRIDE_YAML, "override.yaml")
        .put(Key.TOPOLOGY_PACKAGE_TYPE, PackageType.TAR)
        .put(Key.VERBOSE_GC, Boolean.FALSE)
        .put(Key.SHELL_BINARY, "heron-shell")
        .put(Key.CLUSTER, "test-cluster")
        .put(Key.ROLE, "test-role")
        .put(Key.ENVIRON, "test-env")
        .put(Key.INSTANCE_CLASSPATH, "instance-classpath")
        .put(Key.METRICS_YAML, "metrics-sink.yaml")
        .put(Key.SCHEDULER_CLASSPATH, "scheduler-classpath")
        .put(Key.PACKING_CLASSPATH, "packing-classpath")
        .put(Key.STATEMGR_CLASSPATH, "statemgr-classpath")
        .build();
    Config runtime = Config.newBuilder()
        .put(Key.TOPOLOGY_NAME, TOPOLOGY_NAME)
        .put(Key.TOPOLOGY_ID, TOPOLOGY_ID)
        .put(Key.TOPOLOGY_DEFINITION, TopologyAPI.Topology.newBuilder()
            .setName(TOPOLOGY_NAME)
            .setId(TOPOLOGY_ID)
            .setState(TopologyAPI.TopologyState.RUNNING)
            .build())
        .put(Key.NUM_CONTAINERS, 2L)
        .build();
    scheduler.initialize(config, runtime);

    // Failed
    Mockito.doReturn(false).when(controller).createJob(
        Matchers.anyString(), Matchers.any(), Matchers.any(String[].class),
        Matchers.anyString(), Matchers.anyLong());
    
    PackingPlan packingPlan = Mockito.mock(PackingPlan.class);
    Set<PackingPlan.ContainerPlan> containers = new HashSet<>();
    containers.add(PackingTestUtils.testContainerPlan(1));
    Mockito.when(packingPlan.getContainers()).thenReturn(containers);

    Assert.assertFalse(scheduler.onSchedule(packingPlan));

    // Happy path
    Mockito.doReturn(true).when(controller).createJob(
        Matchers.anyString(), Matchers.any(), Matchers.any(String[].class),
        Matchers.anyString(), Matchers.anyLong());
    Assert.assertTrue(scheduler.onSchedule(packingPlan));
  }
}