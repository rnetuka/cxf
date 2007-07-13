/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.cxf.binding.soap;


import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import javax.wsdl.BindingInput;
import javax.wsdl.BindingOutput;
import javax.wsdl.Definition;
import javax.wsdl.Part;
import javax.wsdl.WSDLException;
import javax.wsdl.extensions.ExtensibilityElement;
import javax.wsdl.extensions.ExtensionRegistry;
import javax.wsdl.extensions.mime.MIMEContent;
import javax.wsdl.extensions.mime.MIMEMultipartRelated;
import javax.wsdl.extensions.mime.MIMEPart;
import javax.xml.namespace.QName;

import org.apache.cxf.binding.AbstractBindingFactory;
import org.apache.cxf.binding.Binding;
import org.apache.cxf.binding.soap.interceptor.EndpointSelectionInterceptor;
import org.apache.cxf.binding.soap.interceptor.MustUnderstandInterceptor;
import org.apache.cxf.binding.soap.interceptor.RPCInInterceptor;
import org.apache.cxf.binding.soap.interceptor.RPCOutInterceptor;
import org.apache.cxf.binding.soap.interceptor.ReadHeadersInterceptor;
import org.apache.cxf.binding.soap.interceptor.Soap11FaultInInterceptor;
import org.apache.cxf.binding.soap.interceptor.Soap11FaultOutInterceptor;
import org.apache.cxf.binding.soap.interceptor.Soap12FaultInInterceptor;
import org.apache.cxf.binding.soap.interceptor.Soap12FaultOutInterceptor;
import org.apache.cxf.binding.soap.interceptor.SoapActionInterceptor;
import org.apache.cxf.binding.soap.interceptor.SoapHeaderInterceptor;
import org.apache.cxf.binding.soap.interceptor.SoapOutInterceptor;
import org.apache.cxf.binding.soap.interceptor.SoapPreProtocolOutInterceptor;
import org.apache.cxf.binding.soap.model.SoapBindingInfo;
import org.apache.cxf.binding.soap.model.SoapBodyInfo;
import org.apache.cxf.binding.soap.model.SoapHeaderInfo;
import org.apache.cxf.binding.soap.model.SoapOperationInfo;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.interceptor.AttachmentInInterceptor;
import org.apache.cxf.interceptor.AttachmentOutInterceptor;
import org.apache.cxf.interceptor.BareOutInterceptor;
import org.apache.cxf.interceptor.DocLiteralInInterceptor;
import org.apache.cxf.interceptor.StaxInInterceptor;
import org.apache.cxf.interceptor.StaxOutInterceptor;
import org.apache.cxf.interceptor.URIMappingInterceptor;
import org.apache.cxf.interceptor.WrappedOutInterceptor;
import org.apache.cxf.message.Message;
import org.apache.cxf.service.model.BindingFaultInfo;
import org.apache.cxf.service.model.BindingInfo;
import org.apache.cxf.service.model.BindingMessageInfo;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.MessageInfo;
import org.apache.cxf.service.model.MessagePartInfo;
import org.apache.cxf.service.model.OperationInfo;
import org.apache.cxf.service.model.ServiceInfo;
import org.apache.cxf.tools.common.extensions.soap.SoapBinding;
import org.apache.cxf.tools.common.extensions.soap.SoapBody;
import org.apache.cxf.tools.common.extensions.soap.SoapFault;
import org.apache.cxf.tools.common.extensions.soap.SoapHeader;
import org.apache.cxf.tools.common.extensions.soap.SoapOperation;
import org.apache.cxf.tools.util.SOAPBindingUtil;
import org.apache.cxf.transport.ChainInitiationObserver;
import org.apache.cxf.transport.Destination;
import org.apache.cxf.transport.MessageObserver;
import org.apache.cxf.transport.MultipleEndpointObserver;
import org.apache.cxf.wsdl.WSDLConstants;
import org.apache.cxf.wsdl.WSDLManager;
import org.apache.cxf.wsdl11.WSDLServiceBuilder;
import org.apache.ws.commons.schema.XmlSchemaCollection;

import static org.apache.cxf.helpers.CastUtils.cast;

public class SoapBindingFactory extends AbstractBindingFactory {

    public static final String SOAP_11_BINDING = "http://schemas.xmlsoap.org/wsdl/soap/";
    public static final String SOAP_12_BINDING = "http://schemas.xmlsoap.org/wsdl/soap12/";

