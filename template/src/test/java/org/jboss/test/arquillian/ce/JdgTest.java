/*
 * JBoss, Home of Professional Open Source
 * Copyright 2015 Red Hat Inc. and/or its affiliates and other
 * contributors as indicated by the @author tags. All rights reserved.
 * See the copyright.txt in the distribution for a full listing of
 * individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.test.arquillian.ce;

import org.jboss.arquillian.ce.api.ExternalDeployment;
import org.jboss.arquillian.ce.api.RunInPod;
import org.jboss.arquillian.ce.api.Template;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.junit.Test;
import org.junit.runner.RunWith;

import static junit.framework.Assert.assertEquals;

/**
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
@RunWith(Arquillian.class)
@RunInPod
@ExternalDeployment
@Template(url = "https://raw.githubusercontent.com/luksa/application-templates/jdg-templates/datagrid/datagrid65-https-s2i.json")
public class JdgTest {
    private static final boolean USE_SASL = true;

    public static final String NAMESPACE = "mluksa";
    public static final String JDG_HOST = "jdg-app-" + NAMESPACE + ".router.default.svc.cluster.local";
    public static final int JDG_PORT = 80;
    public static final String CONTEXT_PATH = "/rest";

    @Test
    @RunAsClient
    public void testRestThroughService() throws Exception {
        String host = System.getenv("JDG_APP_SERVICE_HOST");
        int port = Integer.parseInt(System.getenv("JDG_APP_SERVICE_PORT"));
//        String host = "jdg-app";
//        int port = 8080;
        RESTCache<String, Object> cache = new RESTCache<String, Object>("default", "http://" + host + ":" + port + CONTEXT_PATH + "/");
        cache.put("foo1", "bar1");
        assertEquals("bar1", cache.get("foo1"));
        Thread.sleep(60000);

    }

//    @Test
//    @RunAsClient
//    public void testRestThroughRoute() throws Exception {
//        // NOTE: requires SSH tunnel
//        RESTCache<String, Object> cache = new RESTCache<String, Object>("default", "http://" + JDG_HOST + ":" + JDG_PORT + CONTEXT_PATH + "/");
//        cache.put("foo1", "bar1");
//        assertEquals("bar1", cache.get("foo1"));
//    }
//
//    @Test
//    @RunAsClient
//    public void testMemcachedThroughService() throws Exception {
//        // NOTE: requires SSH tunnel
//        MemcachedCache<String, Object> cache = new MemcachedCache<>("localhost", 11211);
//        cache.put("foo2", "bar2");
//        assertEquals("bar2", cache.get("foo2"));
//    }
//
//    @Test
//    @RunAsClient
//    public void testMemcachedThroughRoute() throws Exception {
//        MemcachedCache<String, Object> cache = new MemcachedCache<>("jdg-app-memcached-mluksa.router.default.svc.cluster.local", 443, USE_SASL);
//        cache.put("foo2", "bar2");
//        assertEquals("bar2", cache.get("foo2"));
//    }
//
//    @Test
//    @RunAsClient
//    public void testHotRodThroughService() throws Exception {
//        // NOTE: requires SSH tunnel
//        ConfigurationBuilder builder = new ConfigurationBuilder();
//        builder.addServer()
//                .host("localhost")
//                .port(11222);
////                .host("jdg-app-hotrod-mluksa.router.default.svc.cluster.local")
////                .port(80);
//        RemoteCacheManager cacheManager = new RemoteCacheManager(builder.build());
//        RemoteCache<Object, Object> cache = cacheManager.getCache("default");
//
//        cache.put("foo3", "bar3");
//
//        assertEquals("bar3", cache.get("foo3"));
//    }

}
