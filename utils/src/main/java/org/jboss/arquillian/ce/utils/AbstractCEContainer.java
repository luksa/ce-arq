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

package org.jboss.arquillian.ce.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerManifest;
import io.fabric8.kubernetes.api.model.ContainerPort;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.HTTPGetAction;
import io.fabric8.kubernetes.api.model.Handler;
import io.fabric8.kubernetes.api.model.Lifecycle;
import io.fabric8.kubernetes.api.model.PodState;
import io.fabric8.kubernetes.api.model.PodTemplate;
import io.fabric8.kubernetes.api.model.ReplicationController;
import io.fabric8.kubernetes.api.model.ReplicationControllerState;
import io.fabric8.kubernetes.api.model.VolumeMount;
import io.fabric8.kubernetes.api.model.util.IntOrString;
import org.jboss.arquillian.container.spi.client.container.DeployableContainer;
import org.jboss.arquillian.container.spi.client.container.DeploymentException;
import org.jboss.arquillian.container.spi.client.container.LifecycleException;
import org.jboss.arquillian.container.spi.client.protocol.ProtocolDescription;
import org.jboss.arquillian.container.spi.client.protocol.metadata.HTTPContext;
import org.jboss.arquillian.container.spi.client.protocol.metadata.ProtocolMetaData;
import org.jboss.arquillian.container.spi.client.protocol.metadata.Servlet;
import org.jboss.arquillian.protocol.servlet.ServletMethodExecutor;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.Node;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.descriptor.api.Descriptor;
import org.jboss.shrinkwrap.descriptor.api.Descriptors;
import org.jboss.shrinkwrap.descriptor.api.application7.ApplicationDescriptor;
import org.jboss.shrinkwrap.descriptor.api.application7.ModuleType;
import org.jboss.shrinkwrap.descriptor.api.application7.WebType;

