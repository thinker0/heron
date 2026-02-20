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

package org.apache.heron.statemgr.localfs;

import java.util.concurrent.TimeUnit;

import com.google.common.util.concurrent.ListenableFuture;

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

import org.apache.heron.common.basics.FileUtils;
import org.apache.heron.proto.scheduler.Scheduler;
import org.apache.heron.spi.common.Config;
import org.apache.heron.spi.common.Key;
import org.apache.heron.spi.statemgr.IStateManager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"jdk.internal.reflect.*", "javax.net.ssl.*", "javax.xml.*", "javax.management.*", "org.w3c.dom.*", "org.xml.sax.*", "javax.xml.parsers.*", "javax.xml.datatype.*"})
@PrepareForTest(FileUtils.class)
public class LocalFileSystemStateManagerTest {

  private static final String TOPOLOGY_NAME = "topologyName";
  private static final String ROOT_ADDR = "";
  private LocalFileSystemStateManager manager;

  @Before
  public void before() throws Exception {
    Config config = Config.newBuilder()
        .put(Key.STATEMGR_ROOT_PATH, ROOT_ADDR)
        .put(LocalFileSystemKey.IS_INITIALIZE_FILE_TREE.value(), false)
        .build();
    manager = spy(new LocalFileSystemStateManager());
    manager.initialize(config);
    
    PowerMockito.mockStatic(FileUtils.class);
  }

  @After
  public void after() throws Exception {
  }

  @Test
  public void testInitialize() throws Exception {
    PowerMockito.when(FileUtils.createDirectory(anyString())).thenReturn(true);
    assertTrue(manager.initTree());
    PowerMockito.verifyStatic(FileUtils.class, atLeastOnce());
    FileUtils.createDirectory(anyString());
  }

  @Test
  public void testGetSchedulerLocation() throws Exception {
    Scheduler.SchedulerLocation location = Scheduler.SchedulerLocation.newBuilder()
        .setHttpEndpoint("host:1")
        .setTopologyName(TOPOLOGY_NAME)
        .build();
    
    PowerMockito.when(FileUtils.isFileExists(anyString())).thenReturn(true);
    PowerMockito.when(FileUtils.hasChildren(anyString())).thenReturn(false);
    PowerMockito.when(FileUtils.readFromFile(anyString())).thenReturn(location.toByteArray());

    Scheduler.SchedulerLocation locationFetched =
        manager.getSchedulerLocation(null, TOPOLOGY_NAME).get();

    assertEquals(location, locationFetched);
  }

  @Test
  public void testSetSchedulerLocation() throws Exception {
    doReturn(com.google.common.util.concurrent.SettableFuture.create()).when(manager)
        .setData(anyString(), any(byte[].class), anyBoolean());

    manager.setSchedulerLocation(Scheduler.SchedulerLocation.getDefaultInstance(), TOPOLOGY_NAME);
    verify(manager).setData(anyString(), any(byte[].class), eq(true));
  }

  private void assertWriteToFile(String path) throws Exception {
    PowerMockito.verifyStatic(FileUtils.class);
    FileUtils.writeToFile(eq(path), any(byte[].class), anyBoolean());
  }

  private void assertDeleteFile(String path) throws Exception {
    PowerMockito.verifyStatic(FileUtils.class);
    FileUtils.deleteFile(eq(path));
  }

  @Test
  public void testDeleteTopology() throws Exception {
    PowerMockito.when(FileUtils.deleteFile(anyString())).thenReturn(true);
    PowerMockito.when(FileUtils.isFileExists(anyString())).thenReturn(true);
    PowerMockito.when(FileUtils.hasChildren(anyString())).thenReturn(false);
    manager.deleteTopology(TOPOLOGY_NAME);
    assertDeleteFile("/topologies/" + TOPOLOGY_NAME);
  }
}