    public static final String HEADER = "messagepart.isheader";
    public static final String OUT_OF_BAND_HEADER = "messagepart.is_out_of_band_header";

    private boolean mtomEnabled = true;

    public BindingInfo createBindingInfo(ServiceInfo si, String bindingid, Object conf) {
        SoapBindingConfiguration config;
        if (conf instanceof SoapBindingConfiguration) {
            config = (SoapBindingConfiguration)conf;
        } else {
            config = new SoapBindingConfiguration();
        }
        if (WSDLConstants.SOAP12_NAMESPACE.equals(bindingid)
            || WSDLConstants.SOAP12_HTTP_TRANSPORT.equals(bindingid)) {
            config.setVersion(Soap12.getInstance());
            config.setTransportURI(WSDLConstants.SOAP12_HTTP_TRANSPORT);
        }
        SoapBindingInfo info = new SoapBindingInfo(si,
                                                   bindingid,
                                                   config.getVersion());

        info.setName(new QName(si.getName().getNamespaceURI(),
                               si.getName().getLocalPart() + "SoapBinding"));
        info.setStyle(config.getStyle());
        info.setTransportURI(config.getTransportURI());

        if (config.isMtomEnabled()) {
            info.setProperty(Message.MTOM_ENABLED, Boolean.TRUE);
        }

        for (OperationInfo op : si.getInterface().getOperations()) {
            SoapOperationInfo sop = new SoapOperationInfo();
            sop.setAction(config.getSoapAction(op));
            sop.setStyle(config.getStyle(op));

            BindingOperationInfo bop =
                info.buildOperation(op.getName(), op.getInputName(), op.getOutputName());

            bop.addExtensor(sop);

            info.addOperation(bop);


            BindingMessageInfo bInput = bop.getInput();
            if (bInput != null) {
                MessageInfo input = null;
                BindingMessageInfo unwrappedMsg = bInput;
                if (bop.isUnwrappedCapable()) {
                    input = bop.getOperationInfo().getUnwrappedOperation().getInput();
                    unwrappedMsg = bop.getUnwrappedOperation().getInput();
                } else {
                    input = bop.getOperationInfo().getInput();
                }
                setupHeaders(bop, bInput, unwrappedMsg, input, config);
            }

            BindingMessageInfo bOutput = bop.getOutput();
            if (bOutput != null) {
                MessageInfo output = null;
                BindingMessageInfo unwrappedMsg = bOutput;
                if (bop.isUnwrappedCapable()) {
                    output = bop.getOperationInfo().getUnwrappedOperation().getOutput();
                    unwrappedMsg = bop.getUnwrappedOperation().getOutput();
                } else {
                    output = bop.getOperationInfo().getOutput();
                }
                setupHeaders(bop, bOutput, unwrappedMsg, output, config);
            }
        }

        try {
            createSoapBinding(info);
        } catch (WSDLException e) {
            e.printStackTrace();
        }

        return info;
    }


