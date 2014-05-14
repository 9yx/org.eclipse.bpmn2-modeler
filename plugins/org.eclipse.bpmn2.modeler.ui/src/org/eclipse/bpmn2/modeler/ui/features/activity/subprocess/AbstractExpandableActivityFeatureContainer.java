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
package org.eclipse.bpmn2.modeler.ui.features.activity.subprocess;

import org.eclipse.bpmn2.BaseElement;
import org.eclipse.bpmn2.CallActivity;
import org.eclipse.bpmn2.CallChoreography;
import org.eclipse.bpmn2.FlowElementsContainer;
import org.eclipse.bpmn2.di.BPMNDiagram;
import org.eclipse.bpmn2.di.BPMNShape;
import org.eclipse.bpmn2.modeler.core.di.DIUtils;
import org.eclipse.bpmn2.modeler.core.features.MultiUpdateFeature;
import org.eclipse.bpmn2.modeler.core.features.activity.task.DirectEditTaskFeature;
import org.eclipse.bpmn2.modeler.ui.features.activity.AbstractActivityFeatureContainer;
import org.eclipse.graphiti.features.IDeleteFeature;
import org.eclipse.graphiti.features.IDirectEditingFeature;
import org.eclipse.graphiti.features.IFeatureProvider;
import org.eclipse.graphiti.features.ILayoutFeature;
import org.eclipse.graphiti.features.IResizeShapeFeature;
import org.eclipse.graphiti.features.IUpdateFeature;
import org.eclipse.graphiti.features.custom.ICustomFeature;

public abstract class AbstractExpandableActivityFeatureContainer extends AbstractActivityFeatureContainer {

	@Override
	public IDirectEditingFeature getDirectEditingFeature(IFeatureProvider fp) {
		return new DirectEditTaskFeature(fp);
	}

	@Override
	public IUpdateFeature getUpdateFeature(IFeatureProvider fp) {
		MultiUpdateFeature multiUpdate = new MultiUpdateFeature(fp);
		multiUpdate.addFeature(super.getUpdateFeature(fp));
		multiUpdate.addFeature(new UpdateExpandableActivityFeature(fp));
		return multiUpdate;
	}

	@Override
	public ILayoutFeature getLayoutFeature(IFeatureProvider fp) {
		return new LayoutExpandableActivityFeature(fp);
	}

	@Override
	public IResizeShapeFeature getResizeFeature(IFeatureProvider fp) {
		return new ResizeExpandableActivityFeature(fp);
	}

	@Override
	public IDeleteFeature getDeleteFeature(IFeatureProvider fp) {
		return new DeleteExpandableActivityFeature(fp);
	}

	@Override
	public ICustomFeature[] getCustomFeatures(IFeatureProvider fp) {
		ICustomFeature[] superFeatures = super.getCustomFeatures(fp);
		ICustomFeature[] thisFeatures = new ICustomFeature[4 + superFeatures.length];
		thisFeatures[0] = new ExpandFlowNodeFeature(fp);
		thisFeatures[1] = new CollapseFlowNodeFeature(fp);
		thisFeatures[2] = new PushdownFeature(fp);
		thisFeatures[3] = new PullupFeature(fp);
		for (int i=0; i<superFeatures.length; ++i)
			thisFeatures[4+i] = superFeatures[i];
		return thisFeatures;
	}
	
	public static boolean isExpandableElement(Object object) {
		return object instanceof FlowElementsContainer
				|| object instanceof CallActivity
				|| object instanceof CallChoreography;
	}
	
	public static boolean isElementExpanded(Object object) {
		if (isExpandableElement(object)) {
			BaseElement be = (BaseElement)object;
			// if the BaseElement has its own BPMNDiagram page it should be considered
			// to be collapsed and should be represented as such.
			// TODO: this condition should be removed once we implement Link events as
			// "off page" connectors.
			BPMNDiagram bpmnDiagram = DIUtils.findBPMNDiagram(be);
			// otherwise check the "isExpanded" state of the BPMNShape element.
			BPMNShape bpmnShape = DIUtils.findBPMNShape(be);
			if (bpmnShape!=null && bpmnShape.isIsExpanded() && bpmnDiagram==null)
				return true;
		}
		return false;
	}
}