/**
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public abstract class AbstractCEContainer<T extends Configuration> implements DeployableContainer<T> {
    protected final Logger log = Logger.getLogger(getClass().getName());

    protected T configuration;
    protected K8sClient client;

    protected abstract void cleanup() throws Exception;

    public void setup(T configuration) {
        this.configuration = getConfigurationClass().cast(configuration);
        this.client = new K8sClient(configuration);
    }

    public void start() throws LifecycleException {
    }

    public void stop() throws LifecycleException {
    }

    public ProtocolDescription getDefaultProtocol() {
        return new ProtocolDescription("Servlet 3.0");
    }

    protected String buildImage(Archive<?> archive, String parent, String dir) throws IOException {
        Properties properties = new Properties();
        String from = System.getProperty("from.name", parent);
        properties.put("from.name", from);
        String deployment = System.getProperty("deployment.dir", dir);
        properties.put("deployment.dir", deployment);
        configuration.apply(properties);

        log.info(String.format("FROM %s [%s]", from, deployment));

        InputStream dockerfileTemplate = getClass().getClassLoader().getResourceAsStream("Dockerfile_template");
        return client.pushImage(dockerfileTemplate, archive, properties);
    }

    protected void deployPod(String imageName, List<ContainerPort> ports, String name, int replicas, String preStopPath) throws Exception {
        String apiVersion = configuration.getApiVersion();

        List<EnvVar> envVars = Collections.emptyList();
        Lifecycle lifecycle = null;
        if (preStopPath != null) {
            lifecycle = new Lifecycle();
            Handler preStopHandler = new Handler();
            HTTPGetAction httpGet = new HTTPGetAction();
            httpGet.setPath(preStopPath);
            httpGet.setPort(findHttpPort(ports));
            preStopHandler.setHttpGet(httpGet);
            lifecycle.setPreStop(preStopHandler);
        }
        List<VolumeMount> volumes = Collections.emptyList();
        Container container = client.createContainer(imageName, name + "-container", envVars, ports, volumes, lifecycle);

        List<Container> containers = Collections.singletonList(container);
        ContainerManifest cm = client.createContainerManifest(name + "Pod", apiVersion, containers);

        PodState podState = client.createPodState(cm);
        Map<String, String> podLabels = Collections.singletonMap("name", name + "Pod");

        PodTemplate podTemplate = client.createPodTemplate(podLabels, podState);

        Map<String, String> selector = Collections.singletonMap("name", name + "Pod");
        ReplicationControllerState desiredState = client.createReplicationControllerState(replicas, selector, podTemplate);

        Map<String, String> labels = Collections.singletonMap("name", name + "Controller");
        ReplicationController rc = client.createReplicationController(name + "rc", ReplicationController.ApiVersion.fromValue(apiVersion), labels, desiredState);

        client.deployReplicationController(rc);
    }

    private static IntOrString toIntOrString(ContainerPort port) {
        IntOrString intOrString = new IntOrString();
        intOrString.setIntVal(port.getContainerPort());
        return intOrString;
    }

    private static IntOrString findHttpPort(List<ContainerPort> ports) {
        return findPort(ports, "http");
    }

    static IntOrString findPort(List<ContainerPort> ports, String name) {
        if (ports.isEmpty()) {
            throw new IllegalArgumentException("Empty ports!");
        }
        if (ports.size() == 1) {
            return toIntOrString(ports.get(0));
        }
        for (ContainerPort port : ports) {
            if (name.equals(port.getName())) {
                return toIntOrString(port);
            }
        }
        throw new IllegalArgumentException("No such port: " + name);
    }

    protected ProtocolMetaData getProtocolMetaData(Archive<?> archive) throws Exception {
        String host = client.getService("http-service").getPortalIP();

        HTTPContext context = new HTTPContext(host, 80);
        addServlets(context, archive);

        log.info(String.format("HTTP host: %s", host));

        Containers.delayArchiveDeploy(String.format("http://%s:%s", host, 80), configuration.getStartupTimeout(), 4000L);

        ProtocolMetaData pmd = new ProtocolMetaData();
        pmd.addContext(context);
        return pmd;
    }

    protected void addServlets(HTTPContext context, Archive<?> archive) throws Exception {
        if (archive instanceof WebArchive) {
            handleWebArchive(context, WebArchive.class.cast(archive));
        } else if (archive instanceof EnterpriseArchive) {
            handleEAR(context, EnterpriseArchive.class.cast(archive));
        }
    }

    private void handleWebArchive(HTTPContext context, WebArchive war) {
        String name = war.getName();
        String contextRoot = "";
        // ROOT --> "/"
        if ("ROOT.war".equals(name) == false) {
            int p = name.lastIndexOf("."); // drop .war
            contextRoot = name.substring(0, p);
        }
        handleWebArchive(context, war, contextRoot);
    }

    private void handleEAR(HTTPContext context, EnterpriseArchive ear) throws IOException {
        final Node appXml = ear.get("META-INF/application.xml");
        if (appXml != null) {
            try (InputStream stream = appXml.getAsset().openStream()) {
                ApplicationDescriptor ad = Descriptors.importAs(ApplicationDescriptor.class).fromStream(stream);
                List<ModuleType<ApplicationDescriptor>> allModules = ad.getAllModule();
                for (ModuleType<ApplicationDescriptor> mt : allModules) {
                    WebType<ModuleType<ApplicationDescriptor>> web = mt.getOrCreateWeb();
                    String uri = web.getWebUri();
                    if (uri != null) {
                        WebArchive war = ear.getAsType(WebArchive.class, uri);
                        handleWebArchive(context, war, web.getContextRoot());
                    }
                }
            }
        }
    }

    @SuppressWarnings("UnusedParameters")
    private void handleWebArchive(HTTPContext context, WebArchive war, String contextRoot) {
        Servlet arqServlet = new Servlet(ServletMethodExecutor.ARQUILLIAN_SERVLET_NAME, contextRoot);
        context.add(arqServlet);
    }

    public void undeploy(Archive<?> archive) throws DeploymentException {
        // do we keep test config around for some more?
        if (configuration.isIgnoreCleanup() == false) {
            try {
                cleanup();
            } catch (Exception ignored) {
            }
        } else {
            log.info(String.format("Ignore Kubernetes cleanup -- test config is still available."));
        }

        try {
            client.close();
        } catch (IOException ignored) {
        }
    }

    public void deploy(Descriptor descriptor) throws DeploymentException {
    }

    public void undeploy(Descriptor descriptor) throws DeploymentException {
    }

}