    private void createSoapBinding(final SoapBindingInfo bi) throws WSDLException {
        boolean isSoap12 = bi.getSoapVersion() instanceof Soap12;
        ExtensionRegistry extensionRegistry = getBus().getExtension(WSDLManager.class)
            .getExtenstionRegistry();

        SoapBinding soapBinding = SOAPBindingUtil.createSoapBinding(extensionRegistry, isSoap12);
        soapBinding.setStyle(bi.getStyle());
        soapBinding.setTransportURI(bi.getTransportURI());
        bi.addExtensor(soapBinding);

        for (BindingOperationInfo b : bi.getOperations()) {
            for (BindingFaultInfo faultInfo : b.getFaults()) {
                SoapFault soapFault = SOAPBindingUtil.createSoapFault(extensionRegistry, isSoap12);
                soapFault.setUse("literal");
                soapFault.setName(faultInfo.getFaultInfo().getFaultName().getLocalPart());
                faultInfo.addExtensor(soapFault);
            }
            SoapOperationInfo soi = b.getExtensor(SoapOperationInfo.class);

            SoapOperation soapOperation = SOAPBindingUtil.createSoapOperation(extensionRegistry,
                                                                              isSoap12);
            soapOperation.setSoapActionURI(soi.getAction());
            soapOperation.setStyle(soi.getStyle());
            boolean isRpc = "rpc".equals(soapOperation.getStyle());

            b.addExtensor(soapOperation);

            if (b.getInput() != null) {
                List<String> bodyParts = null;
                SoapHeaderInfo headerInfo = b.getInput().getExtensor(SoapHeaderInfo.class);
                if (headerInfo != null) {
                    bodyParts = new ArrayList<String>();
                    for (MessagePartInfo part : b.getInput().getMessageParts()) {
                        bodyParts.add(part.getName().getLocalPart());
                    }

                    SoapHeader soapHeader = SOAPBindingUtil.createSoapHeader(extensionRegistry,
                                                                             BindingInput.class,
                                                                             isSoap12);
                    soapHeader.setMessage(b.getInput().getMessageInfo().getName());
                    soapHeader.setPart(headerInfo.getPart().getName().getLocalPart());
                    soapHeader.setUse("literal");
                    bodyParts.remove(headerInfo.getPart().getName().getLocalPart());
                    b.getInput().addExtensor(soapHeader);

                }
                SoapBody body = SOAPBindingUtil.createSoapBody(extensionRegistry,
                                                               BindingInput.class,
                                                               isSoap12);
                body.setUse("literal");
                if (isRpc) {
                    body.setNamespaceURI(b.getName().getNamespaceURI());
                }

                if (bodyParts != null) {
                    body.setParts(bodyParts);
                }

                b.getInput().addExtensor(body);
            }

            if (b.getOutput() != null) {
                List<String> bodyParts = null;
                SoapHeaderInfo headerInfo = b.getOutput().getExtensor(SoapHeaderInfo.class);
                if (headerInfo != null) {
                    bodyParts = new ArrayList<String>();
                    for (MessagePartInfo part : b.getOutput().getMessageParts()) {
                        bodyParts.add(part.getName().getLocalPart());
                    }
                    SoapHeader soapHeader = SOAPBindingUtil.createSoapHeader(extensionRegistry,
                                                                             BindingOutput.class,
                                                                             isSoap12);
                    soapHeader.setMessage(b.getOutput().getMessageInfo().getName());
                    soapHeader.setPart(headerInfo.getPart().getName().getLocalPart());
                    soapHeader.setUse("literal");
                    bodyParts.remove(headerInfo.getPart().getName().getLocalPart());
                    b.getOutput().addExtensor(soapHeader);

                }
                SoapBody body = SOAPBindingUtil.createSoapBody(extensionRegistry,
                                                               BindingOutput.class,
                                                               isSoap12);
                body.setUse("literal");
                if (isRpc) {
                    body.setNamespaceURI(b.getName().getNamespaceURI());
                }

                if (bodyParts != null) {
                    body.setParts(bodyParts);
                }

                b.getOutput().addExtensor(body);
            }
        }
    }


    private void setupHeaders(BindingOperationInfo op,
                              BindingMessageInfo bMsg,
                              BindingMessageInfo unwrappedBMsg,
                              MessageInfo msg,
                              SoapBindingConfiguration config) {
        List<MessagePartInfo> parts = new ArrayList<MessagePartInfo>();
        for (MessagePartInfo part : msg.getMessageParts()) {
            if (config.isHeader(op, part)) {
                SoapHeaderInfo headerInfo = new SoapHeaderInfo();
                headerInfo.setPart(part);
                headerInfo.setUse(config.getUse());

                bMsg.addExtensor(headerInfo);
            } else {
                parts.add(part);
            }
        }
        unwrappedBMsg.setMessageParts(parts);
    }

