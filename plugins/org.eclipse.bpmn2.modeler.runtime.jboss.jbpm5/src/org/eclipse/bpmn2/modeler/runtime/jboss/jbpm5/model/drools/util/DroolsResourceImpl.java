/*******************************************************************************
 * Copyright (c) 2011, 2012 Red Hat, Inc.
 *  All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 *
 * @author Bob Brodt
 ******************************************************************************/
package org.eclipse.bpmn2.modeler.runtime.jboss.jbpm5.model.drools.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.bpmn2.Activity;
import org.eclipse.bpmn2.Bpmn2Package;
import org.eclipse.bpmn2.CallActivity;
import org.eclipse.bpmn2.CallableElement;
import org.eclipse.bpmn2.CatchEvent;
import org.eclipse.bpmn2.DataAssociation;
import org.eclipse.bpmn2.DataInput;
import org.eclipse.bpmn2.DataInputAssociation;
import org.eclipse.bpmn2.DataObject;
import org.eclipse.bpmn2.DataOutput;
import org.eclipse.bpmn2.DataOutputAssociation;
import org.eclipse.bpmn2.DataStore;
import org.eclipse.bpmn2.Definitions;
import org.eclipse.bpmn2.Error;
import org.eclipse.bpmn2.Escalation;
import org.eclipse.bpmn2.Event;
import org.eclipse.bpmn2.InputSet;
import org.eclipse.bpmn2.Interface;
import org.eclipse.bpmn2.ItemAwareElement;
import org.eclipse.bpmn2.ItemDefinition;
import org.eclipse.bpmn2.LoopCharacteristics;
import org.eclipse.bpmn2.Message;
import org.eclipse.bpmn2.MultiInstanceLoopCharacteristics;
import org.eclipse.bpmn2.OutputSet;
import org.eclipse.bpmn2.Process;
import org.eclipse.bpmn2.Property;
import org.eclipse.bpmn2.RootElement;
import org.eclipse.bpmn2.Signal;
import org.eclipse.bpmn2.ThrowEvent;
import org.eclipse.bpmn2.modeler.core.model.Bpmn2ModelerResourceImpl;
import org.eclipse.bpmn2.modeler.core.model.ModelDecorator;
import org.eclipse.bpmn2.modeler.core.utils.ModelUtil;
import org.eclipse.bpmn2.modeler.runtime.jboss.jbpm5.ProcessVariableNameChangeAdapter;
import org.eclipse.bpmn2.modeler.runtime.jboss.jbpm5.model.drools.DroolsFactory;
import org.eclipse.bpmn2.modeler.runtime.jboss.jbpm5.model.drools.DroolsPackage;
import org.eclipse.bpmn2.modeler.runtime.jboss.jbpm5.model.drools.ExternalProcess;
import org.eclipse.bpmn2.modeler.runtime.jboss.jbpm5.model.drools.GlobalType;
import org.eclipse.bpmn2.modeler.runtime.jboss.jbpm5.preferences.JbpmPreferencePage;
import org.eclipse.bpmn2.util.Bpmn2ResourceImpl;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.impl.EAttributeImpl;
import org.eclipse.emf.ecore.util.BasicFeatureMap;
import org.eclipse.emf.ecore.util.BasicInternalEList;
import org.eclipse.emf.ecore.util.ExtendedMetaData;
import org.eclipse.emf.ecore.xmi.XMLHelper;
import org.eclipse.emf.ecore.xmi.XMLLoad;
import org.eclipse.emf.ecore.xmi.XMLResource;
import org.eclipse.emf.ecore.xmi.XMLSave;
import org.eclipse.emf.ecore.xmi.impl.XMLLoadImpl;
import org.eclipse.emf.ecore.xmi.impl.XMLString;
import org.xml.sax.helpers.DefaultHandler;

/**
 * <!-- begin-user-doc -->
 * The <b>Resource </b> associated with the package.
 * <!-- end-user-doc -->
 * @see org.eclipse.bpmn2.modeler.runtime.jboss.jbpm5.model.util.ModelResourceFactoryImpl
 * @generated NOT
 */
public class DroolsResourceImpl extends Bpmn2ModelerResourceImpl {
	
