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

package org.apache.heron.statemgr.zookeeper.curator;

import java.lang.reflect.Field;
import java.util.concurrent.TimeUnit;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.protobuf.Message;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.api.BackgroundCallback;
import org.apache.curator.framework.api.CuratorEvent;
import org.apache.curator.framework.api.BackgroundPathable;
import org.apache.curator.framework.api.ErrorListenerPathable;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import org.apache.heron.proto.system.ExecutionEnvironment;
import org.apache.heron.spi.common.Config;
import org.apache.heron.spi.common.Key;
import org.apache.heron.statemgr.zookeeper.ZkContext;
import org.apache.zookeeper.Watcher;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

public class CuratorStateManagerTest {

  private static final String CONNECTION_STRING = "connectionString";
  private static final String PATH = "/heron/n90/path";
  private static final String ROOT_ADDR = "/";
  private static final String TOPOLOGY_NAME = "topology";
  private static final String TOPOLOGY_ID = "topology-id";

  private Config config;

  @Before
  public void before() throws Exception {
    config = Config.newBuilder(true)
        .put(Key.STATEMGR_ROOT_PATH, ROOT_ADDR)
        .put(Key.TOPOLOGY_NAME, TOPOLOGY_NAME)
        .put(Key.STATEMGR_CONNECTION_STRING, CONNECTION_STRING)
        .put(ZkContext.IS_INITIALIZE_TREE, false)
        .build();
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testGetNodeData() throws Exception {
    CuratorStateManager stateManager = new CuratorStateManager();
    CuratorFramework mockClient = mock(CuratorFramework.class);
    CuratorEvent mockEvent = mock(CuratorEvent.class);
    
    Message.Builder mockBuilder = ExecutionEnvironment.ExecutionState.newBuilder()
        .setTopologyName(TOPOLOGY_NAME)
        .setTopologyId(TOPOLOGY_ID)
        .setCluster("cluster")
        .setEnviron("environ")
        .setRole("role");
    Message mockMessage = mockBuilder.build();
    byte[] data = mockMessage.toByteArray();

    // Set client field directly
    Field clientField = CuratorStateManager.class.getDeclaredField("client");
    clientField.setAccessible(true);
    clientField.set(stateManager, mockClient);

    org.apache.curator.framework.api.GetDataBuilder mockGetDataBuilder = mock(org.apache.curator.framework.api.GetDataBuilder.class);
    // Use Mockito settings to support multiple interfaces
    BackgroundPathable<byte[]> mockPathable = mock(BackgroundPathable.class, 
        withSettings().extraInterfaces(ErrorListenerPathable.class));

    when(mockClient.getData()).thenReturn(mockGetDataBuilder);
    when(mockGetDataBuilder.usingWatcher(any(Watcher.class))).thenReturn(mockPathable);
    
    final BackgroundCallback[] cbCapture = new BackgroundCallback[1];
    when(mockPathable.inBackground(any(BackgroundCallback.class))).thenAnswer(inv -> {
      cbCapture[0] = (BackgroundCallback) inv.getArguments()[0];
      return mockPathable;
    });
    
    when(mockPathable.forPath(anyString())).thenAnswer(inv -> {
      if (cbCapture[0] != null) {
        cbCapture[0].processResult(mockClient, mockEvent);
      }
      return null;
    });

    doReturn(data).when(mockEvent).getData();
    doReturn(0).when(mockEvent).getResultCode();

    ListenableFuture<Message> result = stateManager.getNodeData(mock(org.apache.heron.spi.statemgr.WatchCallback.class), PATH, mockBuilder);
    Assert.assertEquals(mockMessage, result.get());
  }

  @Test
  public void testInitialize() throws Exception {
    CuratorStateManager spyStateManager = spy(new CuratorStateManager());
    CuratorFramework mockClient = mock(CuratorFramework.class);
    doReturn(mockClient).when(spyStateManager).getCuratorClient();
    doReturn(true).when(mockClient).blockUntilConnected(anyInt(), any(TimeUnit.class));

    spyStateManager.initialize(config);
    verify(mockClient).start();
  }
}