    public Binding createBinding(BindingInfo binding) {
        // TODO what about the mix style/use?

        // The default style should be doc-lit wrapped.
        String parameterStyle = SoapConstants.PARAMETER_STYLE_WRAPPED;
        String bindingStyle = SoapConstants.BINDING_STYLE_DOC;

        org.apache.cxf.binding.soap.SoapBinding sb = null;
        SoapVersion version = null;
        if (binding instanceof SoapBindingInfo) {
            SoapBindingInfo sbi = (SoapBindingInfo) binding;
            version = sbi.getSoapVersion();
            sb = new org.apache.cxf.binding.soap.SoapBinding(binding, version);
            // Service wide style
            if (!StringUtils.isEmpty(sbi.getStyle())) {
                bindingStyle = sbi.getStyle();
            }

            // Operation wide style, what to do with the mixed style/use?
            for (BindingOperationInfo boi : sbi.getOperations()) {
                if (sbi.getStyle(boi.getOperationInfo()) != null) {
                    bindingStyle = sbi.getStyle(boi.getOperationInfo());
                }
                if (boi.getUnwrappedOperation() == null) {
                    parameterStyle = SoapConstants.PARAMETER_STYLE_BARE;
                }
            }
        } else {
            throw new RuntimeException("Can not initialize SoapBinding, BindingInfo is not SoapBindingInfo");
        }

        sb.getOutFaultInterceptors().add(new StaxOutInterceptor());

        //Do not add any interceptors if it is Provider/Dispatch
        if (!Boolean.TRUE.equals(binding.getProperty(DATABINDING_DISABLED))) {
            sb.getInInterceptors().add(new AttachmentInInterceptor());
            sb.getInInterceptors().add(new StaxInInterceptor());

            sb.getOutInterceptors().add(new SoapActionInterceptor());
            sb.getOutInterceptors().add(new AttachmentOutInterceptor());
            sb.getOutInterceptors().add(new StaxOutInterceptor());

            if (SoapConstants.BINDING_STYLE_RPC.equalsIgnoreCase(bindingStyle)) {
                sb.getInInterceptors().add(new RPCInInterceptor());
                sb.getOutInterceptors().add(new RPCOutInterceptor());
            } else if (SoapConstants.BINDING_STYLE_DOC.equalsIgnoreCase(bindingStyle)
                            && SoapConstants.PARAMETER_STYLE_BARE.equalsIgnoreCase(parameterStyle)) {
                //sb.getInInterceptors().add(new BareInInterceptor());
                sb.getInInterceptors().add(new DocLiteralInInterceptor());
                sb.getOutInterceptors().add(new BareOutInterceptor());
            } else {
                //sb.getInInterceptors().add(new WrappedInInterceptor());
                sb.getInInterceptors().add(new DocLiteralInInterceptor());
                sb.getOutInterceptors().add(new WrappedOutInterceptor());
                sb.getOutInterceptors().add(new BareOutInterceptor());
            }
            sb.getInInterceptors().add(new SoapHeaderInterceptor());

            sb.getInInterceptors().add(new ReadHeadersInterceptor(getBus()));
            sb.getInInterceptors().add(new MustUnderstandInterceptor());
            sb.getOutInterceptors().add(new SoapPreProtocolOutInterceptor());
            sb.getOutInterceptors().add(new SoapOutInterceptor(getBus()));
            sb.getOutFaultInterceptors().add(new SoapOutInterceptor(getBus()));

            // REVISIT: The phase interceptor chain seems to freak out if this added
            // first. Not sure what the deal is at the moment, I suspect the
            // ordering algorithm needs to be improved
            sb.getInInterceptors().add(new URIMappingInterceptor());
        }

        if (version.getVersion() == 1.1) {
            sb.getInFaultInterceptors().add(new Soap11FaultInInterceptor());
            sb.getOutFaultInterceptors().add(new Soap11FaultOutInterceptor());
        } else if (version.getVersion() == 1.2) {
            sb.getInFaultInterceptors().add(new Soap12FaultInInterceptor());
            sb.getOutFaultInterceptors().add(new Soap12FaultOutInterceptor());
        }

        return sb;
    }

    protected void addMessageFromBinding(ExtensibilityElement ext, BindingOperationInfo bop,
                                         boolean isInput) {
        SoapHeader header = SOAPBindingUtil.getSoapHeader(ext);

        ServiceInfo serviceInfo = bop.getBinding().getService();

        if (header != null && serviceInfo.getMessage(header.getMessage()) == null) {
            Definition def = (Definition)serviceInfo.getProperty(WSDLServiceBuilder.WSDL_DEFINITION);
            XmlSchemaCollection schemas = (XmlSchemaCollection)serviceInfo
                .getProperty(WSDLServiceBuilder.WSDL_SCHEMA_LIST);

            if (def != null && schemas != null) {
                javax.wsdl.Message msg = def.getMessage(header.getMessage());
                if (msg != null) {
                    addOutOfBandParts(bop, msg, schemas, isInput);
                    serviceInfo.refresh();
                } else {
                    //TODO: The header message is not defined in this wsdl, what to do
                }
            }
        }
    }