	/**
	 * Creates an instance of the resource.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param uri the URI of the new resource.
	 * @generated NOT
	 */
	public DroolsResourceImpl(URI uri) {
		super(uri);
	}

    @Override
    protected XMLHelper createXMLHelper() {
    	if (xmlHelper!=null)
    		return xmlHelper;
    	return new DroolsXmlHelper(this);
    }

    /**
     * Override this method to hook in our own XmlHandler
     */
    @Override
    protected XMLLoad createXMLLoad() {
        return new XMLLoadImpl(createXMLHelper()) {
            @Override
            protected DefaultHandler makeDefaultHandler() {
                return new DroolsXmlHandler(resource, helper, options);
            }
        };
    }

    @Override
	protected XMLSave createXMLSave() {
		return new Bpmn2ModelerXMLSave(createXMLHelper()) {
			
			private boolean needTargetNamespace = true;
			
			@Override
			protected void saveElement(EObject o, EStructuralFeature f) {
				if (o instanceof ExternalProcess) {
					return;
				}
				super.saveElement(o, f);
			}

			@Override
			protected boolean shouldSaveFeature(EObject o, EStructuralFeature f) {
				
				if (Bpmn2Package.eINSTANCE.getDocumentation_Text().equals(f))
					return false;

				if (f==Bpmn2Package.eINSTANCE.getDefinitions_Relationships()) {
					if (!JbpmPreferencePage.isEnableSimulation())
						return false;
				}
				if (o instanceof GlobalType && f==Bpmn2Package.eINSTANCE.getBaseElement_Id()) {
					return false;
				}

				// workaround for Bug 430953 don't save the following collections:
				//
				// OutputSet
				// DataOutputs
				// DataOutputAssociation
				// InputSet
				// DataInputs
				// DataInputAssociations
				//
				// if no associations have been made.
				if (o instanceof CatchEvent) {
					CatchEvent event = (CatchEvent) o;
					String featureName = f.getName();
					if (	featureName.equals("outputSet") ||
							featureName.equals("dataOutputs") ||
							featureName.equals("dataOutputAssociation")) {
						if (	event.getOutputSet()==null ||
								event.getOutputSet().getDataOutputRefs().size()==0 ||
								event.getDataOutputAssociation().size()==0 ||
								event.getDataOutputs().size()==0) {
							return false;
						}
						DataOutputAssociation association = event.getDataOutputAssociation().get(0);
						if (association.getTargetRef()==null)
							return false;
					}
				}
				if (o.eContainer() instanceof CatchEvent) {
					CatchEvent event = (CatchEvent) o.eContainer();
					if (	o instanceof OutputSet ||
							o instanceof DataOutput ||
							o instanceof DataOutputAssociation) {
						if (	event.getOutputSet()==null ||
								event.getOutputSet().getDataOutputRefs().size()==0 ||
								event.getDataOutputAssociation().size()==0 ||
								event.getDataOutputs().size()==0) {
							return false;
						}
						DataOutputAssociation association = event.getDataOutputAssociation().get(0);
						if (association.getTargetRef()==null)
							return false;
					}
				}

				if (o instanceof ThrowEvent) {
					ThrowEvent event = (ThrowEvent) o;
					String featureName = f.getName();
					if (	featureName.equals("inputSet") ||
							featureName.equals("dataInputs") ||
							featureName.equals("dataInputAssociation")) {
						if (	event.getInputSet()==null ||
								event.getInputSet().getDataInputRefs().size()==0 ||
								event.getDataInputAssociation().size()==0 ||
								event.getDataInputs().size()==0) {
							return false;
						}
						DataInputAssociation association = event.getDataInputAssociation().get(0);
						if (association.getTargetRef()==null)
							return false;
					}
				}
				if (o.eContainer() instanceof ThrowEvent) {
					ThrowEvent event = (ThrowEvent) o.eContainer();
					if (	o instanceof InputSet ||
							o instanceof DataInput ||
							o instanceof DataInputAssociation) {
						if (	event.getInputSet()==null ||
								event.getInputSet().getDataInputRefs().size()==0 ||
								event.getDataInputAssociation().size()==0 ||
								event.getDataInputs().size()==0) {
							return false;
						}
						DataInputAssociation association = event.getDataInputAssociation().get(0);
						if (association.getTargetRef()==null)
							return false;
					}
				}

				return super.shouldSaveFeature(o, f);
			}

			@Override
			protected void init(XMLResource resource, Map<?, ?> options) {
				super.init(resource, options);

				((DroolsXmlHelper)helper).setDefaultNamespace();
	        
		        featureTable = new Bpmn2ModelerXMLSave.Bpmn2Lookup(map, extendedMetaData, elementHandler) {
		        	@Override
		    		protected EStructuralFeature[] reorderFeatureList(EClass cls, EStructuralFeature[] featureList) {
		        		EStructuralFeature[] newList = null;
		    			if (cls.getName().equalsIgnoreCase("Process")) {
		    				/*
		    				 * Semantic.xsd sequence definition for Process:
		    				 * 
		    				 * <xsd:sequence>
		    				 * <xsd:element ref="auditing" minOccurs="0" maxOccurs="1"/>
		    				 * <xsd:element ref="monitoring" minOccurs="0" maxOccurs="1"/>
		    				 * <xsd:element ref="property" minOccurs="0" maxOccurs="unbounded"/>
		    				 * <xsd:element ref="laneSet" minOccurs="0" maxOccurs="unbounded"/>
		    				 * <xsd:element ref="flowElement" minOccurs="0" maxOccurs="unbounded"/>
		    				 * <xsd:element ref="artifact" minOccurs="0" maxOccurs="unbounded"/>
		    				 * <xsd:element ref="resourceRole" minOccurs="0" maxOccurs="unbounded"/>
		    				 * <xsd:element ref="correlationSubscription" minOccurs="0" maxOccurs="unbounded"/>
		    				 * <xsd:element name="supports" type="xsd:QName" minOccurs="0" maxOccurs="unbounded"/>
		    				 * </xsd:sequence>
		    				 */
		    				String[] featureNames = {
		    					"auditing",
		    					"monitoring",
		    					"properties",
		    					"laneSets",
		    					"flowElements",
		    					"artifacts",
		    					"resources",
		    					"correlationSubscriptions",
		    					"supports"
		    				};
		    				newList = reorderFeatureList(cls, featureList, featureNames);
		    			}
		    			else
		    				newList = featureList;
		    			
		    			// for BaseElements, "documentation" always comes before "extensionValues"
		    			if (Bpmn2Package.eINSTANCE.getBaseElement().isSuperTypeOf(cls)) {
		    				String[] featureNames = {
		    					"documentation",
		    					"extensionValues"
		    				};
			    			newList = reorderFeatureList(cls, newList, featureNames);
		    			}
		    			
		    			return newList;
		        	}
		        };
			}
			
			protected XMLString createXMLString() {
		        return new Bpmn2ModelerXMLString(publicId, systemId) {
		        	@Override
		        	public void addAttribute(String name, String value) {
		        		if ("targetNamespace".equals(name))
		        			needTargetNamespace = false;
		        		else if (XSI_SCHEMA_LOCATION.equals(name)) {
		        			value = "http://www.omg.org/spec/BPMN/20100524/MODEL BPMN20.xsd" +
		        					" http://www.jboss.org/drools drools.xsd"+
		        					" http://www.bpsim.org/schemas/1.0 bpsim.xsd";
		        		}
		        			
		        		super.addAttribute(name, value);
		        	}
		        };
			}
			
			@Override
			protected void addNamespaceDeclarations() {
				if (needTargetNamespace)
					doc.addAttribute("targetNamespace", DroolsPackage.eNS_URI);
				super.addNamespaceDeclarations();
			}
			
		};
	}

