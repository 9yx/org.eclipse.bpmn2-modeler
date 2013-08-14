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
 * @author Innar Made
 ******************************************************************************/
package org.eclipse.bpmn2.modeler.ui.features.flow;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.bpmn2.Activity;
import org.eclipse.bpmn2.BaseElement;
import org.eclipse.bpmn2.Bpmn2Factory;
import org.eclipse.bpmn2.Bpmn2Package;
import org.eclipse.bpmn2.CatchEvent;
import org.eclipse.bpmn2.DataAssociation;
import org.eclipse.bpmn2.DataInput;
import org.eclipse.bpmn2.DataInputAssociation;
import org.eclipse.bpmn2.DataOutput;
import org.eclipse.bpmn2.DataOutputAssociation;
import org.eclipse.bpmn2.InputOutputSpecification;
import org.eclipse.bpmn2.InputSet;
import org.eclipse.bpmn2.ItemAwareElement;
import org.eclipse.bpmn2.OutputSet;
import org.eclipse.bpmn2.ThrowEvent;
import org.eclipse.bpmn2.modeler.core.features.BaseElementConnectionFeatureContainer;
import org.eclipse.bpmn2.modeler.core.features.flow.AbstractAddFlowFeature;
import org.eclipse.bpmn2.modeler.core.features.flow.AbstractCreateFlowFeature;
import org.eclipse.bpmn2.modeler.core.features.flow.AbstractReconnectFlowFeature;
import org.eclipse.bpmn2.modeler.core.model.Bpmn2ModelerFactory;
import org.eclipse.bpmn2.modeler.core.utils.AnchorUtil;
import org.eclipse.bpmn2.modeler.core.utils.BusinessObjectUtil;
import org.eclipse.bpmn2.modeler.core.utils.ModelUtil;
import org.eclipse.bpmn2.modeler.core.utils.StyleUtil;
import org.eclipse.bpmn2.modeler.ui.ImageProvider;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.graphiti.features.IAddFeature;
import org.eclipse.graphiti.features.ICreateConnectionFeature;
import org.eclipse.graphiti.features.IFeatureProvider;
import org.eclipse.graphiti.features.IReason;
import org.eclipse.graphiti.features.IReconnectionFeature;
import org.eclipse.graphiti.features.IUpdateFeature;
import org.eclipse.graphiti.features.context.IAddConnectionContext;
import org.eclipse.graphiti.features.context.IAddContext;
import org.eclipse.graphiti.features.context.IContext;
import org.eclipse.graphiti.features.context.ICreateConnectionContext;
import org.eclipse.graphiti.features.context.IReconnectionContext;
import org.eclipse.graphiti.features.context.IUpdateContext;
import org.eclipse.graphiti.features.context.impl.AddConnectionContext;
import org.eclipse.graphiti.features.context.impl.CreateConnectionContext;
import org.eclipse.graphiti.features.context.impl.ReconnectionContext;
import org.eclipse.graphiti.features.impl.AbstractUpdateFeature;
import org.eclipse.graphiti.features.impl.Reason;
import org.eclipse.graphiti.mm.algorithms.Polyline;
import org.eclipse.graphiti.mm.algorithms.styles.LineStyle;
import org.eclipse.graphiti.mm.pictograms.Anchor;
import org.eclipse.graphiti.mm.pictograms.AnchorContainer;
import org.eclipse.graphiti.mm.pictograms.Connection;
import org.eclipse.graphiti.mm.pictograms.ConnectionDecorator;
import org.eclipse.graphiti.mm.pictograms.FreeFormConnection;
import org.eclipse.graphiti.mm.pictograms.PictogramElement;
import org.eclipse.graphiti.mm.pictograms.Shape;
import org.eclipse.graphiti.services.Graphiti;
import org.eclipse.graphiti.services.IGaService;
import org.eclipse.graphiti.services.IPeService;
import org.eclipse.graphiti.ui.internal.util.ui.PopupMenu;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;

public class DataAssociationFeatureContainer extends BaseElementConnectionFeatureContainer {

