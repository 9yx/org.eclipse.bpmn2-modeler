/*******************************************************************************
 * Copyright (c) 2011 Red Hat, Inc.
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

package org.eclipse.bpmn2.modeler.ui.property.editors;

import org.eclipse.bpmn2.modeler.ui.property.AbstractBpmn2PropertiesComposite;
import org.eclipse.bpmn2.modeler.ui.property.dialogs.SchemaSelectionDialog;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.impl.DynamicEObjectImpl;
import org.eclipse.emf.ecore.xml.type.impl.AnyTypeImpl;
import org.eclipse.emf.transaction.RecordingCommand;
import org.eclipse.emf.transaction.TransactionalEditingDomain;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

/**
 * @author Bob Brodt
 *
 */
public abstract class TextAndButtonObjectEditor extends ObjectEditor {

	protected Label text;
	protected Button button;

	/**
	 * @param parent
	 * @param object
	 * @param feature
	 */
	public TextAndButtonObjectEditor(AbstractBpmn2PropertiesComposite parent, EObject object, EStructuralFeature feature) {
		super(parent, object, feature);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.bpmn2.modeler.ui.property.editors.ObjectEditor#createControl(org.eclipse.swt.widgets.Composite, java.lang.String, int)
	 */
	@Override
	public Control createControl(Composite composite, String label, int style) {
		createLabel(composite, label);

		text = getToolkit().createLabel(composite, "", SWT.BORDER );
		text.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		text.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));

		button = getToolkit().createButton(composite, "Edit...", SWT.PUSH);
		button.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));

		updateText( getTextValue(null) );

		SelectionAdapter editListener = new SelectionAdapter() {

			@SuppressWarnings("unchecked")
			@Override
			public void widgetSelected(SelectionEvent e) {
				buttonClicked();
			}
		};
		button.addSelectionListener(editListener);

		return text;
	}

	/**
	 * The implementation must override this to handle the "Edit..." button click.
	 */
	protected abstract void buttonClicked(); 

	/**
	 * Update the value of the EObject's feature being edited.
	 * 
	 * As a side-effect, if the new value was set successfully,
	 * the text field is also updated with the new string representation
	 * for the object value.
	 *  
	 * @param value
	 * @return true if the object was successfully updated, false if error
	 */
	protected boolean updateObject(final Object value) {
		if (value != object.eGet(feature)) {
			TransactionalEditingDomain editingDomain = getDiagramEditor().getEditingDomain();
			editingDomain.getCommandStack().execute(new RecordingCommand(editingDomain) {
				@Override
				protected void doExecute() {
					object.eSet(feature, value);
				}
			});
			if (getDiagramEditor().getDiagnostics()!=null) {
				getDiagramEditor().showErrorMessage(getDiagramEditor().getDiagnostics().getMessage());
			}
			else {
				getDiagramEditor().showErrorMessage(null);
				updateText(value);
				return true;
			}
		}
		return false;
	}

	/**
	 * Update the read-only text field with the give value
	 * 
	 * @param value - new value for the text field
	 */
	protected void updateText(Object value) {
		text.setText( getTextValue(value) );
	}
	
	/**
	 * Returns the string representation of the given value used for
	 * display in the text field. The default implementation correctly
	 * handles structureRef values (proxy URIs from a DynamicEObject)
	 * and provides reasonable behavior for EObject values.
	 * 
	 * @param value - new object value. If null is passed in, the implementation
	 * should substitute the original value of the EObject's feature.
	 * 
	 * @return string representation of the EObject feature's value.
	 */
	protected String getTextValue(Object value) {
		if (value==null)
			value = object.eGet(feature);
		
		if (value instanceof DynamicEObjectImpl) {
			DynamicEObjectImpl de = (DynamicEObjectImpl)value;
			URI uri = de.eProxyURI();
			return uri.toString();
		}
		else if (value instanceof EObject) {
			EObject object = (EObject)value;
			EStructuralFeature feature = object.eClass().getEStructuralFeature("name");
			String name = object.eClass().getName();
			if (feature!=null && object.eIsSet(feature)) {
				return name+" "+object.eGet(feature);
			}
			else
				return name;
		}
		else if (value==null)
			return "";
		return value.toString();
	}
}