    protected static class DroolsXmlHelper extends Bpmn2ModelerXmlHelper {

		public DroolsXmlHelper(Bpmn2ResourceImpl resource) {
			super(resource);
//			qnameMap.clear();
//			qnameMap.remove(Bpmn2Package.eINSTANCE.getItemAwareElement_ItemSubjectRef());
//			qnameMap.remove(Bpmn2Package.eINSTANCE.getInterface_ImplementationRef());
//			qnameMap.remove(Bpmn2Package.eINSTANCE.getOperation_ImplementationRef());
		}
		
		public void setDefaultNamespace() {
			EPackage ePackage = Bpmn2Package.eINSTANCE;
			String nsURI = xmlSchemaTypePackage == ePackage ? XMLResource.XML_SCHEMA_URI
					: extendedMetaData == null ? ePackage.getNsURI()
							: extendedMetaData.getNamespace(ePackage);

			String bpmn2Prefix = "bpmn2";
			int suffix = 1;
			for (;;) {
				String ns = prefixesToURIs.get(bpmn2Prefix);
				if (ns!=null && !ns.equals(nsURI)) {
					bpmn2Prefix = "bpmn2" + "_" + suffix++;
				}
				else
					break;
			}

			boolean found = false;
			List<String> prefixes = urisToPrefixes.get(nsURI);
			if (prefixes==null) {
				prefixes = new ArrayList<String>();
				urisToPrefixes.put(nsURI, prefixes);
			}
			for (int i = 0; i < prefixes.size(); ++i) {
				String prefix = prefixes.get(i);
				if ("".equals(prefix)) {
					prefixes.set(i, bpmn2Prefix);
					found = true;
					break;
				}
				else if (bpmn2Prefix.equals(prefix)) {
					found = true;
				}
					
			}
			if (!found)
				prefixes.add(0, bpmn2Prefix);
			
			nsURI = DroolsPackage.eNS_URI;
			prefixes = urisToPrefixes.get(nsURI);
			if (prefixes!=null)
				prefixes.add("");

			for (int i=0; i<allPrefixToURI.size(); ++i) {
				String prefix = allPrefixToURI.get(i);
				if ("".equals(prefix)) {
					allPrefixToURI.remove(i);
					if (i+1<allPrefixToURI.size())
						allPrefixToURI.remove(i+1);
					break;
				}
			}
			allPrefixToURI.add("");
			allPrefixToURI.add(nsURI);
			prefixesToURIs.remove("");
			
			// change default namespace for existing packages
			for (Entry<EPackage, String> e : packages.entrySet()) {
				if ("".equals(e.getValue())) {
					ePackage = e.getKey();
					if (e.getKey() != DroolsPackage.eINSTANCE) {
						packages.remove(e.getKey());
						packages.put(ePackage, ePackage.getNsPrefix());
						urisToPrefixes.remove(ePackage.getNsURI());
						break;
					}
				}
			}

			mustHavePrefix = true;
		}
	    