	// the property used to store the current Association's direction;
	// the value can be one of the AssociationDirection enumerations (a null
	// or empty string is the same as "None")
	public static final String ASSOCIATION_DIRECTION = "association.direction";
	public static final String ARROWHEAD_DECORATOR = "arrowhead.decorator";
	
	protected CreateConnectionContext createContext;
	
	@Override
	public boolean canApplyTo(Object o) {
		return super.canApplyTo(o) && o instanceof DataAssociation;
	}

	@Override
	public ICreateConnectionFeature getCreateConnectionFeature(IFeatureProvider fp) {
		return new CreateDataAssociationFeature(fp);
	}

	@Override
	public IAddFeature getAddFeature(IFeatureProvider fp) {
		return new AddDataAssociationFeature(fp);
	}

	@Override
	public IUpdateFeature getUpdateFeature(IFeatureProvider fp) {
		return new UpdateDataAssociationFeature(fp);
	}
	
	@Override
	public IReconnectionFeature getReconnectionFeature(IFeatureProvider fp) {
		// TODO Auto-generated method stub
		return new ReconnectDataAssociationFeature(fp);
	}

	private boolean canConnect(BaseElement source, BaseElement target) {
		// Connection rules:
		// either the source or target must be an ItemAwareElement,
		// and the other must be either an Activity or a Catch or Throw Event
		// depending on whether it's the target or the source.
		if ((source instanceof Activity || source instanceof CatchEvent) && target instanceof ItemAwareElement) {
			if (!(target instanceof DataInput))
				return true;
		}
		if ((target instanceof Activity || target instanceof ThrowEvent) && source instanceof ItemAwareElement) {
			if (!(source instanceof DataOutput))
				return true;
		}
		return false;
	}

	
	private static ILabelProvider labelProvider = new ILabelProvider() {

		public void removeListener(ILabelProviderListener listener) {
		}

		public boolean isLabelProperty(Object element, String property) {
			return false;
		}

		public void dispose() {
		}

		public void addListener(ILabelProviderListener listener) {
		}

		public String getText(Object object) {
			ItemAwareElement element = (ItemAwareElement) object;
			if (element.getId()==null)
				return ModelUtil.getDisplayName(object);
			return "Map existing \"" + ModelUtil.getDisplayName(object) + "\"";
		}

		public Image getImage(Object element) {
			return null;
		}

	};

	public class CreateDataAssociationFeature extends AbstractCreateFlowFeature<DataAssociation, BaseElement, BaseElement> {

		public CreateDataAssociationFeature(IFeatureProvider fp) {
			super(fp, "Data Association", "Create "+"Data Association");
		}

		@Override
		public boolean isAvailable(IContext context) {
			return true;
		}

		@Override
		public boolean canStartConnection(ICreateConnectionContext context) {
			BaseElement source = getSourceBo(context);
			if (source instanceof Activity || source instanceof CatchEvent)
				return true;
			if (source instanceof ItemAwareElement) {
				if (source instanceof DataInput)
					return false;
				return true;
			}
			return false;
		}

		@Override
		public boolean canCreate(ICreateConnectionContext context) {
			if (super.canCreate(context)) {
				BaseElement source = getSourceBo(context);
				BaseElement target = getTargetBo(context);
				return canConnect(source, target);
			}
			return false;
		}

		@Override
		public Connection create(ICreateConnectionContext context) {
			// save the CreateContext because we'll need it in AddFeature
			createContext = (CreateConnectionContext)context;
			Anchor sourceAnchor = createContext.getSourceAnchor();
			Anchor targetAnchor = createContext.getTargetAnchor();
			PictogramElement source = createContext.getSourcePictogramElement();
			PictogramElement target = createContext.getTargetPictogramElement();
			
			if (sourceAnchor==null && source instanceof FreeFormConnection) {
				Shape connectionPointShape = AnchorUtil.createConnectionPoint(getFeatureProvider(),
						(FreeFormConnection)source,
						Graphiti.getPeLayoutService().getConnectionMidpoint((FreeFormConnection)source, 0.5));
				sourceAnchor = AnchorUtil.getConnectionPointAnchor(connectionPointShape);
				createContext.setSourceAnchor(sourceAnchor);
			}
			if (targetAnchor==null && target instanceof FreeFormConnection) {
				Shape connectionPointShape = AnchorUtil.createConnectionPoint(getFeatureProvider(),
						(FreeFormConnection)target,
						Graphiti.getPeLayoutService().getConnectionMidpoint((FreeFormConnection)target, 0.5));
				targetAnchor = AnchorUtil.getConnectionPointAnchor(connectionPointShape);
				createContext.setTargetAnchor(targetAnchor);
			}

			return super.create(context);
		}

