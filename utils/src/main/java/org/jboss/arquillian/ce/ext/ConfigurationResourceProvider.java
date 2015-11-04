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

package org.jboss.arquillian.ce.ext;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.lang.annotation.Annotation;
import java.util.Properties;

import org.jboss.arquillian.ce.api.ConfigurationHandle;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.arquillian.test.spi.enricher.resource.ResourceProvider;

/**
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public class ConfigurationResourceProvider implements ResourceProvider {
    public static final String FILE_NAME = "ce-arq-configuration.properties";

    public static String toProperties(ConfigurationHandle configuration) {
        try {
            Properties properties = new Properties();
            properties.put("docker.url", configuration.getDockerUrl());
            properties.put("kubernetes.master", configuration.getKubernetesMaster());
            properties.put("kubernetes.api.version", configuration.getApiVersion());
            properties.put("kubernetes.namespace", configuration.getNamespace());
            StringWriter writer = new StringWriter();
            properties.store(writer, "CE Arquillian Configuration");
            return writer.toString();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public boolean canProvide(Class<?> type) {
        return ConfigurationHandle.class.isAssignableFrom(type);
    }

    @Override
    public Object lookup(ArquillianResource resource, Annotation... qualifiers) {
        final Properties properties = new Properties();
        try {
            try (InputStream stream = ConfigurationResourceProvider.class.getClassLoader().getResourceAsStream(FILE_NAME)) {
                properties.load(stream);
            }
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }

        return new ConfigurationHandle() {
            public String getDockerUrl() {
                return properties.getProperty("docker.url");
            }

            public String getKubernetesMaster() {
                return properties.getProperty("kubernetes.master");
            }

            public String getApiVersion() {
                return properties.getProperty("kubernetes.api.version");
            }

            public String getNamespace() {
                return properties.getProperty("kubernetes.namespace");
            }
        };
    }
}