	    public List<String> getPrefixes(EPackage ePackage) {
	    	List<String> result = super.getPrefixes(ePackage);
	    	boolean found = false;
	    	for (int i=0; i<result.size(); ++i) {
	    		if ("".equals(result.get(i))) {
	    			if (ePackage == DroolsPackage.eINSTANCE) {
	    				found = true;
	    				break; // ok
	    			}
	    			else {
	    				result.remove(i--);
	    			}
	    		}
	    	}
			if (ePackage == DroolsPackage.eINSTANCE && !found) {
				result.add("");
			}
			return result;
	    }

	    @Override
	    public Object getValue(EObject eObject, EStructuralFeature eStructuralFeature) {
	    	
	    	if (eObject instanceof Definitions && eStructuralFeature == Bpmn2Package.eINSTANCE.getDefinitions_RootElements()) {
	    		// reorder the root elements so that ItemDefinitions are first, Process is last
	    		// and everything else is in between
	    		List<RootElement> oldList = ((Definitions)eObject).getRootElements();
	    		List<RootElement> newList = new ArrayList<RootElement>();
	    		for (RootElement re : oldList) {
	    			if (re instanceof ItemDefinition) {
	    				newList.add(re);
	    			}
	    		}
	    		for (RootElement re : oldList) {
	    			if (!(re instanceof ItemDefinition) && !(re instanceof Process)) {
	    				newList.add(re);
	    			}
	    		}
	    		for (RootElement re : oldList) {
	    			if (re instanceof Process) {
	    				newList.add(re);
	    			}
	    		}
	    		return new BasicInternalEList<RootElement>(RootElement.class, newList);
	    	}
	    	return super.getValue(eObject, eStructuralFeature);
	    }
    }
    