		@Override
		protected String getStencilImageId() {
			return ImageProvider.IMG_16_DATA_ASSOCIATION;
		}

		@Override
		protected Class<BaseElement> getSourceClass() {
			return BaseElement.class;
		}

		@Override
		protected Class<BaseElement> getTargetClass() {
			return BaseElement.class;
		}

		@Override
		protected BaseElement getSourceBo(ICreateConnectionContext context) {
			Anchor anchor = context.getSourceAnchor();
			if (anchor != null && anchor.getParent() instanceof Shape) {
				Shape shape = (Shape) anchor.getParent();
				Connection conn = AnchorUtil.getConnectionPointOwner(shape);
				if (conn!=null) {
					return BusinessObjectUtil.getFirstElementOfType(conn, getTargetClass());
				}
				return BusinessObjectUtil.getFirstElementOfType(shape, getTargetClass());
			}
			return null;
		}

		@Override
		protected BaseElement getTargetBo(ICreateConnectionContext context) {
			Anchor anchor = context.getTargetAnchor();
			if (anchor != null && anchor.getParent() instanceof Shape) {
				Shape shape = (Shape) anchor.getParent();
				Connection conn = AnchorUtil.getConnectionPointOwner(shape);
				if (conn!=null) {
					return BusinessObjectUtil.getFirstElementOfType(conn, getTargetClass());
				}
				return BusinessObjectUtil.getFirstElementOfType(shape, getTargetClass());
			}
			return null;
		}

		/* (non-Javadoc)
		 * @see org.eclipse.bpmn2.modeler.core.features.AbstractBpmn2CreateConnectionFeature#getBusinessObjectClass()
		 */
		@Override
		public EClass getBusinessObjectClass() {
			return Bpmn2Package.eINSTANCE.getDataAssociation();
		}

		private DataInputAssociation selectInput(BaseElement target, List<DataInput> dataInputs, List<DataInputAssociation> dataInputAssociations, InputSet inputSet) {
			EObject object = null;
			EStructuralFeature objectFeature = null;
			EStructuralFeature targetFeature = null;
			if (target instanceof Activity) {
				object = ((Activity)target).getIoSpecification();
				objectFeature = Bpmn2Package.eINSTANCE.getInputOutputSpecification_DataInputs();
				targetFeature = Bpmn2Package.eINSTANCE.getActivity_DataInputAssociations();
			}
			else if (target instanceof ThrowEvent) {
				object = target;
				objectFeature = Bpmn2Package.eINSTANCE.getThrowEvent_DataInputs();
				targetFeature = Bpmn2Package.eINSTANCE.getThrowEvent_DataInputAssociation();
			}

			// allow user to select a dataInput:
			// create a throw away object as a placeholder in our popup list
			DataInput dataInput = Bpmn2Factory.eINSTANCE.createDataInput();
			dataInput.setName("Create new input parameter for "+ModelUtil.getDisplayName(target));
			DataInput result = dataInput;
			// build the popup list
			List<DataInput> list = new ArrayList<DataInput>();
			list.add(dataInput);
			list.addAll(dataInputs);
			if (list.size()>1) {
				PopupMenu popupMenu = new PopupMenu(list, labelProvider);
				boolean b = popupMenu.show(Display.getCurrent().getActiveShell());
				if (b) {
					result = (DataInput) popupMenu.getResult();
				}
			}

			DataInputAssociation dataInputAssoc = null;
			if (result == dataInput) {
				// create the new one
				dataInput = Bpmn2ModelerFactory.createFeature(object, objectFeature, DataInput.class);
				dataInputs.add(dataInput);
				inputSet.getDataInputRefs().add(dataInput);
				dataInputAssoc = (DataInputAssociation) Bpmn2ModelerFactory.createFeature(target, targetFeature);
				dataInputAssoc.setTargetRef(dataInput);
			} else {
				// select an existing one
				dataInput = result;
				// find the DataInputAssociation for this DataInput
				for (DataInputAssociation d : dataInputAssociations) {
					if (d.getTargetRef() == dataInput) {
						dataInputAssoc = d;
						break;
					}
				}
				if (dataInputAssoc==null) {
					// none found, create a new one
					dataInputAssoc = (DataInputAssociation) Bpmn2ModelerFactory.createFeature(target, targetFeature);
					dataInputAssoc.setTargetRef(dataInput);
				}
			}
			return dataInputAssoc;
		}

