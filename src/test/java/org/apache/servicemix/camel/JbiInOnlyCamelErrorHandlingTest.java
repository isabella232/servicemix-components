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
package org.apache.servicemix.camel;

import javax.jbi.messaging.ExchangeStatus;
import javax.jbi.messaging.InOnly;
import javax.xml.namespace.QName;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.converter.jaxp.StringSource;
import org.apache.servicemix.client.DefaultServiceMixClient;
import org.apache.servicemix.client.ServiceMixClient;
import org.apache.servicemix.jbi.FaultException;

/**
 * Tests on handling fault messages with the Camel Exception handler  
 */
public class JbiInOnlyCamelErrorHandlingTest extends JbiCamelErrorHandlingTestSupport {
    
    private static final String MESSAGE = "<just><a>test</a></just>";

    public void testInOnlyWithNoHandleFault() throws Exception {
        MockEndpoint errors = getMockEndpoint("mock:errors");
        errors.expectedMessageCount(1);
        
        ServiceMixClient client = new DefaultServiceMixClient(jbiContainer);
        InOnly exchange = client.createInOnlyExchange();
        exchange.setService(new QName("urn:test", "no-handle-fault"));
        exchange.getInMessage().setContent(new StringSource(MESSAGE));
        client.sendSync(exchange);
        assertEquals(ExchangeStatus.ERROR, exchange.getStatus());
        assertTrue("A FaultException was expected", exchange.getError() instanceof FaultException);
        
        errors.assertIsSatisfied();
    }

    public void testInOnlyWithHandleFault() throws Exception {
        MockEndpoint errors = getMockEndpoint("mock:errors");
        errors.expectedMessageCount(1);
        
        ServiceMixClient client = new DefaultServiceMixClient(jbiContainer);
        InOnly exchange = client.createInOnlyExchange();
        exchange.setService(new QName("urn:test", "handle-fault"));
        exchange.getInMessage().setContent(new StringSource(MESSAGE));
        client.sendSync(exchange);
        assertEquals(ExchangeStatus.ERROR, exchange.getStatus());
        assertTrue("A FaultException was expected", exchange.getError() instanceof FaultException);

        errors.assertIsSatisfied();
    }

    public void testInOnlyWithErrorNotHandled() throws Exception {
        MockEndpoint errors = getMockEndpoint("mock:errors");
        errors.expectedMessageCount(1);
        
        ServiceMixClient client = new DefaultServiceMixClient(jbiContainer);
        InOnly exchange = client.createInOnlyExchange();
        exchange.setService(new QName("urn:test", "error-not-handled"));
        exchange.getInMessage().setContent(new StringSource(MESSAGE));
        client.sendSync(exchange);
        assertEquals(ExchangeStatus.ERROR, exchange.getStatus());
        assertTrue("A IllegalArgumentException was expected", exchange.getError() instanceof IllegalArgumentException);

        errors.assertIsSatisfied();
    }

    public void testInOnlyWithErrorHandledFalse() throws Exception {
        MockEndpoint errors = getMockEndpoint("mock:errors");
        errors.expectedMessageCount(0);
        
        ServiceMixClient client = new DefaultServiceMixClient(jbiContainer);
        InOnly exchange = client.createInOnlyExchange();
        exchange.setService(new QName("urn:test", "error-handled-false"));
        exchange.getInMessage().setContent(new StringSource(MESSAGE));
        client.sendSync(exchange);
        assertEquals(ExchangeStatus.ERROR, exchange.getStatus());
        assertTrue("A IllegalStateException was expected", exchange.getError() instanceof IllegalStateException);

        receiverComponent.getMessageList().assertMessagesReceived(1);
        
        errors.assertIsSatisfied();
    }
    
    public void testInOnlyWithErrorHandledTrue() throws Exception {
        MockEndpoint errors = getMockEndpoint("mock:errors");
        errors.expectedMessageCount(0);
        
        ServiceMixClient client = new DefaultServiceMixClient(jbiContainer);
        InOnly exchange = client.createInOnlyExchange();
        exchange.setService(new QName("urn:test", "error-handled-true"));
        exchange.getInMessage().setContent(new StringSource(MESSAGE));
        client.sendSync(exchange);
        assertEquals(ExchangeStatus.DONE, exchange.getStatus());

        receiverComponent.getMessageList().assertMessagesReceived(1);
        
        errors.assertIsSatisfied();
    }

    @Override
    protected RouteBuilder createRoutes() {
        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {
            	onException(IllegalStateException.class).handled(false).to("jbi:service:urn:test:receiver-service?mep=in-only");
            	onException(NullPointerException.class).handled(true).to("jbi:service:urn:test:receiver-service?mep=in-only");
                errorHandler(deadLetterChannel("mock:errors").maximumRedeliveries(1).initialRedeliveryDelay(300));
                from("jbi:service:urn:test:no-handle-fault").to("jbi:service:urn:test:faulty-service?mep=in-only");
                from("jbi:service:urn:test:handle-fault").handleFault().to("jbi:service:urn:test:faulty-service?mep=in-only");
                from("jbi:service:urn:test:error-not-handled").to("jbi:service:urn:test:iae-error-service?mep=in-only");
                from("jbi:service:urn:test:error-handled-false").to("jbi:service:urn:test:ise-error-service?mep=in-only");
                from("jbi:service:urn:test:error-handled-true").to("jbi:service:urn:test:npe-error-service?mep=in-only");
            }
        };
    }
}
