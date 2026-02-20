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

package org.apache.heron.scheduler.nomad;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.common.base.Optional;
import com.hashicorp.nomad.apimodel.Job;
import com.hashicorp.nomad.apimodel.Task;
import com.hashicorp.nomad.apimodel.TaskGroup;
import com.hashicorp.nomad.javasdk.NomadApiClient;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import org.apache.heron.api.generated.TopologyAPI;
import org.apache.heron.common.basics.ByteAmount;
import org.apache.heron.common.basics.PackageType;
import org.apache.heron.proto.scheduler.Scheduler;
import org.apache.heron.scheduler.utils.SchedulerUtils;
import org.apache.heron.spi.common.Config;
import org.apache.heron.spi.common.Key;
import org.apache.heron.spi.packing.PackingPlan;
import org.apache.heron.spi.packing.Resource;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"jdk.internal.reflect.*", "javax.net.ssl.*", "javax.xml.*", "javax.management.*", "org.w3c.dom.*", "org.xml.sax.*", "javax.xml.parsers.*", "javax.xml.datatype.*"})
@PrepareForTest({NomadScheduler.class, Job.class, SchedulerUtils.class, NomadApiClient.class})
public class NomadSchedulerTest {
  private static final String TOPOLOGY_NAME = "topology-name";
  private static final String TOPOLOGY_ID = "topology-id";
  private static final String SCHEDULER_URI = "http://localhost:4646";
  private static final String PACKING_PLAN_ID = "packing-plan-id";
  private static final int HERON_NOMAD_CORE_FREQ_MAPPING = 1000;
  private static final String CORE_PACKAGE_URI = "core-package-uri";
  private static final Boolean USE_CORE_PACKAGE_URI = true;
  private static final String EXECUTOR_BINARY = "executor-binary";

  private NomadScheduler scheduler;
  private Config mockRuntime;
  private Config mockConfig;
  private NomadApiClient mockApiClient;

  @Before
  public void setUp() throws Exception {
    mockConfig = Config.newBuilder()
        .put(Key.TOPOLOGY_NAME, TOPOLOGY_NAME)
        .put(Key.TOPOLOGY_ID, TOPOLOGY_ID)
        .put(NomadContext.HERON_NOMAD_SCHEDULER_URI, SCHEDULER_URI)
        .put(NomadContext.HERON_NOMAD_CORE_FREQ_MAPPING, HERON_NOMAD_CORE_FREQ_MAPPING)
        .put(Key.CORE_PACKAGE_URI, CORE_PACKAGE_URI)
        .put(Key.USE_CORE_PACKAGE_URI, USE_CORE_PACKAGE_URI)
        .put(Key.EXECUTOR_BINARY, EXECUTOR_BINARY)
        .put(NomadContext.HERON_NOMAD_DRIVER, NomadConstants.NomadDriver.RAW_EXEC.getName())
        .put(NomadContext.HERON_NOMAD_NETWORK_MODE, "default")
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
        .put(Key.DOWNLOADER_BINARY, "heron-downloader")
        .put(Key.HERON_CONF, "/etc/heron")
        .put(Key.HERON_HOME, "/usr/local/heron")
        .build();

    mockRuntime = Config.newBuilder()
        .put(Key.TOPOLOGY_DEFINITION, TopologyAPI.Topology.newBuilder()
            .setName(TOPOLOGY_NAME)
            .setId(TOPOLOGY_ID)
            .setState(TopologyAPI.TopologyState.RUNNING)
            .build())
        .put(Key.NUM_CONTAINERS, 2L)
        .put(Key.TOPOLOGY_ID, TOPOLOGY_ID)
        .put(Key.TOPOLOGY_NAME, TOPOLOGY_NAME)
        .put(Key.SCHEDULER_PROPERTIES, new java.util.Properties())
        .put(Key.TOPOLOGY_PACKAGE_URI, java.net.URI.create("http://topology.package.uri"))
        .build();

    mockApiClient = mock(NomadApiClient.class);
    
    // Use spy/mockStatic carefully to avoid real method calls
    PowerMockito.mockStatic(NomadScheduler.class, Mockito.CALLS_REAL_METHODS);
    PowerMockito.doReturn(mockApiClient).when(NomadScheduler.class, "getApiClient", anyString());
    PowerMockito.doReturn("heron_nomad_script").when(NomadScheduler.class, "getHeronNomadScript", any());

    scheduler = spy(new NomadScheduler());
  }