		private DataOutputAssociation selectOutput(BaseElement source, List<DataOutput> dataOutputs, List<DataOutputAssociation> dataOutputAssociations, OutputSet outputSet) {
			EObject object = null;
			EStructuralFeature objectFeature = null;
			EStructuralFeature sourceFeature = null;
			if (source instanceof Activity) {
				object = ((Activity)source).getIoSpecification();
				objectFeature = Bpmn2Package.eINSTANCE.getInputOutputSpecification_DataOutputs();
				sourceFeature = Bpmn2Package.eINSTANCE.getActivity_DataOutputAssociations();
			}
			else if (source instanceof CatchEvent) {
				object = source;
				objectFeature = Bpmn2Package.eINSTANCE.getCatchEvent_DataOutputs();
				sourceFeature = Bpmn2Package.eINSTANCE.getCatchEvent_DataOutputAssociation();
			}

			// allow user to select a dataOutput:
			// create a throw away object as a placeholder in our popup list
			DataOutput dataOutput = Bpmn2Factory.eINSTANCE.createDataOutput();
			dataOutput.setName("Create new output parameter for "+ModelUtil.getDisplayName(source));
			DataOutput result = dataOutput;
			// build the popup list
			List<DataOutput> list = new ArrayList<DataOutput>();
			list.add(dataOutput);
			list.addAll(dataOutputs);
			if (list.size()>1) {
				PopupMenu popupMenu = new PopupMenu(list, labelProvider);
				boolean b = popupMenu.show(Display.getCurrent().getActiveShell());
				if (b) {
					result = (DataOutput) popupMenu.getResult();
				}
			}

			DataOutputAssociation dataOutputAssoc = null;
			if (result == dataOutput) {
				// create the new one
				dataOutput = Bpmn2ModelerFactory.createFeature(object, objectFeature, DataOutput.class);
				dataOutputs.add(dataOutput);
				outputSet.getDataOutputRefs().add(dataOutput);
				dataOutputAssoc = (DataOutputAssociation) Bpmn2ModelerFactory.createFeature(source, sourceFeature);
				dataOutputAssoc.getSourceRef().add(dataOutput);
			} else {
				// select an existing one
				dataOutput = result;
				// find the DataOutputAssociation for this DataOutput
				for (DataOutputAssociation d : dataOutputAssociations) {
					if (d.getTargetRef() == dataOutput) {
						dataOutputAssoc = d;
						break;
					}
				}
				if (dataOutputAssoc==null) {
					// none found, create a new one
					dataOutputAssoc = (DataOutputAssociation) Bpmn2ModelerFactory.createFeature(source, sourceFeature);
					dataOutputAssoc.getSourceRef().add(dataOutput);
				}
			}
			return dataOutputAssoc;
		}
		
