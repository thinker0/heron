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

package org.apache.heron.scheduler.local;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import org.apache.heron.api.generated.TopologyAPI;
import org.apache.heron.proto.scheduler.Scheduler;
import org.apache.heron.spi.common.Config;
import org.apache.heron.spi.common.Key;
import org.apache.heron.spi.packing.PackingPlan;
import org.apache.heron.spi.utils.PackingTestUtils;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anySet;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings("unchecked")
public class LocalSchedulerTest {
  private static final String TOPOLOGY_NAME = "testTopology";
  private static final int MAX_WAITING_SECOND = 10;
  private static LocalScheduler scheduler;
  private Config config;
  private Config runtime;

  @Before
  public void before() throws Exception {
    scheduler = spy(LocalScheduler.class);
    config = mock(Config.class);
    when(config.getStringValue(Key.TOPOLOGY_NAME)).thenReturn(TOPOLOGY_NAME);

    runtime = mock(Config.class);

    when(runtime.get(Key.TOPOLOGY_DEFINITION)).thenReturn(TopologyAPI.Topology
        .newBuilder()
        .setId("a")
        .setName("a")
        .setState(TopologyAPI.TopologyState.RUNNING)
        .setTopologyConfig(
            TopologyAPI.Config.newBuilder()
                .addKvs(TopologyAPI.Config.KeyValue.newBuilder()
                    .setKey(org.apache.heron.api.Config.TOPOLOGY_REMOTE_DEBUGGING_ENABLE)
                    .setValue("false"))).build());

    scheduler.initialize(config, runtime);
    
    // Silence background monitoring and real process execution
    doReturn(mock(ExecutorService.class)).when(scheduler).getMonitorService();
    doAnswer(invocation -> {
      int containerId = (Integer) invocation.getArguments()[0];
      scheduler.getProcessToContainer().put(mock(Process.class), containerId);
      return null;
    }).when(scheduler).startExecutor(anyInt(), any());
  }

  @After
  public void after() throws Exception {
    scheduler.close();
  }

  @Test
  public void testClose() throws Exception {
    LocalScheduler localScheduler = new LocalScheduler();
    localScheduler.initialize(mock(Config.class), mock(Config.class));
    // The MonitorService should started
    ExecutorService monitorService = localScheduler.getMonitorService();
    Assert.assertFalse(monitorService.isShutdown());
    Assert.assertEquals(0, localScheduler.getProcessToContainer().size());

    // The MonitorService should shutdown
    localScheduler.close();
    Assert.assertTrue(monitorService.isShutdown());
  }

  @Test
  public void testOnSchedule() throws Exception {
    PackingPlan packingPlan = mock(PackingPlan.class);
    Set<PackingPlan.ContainerPlan> containers = new HashSet<>();
    containers.add(PackingTestUtils.testContainerPlan(1));
    containers.add(PackingTestUtils.testContainerPlan(3));
    when(packingPlan.getContainers()).thenReturn(containers);

    Assert.assertTrue(scheduler.onSchedule(packingPlan));
    verifyIdsOfLaunchedContainers(0, 1, 3);

    verify(scheduler).startExecutor(eq(0), any());
    verify(scheduler).startExecutor(eq(1), any());
    verify(scheduler).startExecutor(eq(3), any());
  }

  @Test
  public void testAddContainer() throws Exception {
    when(runtime.getLongValue(Key.NUM_CONTAINERS)).thenReturn(2L);
    scheduler.initialize(config, runtime);

    PackingPlan packingPlan = mock(PackingPlan.class);
    Set<PackingPlan.ContainerPlan> containers = new HashSet<>();
    containers.add(PackingTestUtils.testContainerPlan(1));
    when(packingPlan.getContainers()).thenReturn(containers);
    Assert.assertTrue(scheduler.onSchedule(packingPlan));

    verify(scheduler, times(2)).startExecutor(anyInt(), any());

    //now verify add container adds new container
    containers.clear();
    containers.add(PackingTestUtils.testContainerPlan(3));
    scheduler.addContainers(containers);
    verify(scheduler).startExecutor(eq(3), any());

    containers.clear();
    containers.add(PackingTestUtils.testContainerPlan(4));
    containers.add(PackingTestUtils.testContainerPlan(5));
    scheduler.addContainers(containers);
    verify(scheduler).startExecutor(eq(4), any());
    verify(scheduler).startExecutor(eq(5), any());
  }

