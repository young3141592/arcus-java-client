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
/* ENABLE_REPLICATION start */
package net.spy.memcached.util;

import net.spy.memcached.MemcachedNode;
import net.spy.memcached.Arcus17NodeAddress;

public class Arcus17KetamaNodeLocatorConfiguration extends
		ArcusKetamaNodeLocatorConfiguration {

	public String getKeyForNode(MemcachedNode node, int repetition) {
		Arcus17NodeAddress addr = (Arcus17NodeAddress)node.getSocketAddress();
		String key = addr.getGroupName() + "-" + repetition;
		return key;
	}
}
/* ENABLE_REPLICATION end */