		@Override
		public DataAssociation createBusinessObject(ICreateConnectionContext context) {
			// Instead of creating a new object, we will try to discover a DataAssociation
			// (input or output) already defined within the context of the source or
			// target object. This will be a DataInputAssociation or DataOutputAssociation
			// of an Activity or Throw/Catch Event. If none exists, we'll create a new one
			// as well as the surrounding elements (ioSpecification, input/output sets and
			// data input/output elements) as needed.
			DataAssociation dataAssoc = null;
			BaseElement source = getSourceBo(context);
			BaseElement target = getTargetBo(context);

			if (target instanceof ItemAwareElement) {
				// Target is the DataObject.
				DataOutputAssociation dataOutputAssoc = null;
				if (source instanceof Activity) {
					// if the source is an Activity, create an ioSpecification if it doesn't have one yet
					Activity activity = (Activity) source;
					InputOutputSpecification ioSpec = activity.getIoSpecification();
					if (ioSpec==null) {
						ioSpec = Bpmn2ModelerFactory.createFeature(activity, "ioSpecification", InputOutputSpecification.class);
					}
					OutputSet outputSet = null;
					if (ioSpec.getOutputSets().size()==0) {
						outputSet = Bpmn2ModelerFactory.create(OutputSet.class);
						ioSpec.getOutputSets().add(outputSet);
					}
					else {
						// add to first output set we find
						// TODO: support output set selection if there are more than one
						outputSet = ioSpec.getOutputSets().get(0);
					}
					dataOutputAssoc = selectOutput(source, ioSpec.getDataOutputs(), activity.getDataOutputAssociations(), outputSet);
				}
				else if (source instanceof CatchEvent) {
					// if the source is an Event, create an output set if it doesn't have one yet
					CatchEvent event = (CatchEvent)source;
					OutputSet outputSet = event.getOutputSet();
					if (outputSet==null) {
						outputSet = Bpmn2ModelerFactory.create(OutputSet.class);
						event.setOutputSet(outputSet);
					}
					dataOutputAssoc = selectOutput(source, event.getDataOutputs(), event.getDataOutputAssociation(), outputSet);
				}
				dataOutputAssoc.setTargetRef((ItemAwareElement) target);

				dataAssoc = dataOutputAssoc;
			}
			else if (source instanceof ItemAwareElement)
			{
				// Source is the DataObject.
				DataInputAssociation dataInputAssoc = null;
				if (target instanceof Activity) {
					// if the target is an Activity, create an ioSpecification if it doesn't have one yet
					Activity activity = (Activity) target;
					InputOutputSpecification ioSpec = activity.getIoSpecification();
					if (ioSpec==null) {
						ioSpec = (InputOutputSpecification) Bpmn2ModelerFactory.createFeature(activity, "ioSpecification");
					}
					InputSet inputSet = null;
					if (ioSpec.getInputSets().size()==0) {
						inputSet = Bpmn2ModelerFactory.create(InputSet.class);
						ioSpec.getInputSets().add(inputSet);
					}
					else {
						// add to first input set we find
						// TODO: support input set selection if there are more than one
						inputSet = ioSpec.getInputSets().get(0);
					}
					dataInputAssoc = selectInput(target, ioSpec.getDataInputs(), activity.getDataInputAssociations(), inputSet);
				}
				else if (target instanceof ThrowEvent) {
					// if the target is an Event, create an input set if it doesn't have one yet
					ThrowEvent event = (ThrowEvent)target;
					InputSet inputSet = event.getInputSet();
					if (inputSet==null) {
						inputSet = Bpmn2ModelerFactory.create(InputSet.class);
						event.setInputSet(inputSet);
					}
					dataInputAssoc = selectInput(target, event.getDataInputs(), event.getDataInputAssociation(), inputSet);
				}
				dataInputAssoc.getSourceRef().clear();
				dataInputAssoc.getSourceRef().add((ItemAwareElement) source);

				dataAssoc = dataInputAssoc;
			}
			putBusinessObject(context, dataAssoc);
			return dataAssoc;
		}
	}

	public class AddDataAssociationFeature extends AbstractAddFlowFeature<DataAssociation> {
		public AddDataAssociationFeature(IFeatureProvider fp) {
			super(fp);
		}