  @Test
  public void testOnSchedule() throws Exception {
    scheduler.initialize(this.mockConfig, this.mockRuntime);
    Set<PackingPlan.ContainerPlan> containers = new HashSet<>();
    containers.add(mock(PackingPlan.ContainerPlan.class));
    PackingPlan validPlan = new PackingPlan(PACKING_PLAN_ID, containers);

    Job[] jobs = {new Job(), new Job(), new Job()};
    List<Job> jobList = Arrays.asList(jobs);
    Mockito.doReturn(jobList).when(scheduler).getJobs(validPlan);
    
    // Fail case
    PowerMockito.doThrow(new RuntimeException()).when(NomadScheduler.class, "startJobs", any(), any());
    Assert.assertFalse(scheduler.onSchedule(validPlan));

    // Success case
    PowerMockito.doNothing().when(NomadScheduler.class, "startJobs", any(), any());
    Assert.assertTrue(scheduler.onSchedule(validPlan));
  }

  @Test
  public void testOnRestart() throws Exception {
    scheduler.initialize(this.mockConfig, this.mockRuntime);
    Scheduler.RestartTopologyRequest request = Scheduler.RestartTopologyRequest.newBuilder()
        .setTopologyName(TOPOLOGY_NAME)
        .setContainerIndex(-1)
        .build();
    
    Job[] jobs = {new Job()};
    List<Job> jobList = Arrays.asList(jobs);
    PowerMockito.doReturn(jobList).when(NomadScheduler.class, "getTopologyJobs", any(), anyString());

    // Fail case
    PowerMockito.doThrow(new RuntimeException()).when(NomadScheduler.class, "restartJobs", any(), any());
    Assert.assertFalse(scheduler.onRestart(request));

    // Success case
    PowerMockito.doNothing().when(NomadScheduler.class, "restartJobs", any(), any());
    Assert.assertTrue(scheduler.onRestart(request));
  }

  @Test
  public void testOnKill() throws Exception {
    scheduler.initialize(this.mockConfig, this.mockRuntime);
    Scheduler.KillTopologyRequest request = Scheduler.KillTopologyRequest.newBuilder()
        .setTopologyName(TOPOLOGY_NAME)
        .build();

    Job[] jobs = {new Job()};
    List<Job> jobList = Arrays.asList(jobs);
    PowerMockito.doReturn(jobList).when(NomadScheduler.class, "getTopologyJobs", any(), anyString());

    // Fail case
    PowerMockito.doThrow(new RuntimeException()).when(NomadScheduler.class, "killJobs", any(), any());
    Assert.assertFalse(scheduler.onKill(request));

    // Success case
    PowerMockito.doNothing().when(NomadScheduler.class, "killJobs", any(), any());
    Assert.assertTrue(scheduler.onKill(request));
  }

  @Test
  public void testGetJobLinks() {
    scheduler.initialize(this.mockConfig, this.mockRuntime);
    Assert.assertFalse(scheduler.getJobLinks().isEmpty());
  }

  @Test
  public void testGetTaskGroup() {
    scheduler.initialize(this.mockConfig, this.mockRuntime);
    TaskGroup taskGroup = scheduler.getTaskGroup("group-name", 0, new Resource(1.0, ByteAmount.fromMegabytes(100), ByteAmount.fromMegabytes(1000)));
    Assert.assertNotNull(taskGroup);
  }

  @Test
  public void testGetTaskRawExec() {
    scheduler.initialize(this.mockConfig, this.mockRuntime);
    Task task = scheduler.getTask("task-name", 1, new Resource(1.0, ByteAmount.fromMegabytes(100), ByteAmount.fromMegabytes(1000)));
    Assert.assertEquals("raw_exec", task.getDriver());
  }

  @Test
  public void testGetTaskDocker() {
    Config config = Config.newBuilder()
        .putAll(this.mockConfig)
        .put(NomadContext.HERON_NOMAD_DRIVER, NomadConstants.NomadDriver.DOCKER.getName())
        .build();
    scheduler.initialize(config, this.mockRuntime);
    Task task = scheduler.getTask("task-name", 1, new Resource(1.0, ByteAmount.fromMegabytes(100), ByteAmount.fromMegabytes(1000)));
    Assert.assertEquals("docker", task.getDriver());
  }

  @Test
  public void testServiceCheck() {
    scheduler.initialize(this.mockConfig, this.mockRuntime);
    Task task = scheduler.getTask("task-name", 1, new Resource(1.0, ByteAmount.fromMegabytes(100), ByteAmount.fromMegabytes(1000)));
    Assert.assertNull(task.getServices());
  }

  @Test
  public void testGetJob() {
    scheduler.initialize(this.mockConfig, this.mockRuntime);
    Job job = scheduler.getJob(1, Optional.absent(), new Resource(1.0, ByteAmount.fromMegabytes(100), ByteAmount.fromMegabytes(1000)));
    Assert.assertEquals(TOPOLOGY_ID + "-1", job.getId());
  }
}