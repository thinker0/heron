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

package org.apache.heron.statemgr.zookeeper;

import java.net.InetSocketAddress;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import org.apache.heron.common.basics.Pair;
import org.apache.heron.spi.common.Config;
import org.apache.heron.spi.common.Key;
import org.apache.heron.spi.utils.NetworkUtils;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"jdk.internal.reflect.*", "javax.net.ssl.*", "javax.xml.*", "javax.management.*", "org.w3c.dom.*", "org.xml.sax.*", "javax.xml.parsers.*", "javax.xml.datatype.*"})
@PrepareForTest(NetworkUtils.class)
public class ZkUtilsTest {

  @Test
  public void testSetupZkTunnel() throws Exception {
    String host0 = "host0";
    int port0 = 12;
    InetSocketAddress address0 = InetSocketAddress.createUnresolved(host0, port0);
    String host1 = "host1";
    int port1 = 13;
    InetSocketAddress address1 = InetSocketAddress.createUnresolved(host1, port1);
    String host2 = "host2";
    int port2 = 9049;
    InetSocketAddress address2 = InetSocketAddress.createUnresolved(host2, port2);

    String tunnelHost = "tunnelHost";
    int tunnelPort = 9519;
    InetSocketAddress tunnelAddress = InetSocketAddress.createUnresolved(tunnelHost, tunnelPort);

    String connectionString = host0 + ":" + port0 + "," + host1 + ":" + port1 + "," + host2 + ":" + port2;

    Config config = mock(Config.class);
    when(config.getStringValue(Key.STATEMGR_CONNECTION_STRING)).thenReturn(connectionString);
    NetworkUtils.TunnelConfig tunnelConfig = NetworkUtils.TunnelConfig.build(config, NetworkUtils.HeronSystem.STATE_MANAGER);

    Process process = mock(Process.class);

    PowerMockito.spy(NetworkUtils.class);
    
    PowerMockito.doReturn(new Pair<>(address0, process))
        .when(NetworkUtils.class, "establishSSHTunnelIfNeeded",
            eq(address0), any(NetworkUtils.TunnelConfig.class), any(NetworkUtils.TunnelType.class));
    PowerMockito.doReturn(new Pair<>(address1, process))
        .when(NetworkUtils.class, "establishSSHTunnelIfNeeded",
            eq(address1), any(NetworkUtils.TunnelConfig.class), any(NetworkUtils.TunnelType.class));
    PowerMockito.doReturn(new Pair<>(tunnelAddress, process))
        .when(NetworkUtils.class, "establishSSHTunnelIfNeeded",
            eq(address2), any(NetworkUtils.TunnelConfig.class), any(NetworkUtils.TunnelType.class));

    Pair<String, List<Process>> ret = ZkUtils.setupZkTunnel(config, tunnelConfig);

    String expectedConnectionString = address0.toString() + "," + address1.toString() + "," + tunnelAddress.toString();
    assertEquals(expectedConnectionString, ret.first);
    assertEquals(3, ret.second.size());
  }
}