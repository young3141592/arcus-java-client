/*
 * arcus-java-client : Arcus Java client
 * Copyright 2010-2014 NAVER Corp.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/* ENABLE_REPLICATION if */
package net.spy.memcached;

import net.spy.memcached.compat.log.Logger;
import net.spy.memcached.compat.log.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ArcusReplNodeAddress extends InetSocketAddress {

  private static final long serialVersionUID = -1555690881482453720L;
  private static final Logger arcusLogger = LoggerFactory.getLogger(ArcusReplNodeAddress.class);
  private boolean master;
  private final String group;
  private final String ip;
  private final int port;

  private ArcusReplNodeAddress(String group, boolean master, String ip, int port) {
    super(ip, port);
    this.group = group;
    this.master = master;
    this.ip = ip;
    this.port = port;
  }

  public ArcusReplNodeAddress(ArcusReplNodeAddress addr) {
    this(addr.group, addr.master, addr.ip, addr.port);
  }

  public String toString() {
    return "{" + group + " " + (master ? "M" : "S") + " " + ip + ":" + port + "}";
  }

  public String getIPPort() {
    return this.ip + ":" + this.port;
  }

  public String getGroupName() {
    return group;
  }

  static ArcusReplNodeAddress create(String group, boolean master, String ipport) {
    String[] temp = ipport.split(":");
    String ip = temp[0];
    int port = Integer.parseInt(temp[1]);
    return new ArcusReplNodeAddress(group, master, ip, port);
  }

  private static List<InetSocketAddress> parseNodeNames(String s) throws Exception {
    List<InetSocketAddress> addrs = new ArrayList<InetSocketAddress>();

    for (String node : s.split(",")) {
      String[] temp = node.split("\\^");
      String group = temp[0];
      boolean master = temp[1].equals("M") ? true : false;
      String ipport = temp[2];
      // We may throw null pointer exception if the string has
      // an unexpected format.  Abort the whole method instead of
      // trying to ignore malformed strings.
      // Is this the right behavior?  FIXME

      addrs.add(ArcusReplNodeAddress.create(group, master, ipport));
    }
    return addrs;
  }

  // Similar to AddrUtil.getAddresses.  This version parses replicaton znode names.
  // Znode names are group^{M,S}^ip:port-hostname
  static List<InetSocketAddress> getAddresses(String s) {
    List<InetSocketAddress> list = null;

    if (s != null && !s.isEmpty()) {
      try {
        list = parseNodeNames(s);
      } catch (Exception e) {
        // May see an exception if nodes do not follow the replication naming convention
        arcusLogger.error("Exception caught while parsing node" +
                " addresses. cache_list=" + s + "\n" + e);
        e.printStackTrace();
      }
    }

    if (list == null) {
      list = new ArrayList<InetSocketAddress>(0);
    }
    return list;
  }

  static Map<String, List<ArcusReplNodeAddress>> makeGroupAddrsList(
          List<InetSocketAddress> addrs) {

    Map<String, List<ArcusReplNodeAddress>> newAllGroups =
            new HashMap<String, List<ArcusReplNodeAddress>>();

    for (int i = 0; i < addrs.size(); i++) {
      ArcusReplNodeAddress a = (ArcusReplNodeAddress) addrs.get(i);
      String groupName = a.getGroupName();
      List<ArcusReplNodeAddress> gNodeList = newAllGroups.get(groupName);
      if (gNodeList == null) {
        gNodeList = new ArrayList<ArcusReplNodeAddress>();
        newAllGroups.put(groupName, gNodeList);
      }
      // Add the master node as the first element of node list.
      if (a.master) { // shifts the element currently at that position
        gNodeList.add(0, a);
      } else { // Don't care the index, just add it.
        gNodeList.add(a);
      }
    }

    for (Map.Entry<String, List<ArcusReplNodeAddress>> entry : newAllGroups.entrySet()) {
      // If newGroupNodes is valid, it is sorted by master and slave order.
      List<ArcusReplNodeAddress> newGroupNodes = entry.getValue();

      if (newGroupNodes.size() >= 3) {
        arcusLogger.error("Invalid group " + entry.getKey() + " : "
                + " Too many nodes. " + newGroupNodes);
        entry.setValue(new ArrayList<ArcusReplNodeAddress>());
      } else if (newGroupNodes.size() == 2 &&
              newGroupNodes.get(0).getIPPort().equals(newGroupNodes.get(1).getIPPort())) {
        // Two nodes have the same ip and port.
        arcusLogger.error("Invalid group " + entry.getKey() + " : "
                + "Two nodes have the same ip and port. " + newGroupNodes);
        entry.setValue(new ArrayList<ArcusReplNodeAddress>());
      } else if (newGroupNodes.size() == 2 &&
              newGroupNodes.get(0).master && newGroupNodes.get(1).master) {
        // Two nodes are masters.
        arcusLogger.error("Invalid group " + entry.getKey() + " : "
                + "Two master nodes exist. " + newGroupNodes);
        entry.setValue(new ArrayList<ArcusReplNodeAddress>());
      } else if (!newGroupNodes.get(0).master) {
        /* This case can occur during the switchover or failover.
         * 1) In the switchover, it occurs after below the first phase. 
         * - the old master is changed to the new slave node.
         * - the old slave is changed to the new master node.
         * 2) In the failover, it occurs after below the first phase.
         * - the old master is removed by abnormal shutdown.
         * - the old slave is changed to the new master node.
         */
        arcusLogger.info("Invalid group " + entry.getKey() + " : "
                + "Master does not exist. " + newGroupNodes);
        entry.setValue(new ArrayList<ArcusReplNodeAddress>());
      }
    }
    return newAllGroups;
  }

  public boolean isMaster() {
    return master;
  }

  public void setMaster(boolean master) {
    this.master = master;
  }
}
/* ENABLE_REPLICATION end */
