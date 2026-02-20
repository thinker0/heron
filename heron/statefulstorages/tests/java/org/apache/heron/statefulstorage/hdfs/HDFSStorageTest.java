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

package org.apache.heron.statefulstorage.hdfs;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.PositionedReadable;
import org.apache.hadoop.fs.Seekable;
import org.apache.heron.proto.ckptmgr.CheckpointManager;
import org.apache.heron.proto.system.PhysicalPlans;
import org.apache.heron.spi.statefulstorage.Checkpoint;
import org.apache.heron.spi.statefulstorage.CheckpointInfo;
import org.apache.heron.statefulstorage.StatefulStorageTestContext;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.Silent.class)
public class HDFSStorageTest {

  private PhysicalPlans.Instance instance;
  private CheckpointManager.InstanceStateCheckpoint instanceCheckpointState;

  private HDFSStorage hdfsStorage;
  private FileSystem mockFileSystem;

  private static class FakeFSDataInputStream extends FSDataInputStream {
    FakeFSDataInputStream(byte[] data) throws IOException {
      super(new FakeInputStream(data));
    }
  }

  private static class FakeInputStream extends InputStream implements Seekable, PositionedReadable {
    private final ByteArrayInputStream bis;
    FakeInputStream(byte[] data) { this.bis = new ByteArrayInputStream(data); }
    @Override public int read() throws IOException { return bis.read(); }
    @Override public int read(byte[] b, int off, int len) throws IOException { return bis.read(b, off, len); }
    @Override public void seek(long pos) throws IOException { }
    @Override public long getPos() throws IOException { return 0; }
    @Override public boolean seekToNewSource(long targetPos) throws IOException { return false; }
    @Override public int read(long position, byte[] buffer, int offset, int length) throws IOException { return 0; }
    @Override public void readFully(long position, byte[] buffer, int offset, int length) throws IOException { }
    @Override public void readFully(long position, byte[] buffer) throws IOException { }
  }

  private static class FakeFSDataOutputStream extends FSDataOutputStream {
    private final ByteArrayOutputStream bos;
    FakeFSDataOutputStream(ByteArrayOutputStream bos) throws IOException {
      super(bos, null);
      this.bos = bos;
    }
    byte[] toByteArray() { return bos.toByteArray(); }
  }

  @Before
  public void before() throws Exception {
    Map<String, Object> config = new HashMap<>();
    config.put(StatefulStorageTestContext.ROOT_PATH_KEY, StatefulStorageTestContext.ROOT_PATH);

    mockFileSystem = mock(FileSystem.class);
    when(mockFileSystem.getUri()).thenReturn(new URI("hdfs://hdfs_home_path/ckpgmgr"));
    when(mockFileSystem.getHomeDirectory()).thenReturn(new Path("hdfs_home_path"));

    hdfsStorage = spy(new HDFSStorage() {
        @Override
        FileSystem getFileSystem(Configuration configuration) throws IOException {
            return mockFileSystem;
        }
    });
    hdfsStorage.init(StatefulStorageTestContext.TOPOLOGY_NAME, config);

    instance = StatefulStorageTestContext.getInstance();
    instanceCheckpointState = StatefulStorageTestContext.getInstanceStateCheckpoint();
  }

  @After
  public void after() throws Exception {
    hdfsStorage.close();
  }

  @Test
  public void testStore() throws Exception {
    CheckpointManager.InstanceStateCheckpoint checkpointState =
        CheckpointManager.InstanceStateCheckpoint.newBuilder()
            .setCheckpointId("test-checkpoint")
            .build();

    Checkpoint checkpoint = new Checkpoint(checkpointState);

    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    FakeFSDataOutputStream fakeOutputStream = new FakeFSDataOutputStream(bos);
    when(mockFileSystem.create(any(Path.class))).thenReturn(fakeOutputStream);

    doNothing().when(hdfsStorage).createDir(anyString());

    final CheckpointInfo info = new CheckpointInfo(
        StatefulStorageTestContext.CHECKPOINT_ID, instance);
    hdfsStorage.storeCheckpoint(info, checkpoint);

    assertArrayEquals(checkpointState.toByteArray(), bos.toByteArray());
  }

  @Test
  public void testRestore() throws Exception {
    FakeFSDataInputStream fakeInputStream = new FakeFSDataInputStream(instanceCheckpointState.toByteArray());
    when(mockFileSystem.open(any(Path.class))).thenReturn(fakeInputStream);

    final CheckpointInfo info = new CheckpointInfo(
        StatefulStorageTestContext.CHECKPOINT_ID, instance);
    Checkpoint restoreCheckpoint = hdfsStorage.restoreCheckpoint(info);
    assertEquals(restoreCheckpoint.getCheckpoint(), instanceCheckpointState);
  }

  @Test
  public void testDisposeAll() throws Exception {
    hdfsStorage.dispose(StatefulStorageTestContext.CHECKPOINT_ID, true);
    verify(mockFileSystem).delete(any(Path.class), eq(true));
  }

  @Test
  public void testDisposePartial() throws Exception {
    Path mockPath = mock(Path.class);
    when(mockPath.getName()).thenReturn("0");

    FileStatus mockFS1 = mock(FileStatus.class);
    when(mockFS1.getPath()).thenReturn(mockPath);

    FileStatus mockFS2 = mock(FileStatus.class);
    when(mockFS2.getPath()).thenReturn(mockPath);

    FileStatus[] mockFileStatus = {mockFS1, mockFS2};
    FileStatus[] emptyFileStatus = new FileStatus[0];
    when(mockFileSystem.listStatus(any(Path.class)))
        .thenReturn(mockFileStatus)
        .thenReturn(emptyFileStatus);

    hdfsStorage.dispose(StatefulStorageTestContext.CHECKPOINT_ID, false);

    verify(mockFileSystem, times(2)).delete(any(Path.class), eq(true));
  }
}