		@Override
		public PictogramElement add(IAddContext context) {
			AddConnectionContext addConContext = (AddConnectionContext)context;
			Anchor sourceAnchor = addConContext.getSourceAnchor();
			Anchor targetAnchor = addConContext.getTargetAnchor();
			PictogramElement source = sourceAnchor==null ? null : sourceAnchor.getParent();
			PictogramElement target = targetAnchor==null ? null : targetAnchor.getParent();
			boolean anchorChanged = false;
			
			if (createContext!=null) {
				if (source==null) {
					source = createContext.getSourcePictogramElement();
					sourceAnchor = createContext.getSourceAnchor();
				}
				if (target==null) {
					target = createContext.getTargetPictogramElement();
					targetAnchor = createContext.getTargetAnchor();
				}
			}
			
			if (sourceAnchor==null && source instanceof FreeFormConnection) {
				Shape connectionPointShape = AnchorUtil.createConnectionPoint(getFeatureProvider(),
						(FreeFormConnection)source,
						Graphiti.getPeLayoutService().getConnectionMidpoint((FreeFormConnection)source, 0.5));
				sourceAnchor = AnchorUtil.getConnectionPointAnchor(connectionPointShape);
				anchorChanged = true;
			}
			if (targetAnchor==null && target instanceof FreeFormConnection) {
				Shape connectionPointShape = AnchorUtil.createConnectionPoint(getFeatureProvider(),
						(FreeFormConnection)target,
						Graphiti.getPeLayoutService().getConnectionMidpoint((FreeFormConnection)target, 0.5));
				targetAnchor = AnchorUtil.getConnectionPointAnchor(connectionPointShape);
				anchorChanged = true;
			}
			
			// this is silly! why are there no setters for sourceAnchor and targetAnchor in AddConnectionContext???
			if (anchorChanged) {
				AddConnectionContext newContext = new AddConnectionContext(sourceAnchor, targetAnchor);
				newContext.setSize(addConContext.getHeight(), addConContext.getWidth());
				newContext.setLocation(addConContext.getX(), addConContext.getY());
				newContext.setNewObject(getBusinessObject(addConContext));
				newContext.setTargetConnection(addConContext.getTargetConnection());
				newContext.setTargetConnectionDecorator(addConContext.getTargetConnectionDecorator());
				newContext.setTargetContainer(addConContext.getTargetContainer());
				
				context = newContext;
			}
			// we're done with this
			createContext = null;
			
			return super.add(context);
		}

		@Override
		protected Polyline createConnectionLine(Connection connection) {
			Polyline connectionLine = super.createConnectionLine(connection);
			connectionLine.setLineWidth(2);
			connectionLine.setLineStyle(LineStyle.DOT);
			return connectionLine;
		}

		@Override
		protected void decorateConnection(IAddConnectionContext context, Connection connection, DataAssociation businessObject) {
			setAssociationDirection(connection, businessObject);
		}

		@Override
		protected Class<? extends BaseElement> getBoClass() {
			return DataAssociation.class;
		}

		@Override
		public int getHeight() {
			return 0;
		}

		@Override
		public int getWidth() {
			return 0;
		}
	}

	
	private static String getDirection(DataAssociation businessObject) {
		return (businessObject instanceof DataInputAssociation) ? "input" : "output";
	}
	
	private static void setAssociationDirection(Connection connection, DataAssociation businessObject) {
		IPeService peService = Graphiti.getPeService();
		IGaService gaService = Graphiti.getGaService();
		String newDirection = getDirection(businessObject);
		String oldDirection = peService.getPropertyValue(connection, ASSOCIATION_DIRECTION);
		if (oldDirection==null || oldDirection.isEmpty())
			oldDirection = "";

		if (!oldDirection.equals(newDirection)) {
			ConnectionDecorator sourceDecorator = null;
			ConnectionDecorator targetDecorator = null;
			for (ConnectionDecorator d : connection.getConnectionDecorators()) {
				String s = peService.getPropertyValue(d, ARROWHEAD_DECORATOR);
				if (s!=null) {
					if (s.equals("source"))
						sourceDecorator = d;
					else if (s.equals("target"))
						targetDecorator = d;
				}
			}
			
			final int w = 7;
			final int l = 13;
			if (sourceDecorator!=null) {
				connection.getConnectionDecorators().remove(sourceDecorator);				
			}

			if (targetDecorator==null) {
				targetDecorator = peService.createConnectionDecorator(connection, false, 1.0, true);
				Polyline arrowhead = gaService.createPolyline(targetDecorator, new int[] { -l, w, 0, 0, -l, -w });
				StyleUtil.applyStyle(arrowhead, businessObject);
				peService.setPropertyValue(targetDecorator, ARROWHEAD_DECORATOR, "target");
			}
		
			// update the property value in the Connection PictogramElement
			peService.setPropertyValue(connection, ASSOCIATION_DIRECTION, newDirection);
		}

	}
	