	/**
     * We need extend the standard SAXXMLHandler to hook into the handling of attribute references
     * which may be either simple ID Strings or QNames. We'll search through all of the objects'
     * IDs first to find the one we're looking for. If not, we'll try a QName search.
     */
    protected static class DroolsXmlHandler extends Bpmn2ModelerXmlHandler {

        public DroolsXmlHandler(XMLResource xmiResource, XMLHelper helper, Map<?, ?> options) {
            super(xmiResource, helper, options);
        }

        @Override
        protected EStructuralFeature getFeature(EObject object, String prefix, String name, boolean isElement)
        {
        	EStructuralFeature result = null;
		    if (object!=null && object.eClass().getEPackage() == DroolsPackage.eINSTANCE) {
		    	result = object.eClass().getEStructuralFeature(name);
		    }
		    if (result==null)
		    	return super.getFeature(object, prefix, name, isElement);
		    return result;
        }
        
		@SuppressWarnings("unchecked")
		@Override
		protected void processElement(String name, String prefix, String localName) {
		    EObject peekObject = objects.peekEObject();
		    if (peekObject!=null && peekObject.eClass().getEPackage() == DroolsPackage.eINSTANCE) {
		    	prefix = helper.getPrefix(DroolsPackage.eINSTANCE);
		    }
			super.processElement(name, prefix, localName);
			
			// ugly hack for https://bugs.eclipse.org/bugs/show_bug.cgi?id=355686
			// Remove the "type" attribute from the feature parentMap if there is one.
			// The XSI type will have already been used to construct the EObject,
			// so any "type" in the feature parentMap will be a duplicate which will
			// cause problems during parsing.
			// See also getXSIType()
			EObject childObject = objects.peekEObject();
			if (childObject!=null) {
				try {
					EStructuralFeature anyAttribute = childObject.eClass().getEStructuralFeature(Bpmn2Package.BASE_ELEMENT__ANY_ATTRIBUTE);
					if (anyAttribute!=null) {
						List<BasicFeatureMap.Entry> anyMap = (List<BasicFeatureMap.Entry>)childObject.eGet(anyAttribute);
						if (anyMap!=null) {
							List<BasicFeatureMap.Entry> removed = new ArrayList<BasicFeatureMap.Entry>();
							for (BasicFeatureMap.Entry fe : anyMap) {
								if (fe.getEStructuralFeature() instanceof EAttribute) {
									EAttributeImpl a = (EAttributeImpl)fe.getEStructuralFeature();
									String n = a.getName();
									String ns = a.getExtendedMetaData().getNamespace();
									if (TYPE.equals(n) && XSI_URI.equals(ns)) {
										removed.add(fe);
									}
								}
							}
							if (removed.size()>0)
								anyMap.removeAll(removed);
						}
					}
					
					if (childObject instanceof GlobalType) {
						// The BaseElement feature "id" is not saved, but it MUST be kept in sync with the
						// GlobalType feature "identifier" - this acts like the "name" feature of other
						// ItemAwareElements that treat "name" like an ID.
						// @see ProcessVariableNameChangeAdapter for details of how these are kept in sync.
						((GlobalType) childObject).setId(((GlobalType) childObject).getIdentifier());
					}
					else if (ProcessVariableNameChangeAdapter.appliesTo(childObject)) {
						EStructuralFeature nameFeature = childObject.eClass().getEStructuralFeature("name"); //$NON-NLS-1$
						String n = (String) childObject.eGet(nameFeature);
						if (n==null || n.isEmpty()) {
							EStructuralFeature idFeature = childObject.eClass().getEStructuralFeature("id"); //$NON-NLS-1$
							childObject.eSet(nameFeature, childObject.eGet(idFeature));
						}
					}
				}
				catch(Exception e) {
				}
			}
		}