    private void addOutOfBandParts(final BindingOperationInfo bop, final javax.wsdl.Message msg,
                                   final XmlSchemaCollection schemas, boolean isInput) {
        MessageInfo minfo = null;
        if (isInput) {
            minfo = bop.getOperationInfo().getInput();
        } else {
            minfo = bop.getOperationInfo().getOutput();
        }

        if (minfo == null) {
            minfo = new MessageInfo(null, msg.getQName());
        }
        buildMessage(minfo, msg, schemas);

        // for wrapped style
        OperationInfo unwrapped = bop.getOperationInfo().getUnwrappedOperation();
        if (unwrapped == null) {
            return;
        }
        if (isInput) {
            minfo = unwrapped.getInput();
        } else {
            minfo = unwrapped.getOutput();
        }

        if (minfo == null) {
            minfo = new MessageInfo(unwrapped, msg.getQName());
        }
        buildMessage(minfo, msg, schemas);
    }

    private void buildMessage(MessageInfo minfo, javax.wsdl.Message msg,
                              XmlSchemaCollection schemas) {
        for (Part part : cast(msg.getParts().values(), Part.class)) {
            MessagePartInfo pi = minfo.addMessagePart(new QName(minfo.getName().getNamespaceURI(), part
                                                                .getName()));
            if (part.getTypeName() != null) {
                pi.setTypeQName(part.getTypeName());
                pi.setElement(false);
                pi.setXmlSchema(schemas.getTypeByQName(part.getTypeName()));
            } else {
                pi.setElementQName(part.getElementName());
                pi.setElement(true);
                pi.setXmlSchema(schemas.getElementByQName(part.getElementName()));
            }
            pi.setProperty(OUT_OF_BAND_HEADER, Boolean.TRUE);
        }
    }

    public BindingInfo createBindingInfo(ServiceInfo service, javax.wsdl.Binding binding, String ns) {
        SoapBindingInfo bi = new SoapBindingInfo(service, ns);
        // Copy all the extensors
        initializeBindingInfo(service, binding, bi);

        SoapBinding wSoapBinding
            = SOAPBindingUtil.getSoapBinding(bi.getExtensors(ExtensibilityElement.class));


        bi.setTransportURI(wSoapBinding.getTransportURI());
        bi.setStyle(wSoapBinding.getStyle());

        for (BindingOperationInfo boi : bi.getOperations()) {
            initializeBindingOperation(bi, boi);
        }

        return bi;
    }

    private void initializeBindingOperation(SoapBindingInfo bi, BindingOperationInfo boi) {
        SoapOperationInfo soi = new SoapOperationInfo();

        SoapOperation soapOp =
            SOAPBindingUtil.getSoapOperation(boi.getExtensors(ExtensibilityElement.class));

        if (soapOp != null) {
            String action = soapOp.getSoapActionURI();
            if (action == null) {
                action = "";
            }

            soi.setAction(action);
            soi.setStyle(soapOp.getStyle());
        }

        boi.addExtensor(soi);

        if (boi.getInput() != null) {
            initializeMessage(bi, boi, boi.getInput());
        }

        if (boi.getOutput() != null) {
            initializeMessage(bi, boi, boi.getOutput());
        }
    }