	public static class UpdateDataAssociationFeature extends AbstractUpdateFeature {

		public UpdateDataAssociationFeature(IFeatureProvider fp) {
			super(fp);
		}

		@Override
		public boolean canUpdate(IUpdateContext context) {
			if (context.getPictogramElement() instanceof Connection) {
				return BusinessObjectUtil.getFirstElementOfType(
						context.getPictogramElement(), DataAssociation.class) != null;
			}
			return false;
		}

		@Override
		public IReason updateNeeded(IUpdateContext context) {
			IPeService peService = Graphiti.getPeService();
			Connection connection = (Connection) context.getPictogramElement();
			DataAssociation businessObject = BusinessObjectUtil.getFirstElementOfType(context.getPictogramElement(),
					DataAssociation.class);
			String newDirection = getDirection(businessObject);
			String oldDirection = peService.getPropertyValue(connection, ASSOCIATION_DIRECTION);
			if (oldDirection==null || oldDirection.isEmpty())
				oldDirection = "";

			if (!oldDirection.equals(newDirection)) {
				return Reason.createTrueReason();
			}
			return Reason.createFalseReason();
		}

		@Override
		public boolean update(IUpdateContext context) {
			Connection connection = (Connection) context.getPictogramElement();
			DataAssociation businessObject = BusinessObjectUtil.getFirstElementOfType(context.getPictogramElement(),
					DataAssociation.class);
			setAssociationDirection(connection, businessObject);
			return true;
		}
	}

	public static class ReconnectDataAssociationFeature extends AbstractReconnectFlowFeature {

		public ReconnectDataAssociationFeature(IFeatureProvider fp) {
			super(fp);
		}

		@Override
		public boolean canReconnect(IReconnectionContext context) {
			DataAssociation businessObject = BusinessObjectUtil.getFirstElementOfType(context.getConnection(),
					DataAssociation.class);
			BaseElement targetElement = BusinessObjectUtil.getFirstElementOfType(context.getTargetPictogramElement(), BaseElement.class);
			if (targetElement instanceof DataAssociation)
				return false;
			PictogramElement targetPictogramElement = context.getTargetPictogramElement();
			if (targetPictogramElement instanceof FreeFormConnection) {
				return true;
			}
			return super.canReconnect(context);
		}

		@Override
		protected Class<? extends EObject> getTargetClass() {
			return BaseElement.class;
		}

		@Override
		protected Class<? extends EObject> getSourceClass() {
			return BaseElement.class;
		}

		@Override
		public void preReconnect(IReconnectionContext context) {
			PictogramElement targetPictogramElement = context.getTargetPictogramElement();
			if (targetPictogramElement instanceof FreeFormConnection) {
				Shape connectionPointShape = AnchorUtil.createConnectionPoint(
						getFeatureProvider(),
						(FreeFormConnection)targetPictogramElement,
						context.getTargetLocation());
				
				if (context instanceof ReconnectionContext) {
					ReconnectionContext rc = (ReconnectionContext) context;
					rc.setNewAnchor(AnchorUtil.getConnectionPointAnchor(connectionPointShape));
					rc.setTargetPictogramElement(connectionPointShape);
				}
			}
			super.preReconnect(context);
		}

		@Override
		public void postReconnect(IReconnectionContext context) {
			Anchor oldAnchor = context.getOldAnchor();
			AnchorContainer oldAnchorContainer = oldAnchor.getParent();
			AnchorUtil.deleteConnectionPointIfPossible(getFeatureProvider(), (Shape) oldAnchorContainer);
			super.postReconnect(context);
		}
	} 
	
}