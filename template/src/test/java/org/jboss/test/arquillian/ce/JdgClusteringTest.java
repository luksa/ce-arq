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

import java.util.logging.Logger;

import junit.framework.Assert;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;
import org.jboss.arquillian.ce.api.Template;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
@RunWith(Arquillian.class)
@Template(url = "https://raw.githubusercontent.com/luksa/application-templates/JDG/datagrid/datagrid65-basic-s2i.json")
public class JdgClusteringTest {
    private static Logger log = Logger.getLogger(JdgClusteringTest.class.getName());

    @Deployment
    public static WebArchive getDeployment() throws Exception {
        return ShrinkWrap.create(WebArchive.class, "test.war");
    }

    @Test
    public void testBasic() throws Exception {
        ConfigurationBuilder builder = new ConfigurationBuilder();
        builder.addServer()
                .host("jdg-app-mluksa.router.default.svc.cluster.local")
                .port(11222);
        RemoteCacheManager cacheManager = new RemoteCacheManager(builder.build());
        RemoteCache<Object, Object> cache = cacheManager.getCache("myCache");

        cache.put("foo", "bar");

        Assert.assertEquals("bar", cache.get("foo"));
    }

}
