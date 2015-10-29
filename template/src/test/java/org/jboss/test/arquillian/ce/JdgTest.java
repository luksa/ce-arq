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

import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;
import org.jboss.arquillian.ce.api.ExternalDeployment;
import org.jboss.arquillian.ce.api.RunInPod;
import org.jboss.arquillian.ce.api.RunInPodDeployment;
import org.jboss.arquillian.ce.api.Template;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.jboss.test.arquillian.ce.deployment.MemcachedCache;
import org.jboss.test.arquillian.ce.deployment.RESTCache;
import org.junit.Test;
import org.junit.runner.RunWith;

import static junit.framework.Assert.assertEquals;

/**
 * @author Marko Luksa
 */
@RunWith(Arquillian.class)
@RunInPod
@ExternalDeployment
@Template(url = "https://raw.githubusercontent.com/luksa/application-templates/jdg-templates/datagrid/datagrid65-https-s2i.json",
        labels = "application=jdg-app")
public class JdgTest {
    private static final boolean USE_SASL = true;

    public static final String NAMESPACE = "mluksa";
    public static final String JDG_HOST = "jdg-app-" + NAMESPACE + ".router.default.svc.cluster.local";
    public static final int JDG_PORT = 80;
    public static final String CONTEXT_PATH = "/rest";

    @Deployment
    @RunInPodDeployment
    public static WebArchive getDeployment() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "run-in-pod.war");
        war.setWebXML(new StringAsset("<web-app/>"));
        war.addPackage(RESTCache.class.getPackage());

        war.addAsLibraries(
                Maven.resolver()
                        .resolve("com.google.code.simple-spring-memcached:spymemcached:2.8.1")
                        .withTransitivity()
                        .asFile());
        war.addAsLibraries(
                Maven.resolver()
                        .resolve("org.infinispan:infinispan-client-hotrod:6.3.1.Final-redhat-1")
                        .withTransitivity()
                        .asFile());
        return war;
    }


    @Test
    public void testRestService() throws Exception {
        String host = System.getenv("JDG_APP_SERVICE_HOST");
        int port = Integer.parseInt(System.getenv("JDG_APP_SERVICE_PORT"));
        RESTCache<String, Object> cache = new RESTCache<String, Object>("default", "http://" + host + ":" + port + CONTEXT_PATH + "/");
        cache.put("foo1", "bar1");
        assertEquals("bar1", cache.get("foo1"));
    }

    @Test
    @RunAsClient
    public void testRestRoute() throws Exception {
        RESTCache<String, Object> cache = new RESTCache<String, Object>("default", "http://" + JDG_HOST + ":" + JDG_PORT + CONTEXT_PATH + "/");
        cache.put("foo1", "bar1");
        assertEquals("bar1", cache.get("foo1"));
    }

    @Test
    public void testMemcachedService() throws Exception {
        String host = System.getenv("JDG_APP_MEMCACHED_SERVICE_HOST");
        int port = Integer.parseInt(System.getenv("JDG_APP_MEMCACHED_SERVICE_PORT"));
        MemcachedCache<String, Object> cache = new MemcachedCache<>(host, port);
        cache.put("foo2", "bar2");
        assertEquals("bar2", cache.get("foo2"));
    }

    @Test
    @RunAsClient
    public void testMemcachedRoute() throws Exception {
        MemcachedCache<String, Object> cache = new MemcachedCache<>("jdg-app-memcached-mluksa.router.default.svc.cluster.local", 443, USE_SASL);
        cache.put("foo2", "bar2");
        assertEquals("bar2", cache.get("foo2"));
    }

    @Test
    public void testHotRodService() throws Exception {
        String host = System.getenv("JDG_APP_HOTROD_SERVICE_HOST");
        int port = Integer.parseInt(System.getenv("JDG_APP_HOTROD_SERVICE_PORT"));

        RemoteCacheManager cacheManager = new RemoteCacheManager(
                new ConfigurationBuilder()
                        .addServer()
                        .host(host).port(port)
                        .build()
        );
        RemoteCache<Object, Object> cache = cacheManager.getCache("default");

        cache.put("foo3", "bar3");
        assertEquals("bar3", cache.get("foo3"));
    }

}