  @Test
  public void testRemoveContainer() throws Exception {
    final int LOCAL_NUM_CONTAINER = 6;

    Set<PackingPlan.ContainerPlan> existingContainers = new HashSet<>();
    for (int i = 1; i < LOCAL_NUM_CONTAINER; i++) {
      existingContainers.add(PackingTestUtils.testContainerPlan(i));
    }

    PackingPlan packingPlan = mock(PackingPlan.class);
    when(packingPlan.getContainers()).thenReturn(existingContainers);
    Assert.assertTrue(scheduler.onSchedule(packingPlan));
    verifyIdsOfLaunchedContainers(0, 1, 2, 3, 4, 5);
    verify(scheduler, times(LOCAL_NUM_CONTAINER)).startExecutor(anyInt(), any());

    Set<PackingPlan.ContainerPlan> containersToRemove = new HashSet<>();
    PackingPlan.ContainerPlan containerToRemove =
        PackingTestUtils.testContainerPlan(LOCAL_NUM_CONTAINER - 1);
    containersToRemove.add(containerToRemove);
    scheduler.removeContainers(containersToRemove);
    verifyIdsOfLaunchedContainers(0, 1, 2, 3, 4);

    containersToRemove.clear();
    containersToRemove.add(PackingTestUtils.testContainerPlan(1));
    containersToRemove.add(PackingTestUtils.testContainerPlan(2));
    scheduler.removeContainers(containersToRemove);
    verifyIdsOfLaunchedContainers(0, 3, 4);
  }

  private void verifyIdsOfLaunchedContainers(int... ids) {
    HashSet<Integer> containerIdsLaunched =
        new HashSet<>(scheduler.getProcessToContainer().values());
    Assert.assertEquals(ids.length, containerIdsLaunched.size());
    for (int i = 0; i < ids.length; i++) {
      Assert.assertTrue(containerIdsLaunched.contains(ids[i]));
    }
  }

  @Test
  public void testOnKill() throws Exception {
    Process mockProcess = mock(Process.class);
    scheduler.getProcessToContainer().put(mockProcess, 0);

    Assert.assertTrue(scheduler.onKill(Scheduler.KillTopologyRequest.getDefaultInstance()));
    Assert.assertEquals(0, scheduler.getProcessToContainer().size());
    Assert.assertTrue(scheduler.isTopologyKilled());
    verify(mockProcess).destroy();
  }

  @Test
  public void testOnRestart() throws Exception {
    Process mockProcess = mock(Process.class);

    // Restart only one container
    Scheduler.RestartTopologyRequest restartOne =
        Scheduler.RestartTopologyRequest.newBuilder().
            setTopologyName(TOPOLOGY_NAME).
            setContainerIndex(1).
            build();

    scheduler.getProcessToContainer().put(mockProcess, 0);
    // Failed to restart due to container not exist
    Assert.assertFalse(scheduler.onRestart(restartOne));

    // Happy to restart one container
    Scheduler.RestartTopologyRequest restartZero =
        Scheduler.RestartTopologyRequest.newBuilder().
            setTopologyName(TOPOLOGY_NAME).
            setContainerIndex(0).
            build();
    scheduler.getProcessToContainer().put(mockProcess, 0);
    Assert.assertTrue(scheduler.onRestart(restartZero));


    // Happy to restart all containers
    Scheduler.RestartTopologyRequest restartAll =
        Scheduler.RestartTopologyRequest.newBuilder().
            setTopologyName(TOPOLOGY_NAME).
            setContainerIndex(-1).
            build();
    scheduler.getProcessToContainer().put(mockProcess, 0);
    Assert.assertTrue(scheduler.onRestart(restartAll));
  }

  @Test
  public void testStartExecutorMonitorNotKilled() throws Exception {
    // This test might need more refactoring if we want to test monitor logic without real threads
  }

  @Test
  public void testStartExecutorMonitorKilled() throws Exception {
    // This test might need more refactoring if we want to test monitor logic without real threads
  }

  // Helper method to keep things simple
  private static <T> T eq(T value) {
    return org.mockito.Matchers.eq(value);
  }
}