    private void initializeMessage(SoapBindingInfo bi, BindingOperationInfo boi, BindingMessageInfo bmsg) {
        MessageInfo msg = bmsg.getMessageInfo();

        List<MessagePartInfo> messageParts = new ArrayList<MessagePartInfo>();
        messageParts.addAll(msg.getMessageParts());

        List<SoapHeader> headers =
            SOAPBindingUtil.getSoapHeaders(bmsg.getExtensors(ExtensibilityElement.class));
        if (headers != null) {
            for (SoapHeader header : headers) {
                SoapHeaderInfo headerInfo = new SoapHeaderInfo();
                headerInfo.setUse(header.getUse());
                MessagePartInfo part = msg.getMessagePart(new QName(msg.getName().getNamespaceURI(), header
                                .getPart()));
                if (part != null) {
                    headerInfo.setPart(part);
                    messageParts.remove(part);
                    bmsg.addExtensor(headerInfo);
                }
            }

            // Exclude the header parts from the message part list.
            bmsg.setMessageParts(messageParts);
        }

        SoapBodyInfo bodyInfo = new SoapBodyInfo();
        SoapBody soapBody = SOAPBindingUtil.getSoapBody(bmsg.getExtensors(ExtensibilityElement.class));
        List parts = null;
        if (soapBody == null) {
            MIMEMultipartRelated mmr = bmsg.getExtensor(MIMEMultipartRelated.class);
            if (mmr != null) {
                parts = mmr.getMIMEParts();
            }
        } else {
            bodyInfo.setUse(soapBody.getUse());
            parts = soapBody.getParts();
        }

        // Initialize the body parts.
        List<MessagePartInfo> attParts = null;
        if (parts != null) {
            List<MessagePartInfo> bodyParts = new ArrayList<MessagePartInfo>();
            for (Iterator itr = parts.iterator(); itr.hasNext();) {
                Object part = itr.next();
                String partName = null;
                if (part instanceof MIMEPart) {
                    MIMEPart mpart = (MIMEPart) part;
                    if (mpart.getExtensibilityElements().size() < 1) {
                        throw new RuntimeException("MIMEPart should at least contain one element!");
                    }
                    for (Object content : mpart.getExtensibilityElements()) {
                        if (content instanceof MIMEContent) {
                            MIMEContent mc = (MIMEContent)content;
                            partName = mc.getPart();

                            if (attParts == null) {
                                attParts = new LinkedList<MessagePartInfo>();
                            }

                            MessagePartInfo mpi =
                                msg.getMessagePart(new QName(msg.getName().getNamespaceURI(),
                                                             partName));
                            mpi.setProperty(Message.CONTENT_TYPE, mc.getType());
                            attParts.add(mpi);
                            // Attachments shouldn't be part of the body message
                            bmsg.getMessageParts().remove(mpi);
                        } else if (SOAPBindingUtil.isSOAPBody(content)) {
                            SoapBody sb = SOAPBindingUtil.getSoapBody(content);
                            if (sb.getParts().size() == 1) {
                                partName = (String) sb.getParts().get(0);
                            }

                            // We can have a list of empty part names here.
                            if (partName != null) {
                                addSoapBodyPart(msg, bodyParts, partName);
                            }
                        } else if (SOAPBindingUtil.isSOAPHeader(content)) {
                            SoapHeader header = SOAPBindingUtil.getSoapHeader(content);

                            SoapHeaderInfo headerInfo = new SoapHeaderInfo();
                            headerInfo.setUse(header.getUse());
                            MessagePartInfo mpi =
                                msg.getMessagePart(new QName(msg.getName().getNamespaceURI(), header
                                            .getPart()));
                            if (mpi != null) {
                                headerInfo.setPart(mpi);
                                messageParts.remove(part);
                                bmsg.getMessageParts().remove(mpi);
                                bmsg.addExtensor(headerInfo);
                            }
                        }
                    }
                } else {
                    addSoapBodyPart(msg, bodyParts, (String)part);
                }
            }
            bodyInfo.setParts(bodyParts);
            bodyInfo.setAttachments(attParts);
        } else {
            bodyInfo.setParts(messageParts);
        }

        bmsg.addExtensor(bodyInfo);
    }
    private void addSoapBodyPart(MessageInfo msg, List<MessagePartInfo> bodyParts, String partName) {
        MessagePartInfo mpi = msg.getMessagePart(new QName(msg.getName().getNamespaceURI(),
                                                           partName));
        bodyParts.add(mpi);
    }

    @Override
    public synchronized void addListener(Destination d, Endpoint e) {
        MessageObserver mo = d.getMessageObserver();
        if (mo == null) {
            super.addListener(d, e);
            return;
        }

        if (mo instanceof ChainInitiationObserver) {
            ChainInitiationObserver cio = (ChainInitiationObserver) mo;
            MultipleEndpointObserver newMO = new MultipleEndpointObserver(getBus()) {
                @Override
                protected Message createMessage(Message message) {
                    return new SoapMessage(message);
                }
            };

            newMO.getBindingInterceptors().add(new AttachmentInInterceptor());
            newMO.getBindingInterceptors().add(new StaxInInterceptor());

            // This will not work if we one of the endpoints disables message
            // processing. But, if you've disabled message processing, you
            // probably aren't going to use this feature.
            newMO.getBindingInterceptors().add(new ReadHeadersInterceptor(getBus()));

            // Add in a default selection interceptor
            newMO.getRoutingInterceptors().add(new EndpointSelectionInterceptor());

            newMO.getEndpoints().add(cio.getEndpoint());

            mo = newMO;
        }

        if (mo instanceof MultipleEndpointObserver) {
            MultipleEndpointObserver meo = (MultipleEndpointObserver) mo;
            meo.getEndpoints().add(e);
        }

        d.setMessageObserver(mo);
    }

    public void setMtomEnabled(boolean mtomEnabled) {
        this.mtomEnabled = mtomEnabled;
    }

    public boolean isMtomEnabled() {
        return mtomEnabled;
    }
}
