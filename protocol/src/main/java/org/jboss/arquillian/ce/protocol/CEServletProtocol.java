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

package org.jboss.arquillian.ce.protocol;

import org.jboss.arquillian.ce.utils.Constants;
import org.jboss.arquillian.container.spi.client.protocol.ProtocolDescription;
import org.jboss.arquillian.container.spi.client.protocol.metadata.ProtocolMetaData;
import org.jboss.arquillian.container.test.spi.ContainerMethodExecutor;
import org.jboss.arquillian.container.test.spi.client.deployment.DeploymentPackager;
import org.jboss.arquillian.container.test.spi.client.protocol.Protocol;
import org.jboss.arquillian.container.test.spi.command.CommandCallback;

/**
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public class CEServletProtocol implements Protocol<CEProtocolConfiguration> {
    public Class<CEProtocolConfiguration> getProtocolConfigurationClass() {
        return CEProtocolConfiguration.class;
    }

    public ProtocolDescription getDescription() {
        return new ProtocolDescription(Constants.PROTOCOL_NAME);
    }

    public DeploymentPackager getPackager() {
        return new CEProtocolDeploymentPackager();
    }

    public ContainerMethodExecutor getExecutor(CEProtocolConfiguration servletProtocolConfiguration, ProtocolMetaData metaData, CommandCallback commandCallback) {
        return new CEServletExecutor(servletProtocolConfiguration, metaData, commandCallback);
    }
}
