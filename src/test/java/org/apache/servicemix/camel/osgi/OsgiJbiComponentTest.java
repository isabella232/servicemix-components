/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.servicemix.camel.osgi;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

import junit.framework.TestCase;

import org.apache.servicemix.camel.CamelComponent;
import org.apache.servicemix.camel.CamelJbiComponent;
import org.easymock.EasyMock;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

/**
 * Test cases for {@link OsgiJbiComponent}
 */
public class OsgiJbiComponentTest extends TestCase {
    
    /*
     * The OsgiJbiComponent should get the CamelComponent from the OSGi Service Registry
     */
    public void testGetCamelComponentFromOsgiServiceRegistry() throws Exception {
        CamelComponent component = new CamelJbiComponent();
        
        BundleContext context = EasyMock.createMock(BundleContext.class);
        ServiceReference reference = EasyMock.createMock(ServiceReference.class);
        expect(context.getServiceReference(CamelComponent.class.getName())).andReturn(reference);
        expect(context.getService(reference)).andReturn(component);
        expect(context.ungetService(reference)).andReturn(true);
        replay(context);

        // check that the CamelComponent is being fetched from the OSGi Service Registry
        OsgiJbiComponent osgiComponent = new OsgiJbiComponent();
        osgiComponent.setBundleContext(context);
        osgiComponent.afterPropertiesSet();
        assertSame(component, osgiComponent.getCamelJbiComponent());
        
        // and that that is being unget in the destroy method
        osgiComponent.destroy();        
        verify(context);
    }
    
    public void testIllegalStateExceptionWithoutServiceRegistryEntry() throws Exception {
        BundleContext context = EasyMock.createMock(BundleContext.class);
        expect(context.getServiceReference(CamelComponent.class.getName())).andReturn(null);
        replay(context);
        
        OsgiJbiComponent osgiComponent = new OsgiJbiComponent();
        osgiComponent.setBundleContext(context);

        try {
            osgiComponent.afterPropertiesSet();
            fail("Should have thrown an IllegalStateException");
        } catch (IllegalStateException e) {
            // this is exactly what we expected
        }
    }
}