		@Override
		protected String getXSIType() {
			if (isNamespaceAware)
				return attribs.getValue(ExtendedMetaData.XSI_URI,
						XMLResource.TYPE);

			// If an parameter specifies multiple xsi:type data types, the last one wins.
			// NOTE: we must check for "type" in any namespace with the URI
			// "http://www.w3.org/2001/XMLSchema-instance"
			String value = null;
			int length = attribs.getLength();
			for (int i = 0; i < length; ++i) {
				attribs.getQName(i);
				String localpart= attribs.getLocalName(i);
				String prefix = null;
				int ci = localpart.lastIndexOf(':');
				if (ci>0) {
					prefix = localpart.substring(0, ci); 
					localpart = localpart.substring(ci+1);
				}
				if (TYPE.equals(localpart)) {
					String uri = helper.getNamespaceURI(prefix);
					if (XSI_URI.equals(uri)) {
						value = attribs.getValue(i);
						if (value!=null && value.startsWith("drools:") && value.contains("_._type"))
							value = value.replace("_._type", "");
					}
				}
			}
			return value;
		}
		
		@Override
		protected void setValueFromId(EObject object, EReference eReference, String ids) {
			if (object instanceof CallActivity && eReference==Bpmn2Package.eINSTANCE.getCallActivity_CalledElementRef()) {
				// the CalledElementRef in CallActivity is just an ID. This means we need
				// to create a CallableElement which is simply a proxy, not a real object.
				CallActivity ca = (CallActivity)object;
				CallableElement ce = null;
				Definitions defs = ModelUtil.getDefinitions(helper.getResource());
				for (RootElement re : defs.getRootElements()) {
					if (re instanceof ExternalProcess) {
						if (ids.equals(re.getId())) {
							ce = (ExternalProcess) re;
							break;
						}
					}
				}
				if (ce==null) {
					ce = DroolsFactory.eINSTANCE.createExternalProcess();
					ce.setName(ids);
					ce.setId(ids);
					defs.getRootElements().add(ce);
				}
				ca.setCalledElementRef(ce);
			}
			else if (object instanceof Interface && eReference==Bpmn2Package.eINSTANCE.getInterface_ImplementationRef()) {
				// the Interface.implementationRef is yet again just a string
				Interface iface = (Interface)object;
				iface.setImplementationRef( ModelUtil.createStringWrapper(ids) );
			}
			else if (object instanceof DataAssociation) {
				ItemAwareElement element = findItemAwareElement(object,ids);
				if (element!=null) {
					if (eReference.isMany()) {
						EList list = (EList)object.eGet(eReference);
						list.add(element);
					}
					else
						object.eSet(eReference, element);
				}
				else
					super.setValueFromId(object, eReference, ids);
			}
			else
				super.setValueFromId(object, eReference, ids);
		}
		
		private ItemAwareElement findItemAwareElement(EObject object, String ids) {
			EObject container = object.eContainer();
			do {
				if (container instanceof Activity) {
					Activity activity = (Activity)container;
					for (Property p : activity.getProperties()) {
						if (ids.equals(p.getId()))
							return p;
					}
					LoopCharacteristics lc = activity.getLoopCharacteristics();
					if (lc instanceof MultiInstanceLoopCharacteristics) {
						MultiInstanceLoopCharacteristics mlc = (MultiInstanceLoopCharacteristics)lc;
						if (mlc.getInputDataItem()!=null && ids.equals(mlc.getInputDataItem().getName()))
							return mlc.getInputDataItem();
						if (mlc.getOutputDataItem()!=null && ids.equals(mlc.getOutputDataItem().getName()))
							return mlc.getOutputDataItem();
					}
				}
				else if (container instanceof Event) {
					Event event = (Event)container;
					for (Property p : event.getProperties()) {
						if (ids.equals(p.getId()))
							return p;
					}
				}
				else if (container instanceof Process) {
					Process process = (Process)container;
					for (Property p : process.getProperties()) {
						if (ids.equals(p.getId()))
							return p;
					}
					
					for (GlobalType g : ModelDecorator.getAllExtensionAttributeValues(process, GlobalType.class)) {
						if (ids.equals(g.getIdentifier()))
							return g;
					}
				}
				else if (container instanceof Definitions) {
					Definitions definitions = (Definitions)container;
					for (DataObject d : ModelUtil.getAllRootElements(definitions, DataObject.class)) {
						if (ids.equals(d.getId()))
							return d;
					}
					for (DataStore s : ModelUtil.getAllRootElements(definitions, DataStore.class)) {
						if (ids.equals(s.getId()))
							return s;
					}
				}
				container = container.eContainer();
			}
			while (container!=null);
			return null;
		}
    }
} //ModelResourceImpl
