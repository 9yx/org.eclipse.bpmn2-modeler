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
 * @author Innar Made
 ******************************************************************************/
package org.eclipse.bpmn2.modeler.ui.editor;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

import org.eclipse.bpmn2.BaseElement;
import org.eclipse.bpmn2.di.BPMNDiagram;
import org.eclipse.bpmn2.modeler.core.Bpmn2TabbedPropertySheetPage;
import org.eclipse.bpmn2.modeler.core.ModelHandler;
import org.eclipse.bpmn2.modeler.core.ModelHandlerLocator;
import org.eclipse.bpmn2.modeler.core.ProxyURIConverterImplExtension;
import org.eclipse.bpmn2.modeler.core.di.DIImport;
import org.eclipse.bpmn2.modeler.core.merrimac.clad.DefaultDetailComposite;
import org.eclipse.bpmn2.modeler.core.merrimac.clad.DefaultDialogComposite;
import org.eclipse.bpmn2.modeler.core.merrimac.clad.DefaultListComposite;
import org.eclipse.bpmn2.modeler.core.merrimac.clad.PropertiesCompositeFactory;
import org.eclipse.bpmn2.modeler.core.model.Bpmn2ModelerResourceImpl;
import org.eclipse.bpmn2.modeler.core.preferences.Bpmn2Preferences;
import org.eclipse.bpmn2.modeler.core.runtime.TargetRuntime;
import org.eclipse.bpmn2.modeler.core.utils.BusinessObjectUtil;
import org.eclipse.bpmn2.modeler.core.utils.DiagramEditorAdapter;
import org.eclipse.bpmn2.modeler.core.utils.ErrorUtils;
import org.eclipse.bpmn2.modeler.core.utils.ModelUtil;
import org.eclipse.bpmn2.modeler.core.utils.ModelUtil.Bpmn2DiagramType;
import org.eclipse.bpmn2.modeler.core.utils.StyleUtil;
import org.eclipse.bpmn2.modeler.core.validation.BPMN2ProjectValidator;
import org.eclipse.bpmn2.modeler.core.validation.BPMN2ValidationStatusLoader;
import org.eclipse.bpmn2.modeler.ui.Activator;
import org.eclipse.bpmn2.modeler.ui.IFileChangeListener;
import org.eclipse.bpmn2.modeler.ui.wizards.BPMN2DiagramCreator;
import org.eclipse.bpmn2.modeler.ui.wizards.Bpmn2DiagramEditorInput;
import org.eclipse.bpmn2.util.Bpmn2ResourceImpl;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.emf.common.command.BasicCommandStack;
import org.eclipse.emf.common.notify.Adapter;
import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.common.notify.Notifier;
import org.eclipse.emf.common.util.BasicDiagnostic;
import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EValidator;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.transaction.RecordingCommand;
import org.eclipse.emf.transaction.TransactionalEditingDomain.Lifecycle;
import org.eclipse.emf.transaction.impl.TransactionalEditingDomainImpl;
import org.eclipse.emf.validation.model.EvaluationMode;
import org.eclipse.emf.validation.service.IBatchValidator;
import org.eclipse.emf.validation.service.ModelValidationService;
import org.eclipse.emf.validation.service.ValidationEvent;
import org.eclipse.emf.workspace.util.WorkspaceSynchronizer;
import org.eclipse.graphiti.features.IFeatureProvider;
import org.eclipse.graphiti.mm.algorithms.GraphicsAlgorithm;
import org.eclipse.graphiti.mm.pictograms.Diagram;
import org.eclipse.graphiti.mm.pictograms.PictogramElement;
import org.eclipse.graphiti.services.Graphiti;
import org.eclipse.graphiti.services.IPeService;
import org.eclipse.graphiti.ui.editor.DiagramEditor;
import org.eclipse.graphiti.ui.editor.DiagramEditorInput;
import org.eclipse.graphiti.ui.internal.editor.GFPaletteRoot;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IViewReference;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchListener;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.SaveAsDialog;
import org.eclipse.ui.ide.IGotoMarker;
import org.eclipse.ui.ide.ResourceUtil;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.part.IPage;
import org.eclipse.ui.progress.IProgressService;
import org.eclipse.ui.views.properties.IPropertySheetPage;
import org.eclipse.ui.views.properties.PropertySheet;
import org.eclipse.ui.views.properties.tabbed.ITabDescriptorProvider;
import org.eclipse.wst.validation.ValidationState;

/**
 * 
 */
@SuppressWarnings("restriction")
public class BPMN2Editor extends DiagramEditor implements IPropertyChangeListener, IGotoMarker {

	// Register the Default List and Detail Composites as fallback for rendering EObject
	static {
		PropertiesCompositeFactory.register(EObject.class, DefaultDetailComposite.class);
		PropertiesCompositeFactory.register(EObject.class, DefaultListComposite.class);
		PropertiesCompositeFactory.register(EObject.class, DefaultDialogComposite.class);
	}

	public static final String EDITOR_ID = "org.eclipse.bpmn2.modeler.ui.bpmn2editor";
	public static final String CONTRIBUTOR_ID = "org.eclipse.bpmn2.modeler.ui.PropertyContributor";

	private ModelHandler modelHandler;
	private URI modelUri;
	private URI diagramUri;

	private IFile modelFile;
	private IFile diagramFile;
	
	private IFileChangeListener fileChangeListener;
	private IWorkbenchListener workbenchListener;
	private IPartListener2 selectionListener;
    private IResourceChangeListener markerChangeListener;
	private boolean workbenchShutdown = false;
	private static BPMN2Editor activeEditor;
	private static ITabDescriptorProvider tabDescriptorProvider;

	private BPMN2EditingDomainListener editingDomainListener;
	
	private Bpmn2Preferences preferences;
	private TargetRuntime targetRuntime;

	protected DiagramEditorAdapter editorAdapter;
	
	public static BPMN2Editor getActiveEditor() {
		return activeEditor;
	}
	
	private void setActiveEditor(BPMN2Editor editor) {
		activeEditor = editor;
		if (activeEditor!=null) {
			Bpmn2Preferences.setActiveProject(activeEditor.modelFile.getProject());
			TargetRuntime.setCurrentRuntime( activeEditor.getTargetRuntime() );
		}
	}

	protected DiagramEditorAdapter getEditorAdapter() {
		return editorAdapter;
	}

	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		try {
			Bpmn2DiagramType diagramType = Bpmn2DiagramType.NONE;
			String targetNamespace = null;
			if (input instanceof IFileEditorInput) {
				modelFile = ((IFileEditorInput) input).getFile();
				loadPreferences(modelFile.getProject());

				input = createNewDiagramEditorInput(diagramType, targetNamespace);

			} else if (input instanceof DiagramEditorInput) {
				getModelPathFromInput((DiagramEditorInput) input);
				loadPreferences(modelFile.getProject());
				if (input instanceof Bpmn2DiagramEditorInput) {
					diagramType = ((Bpmn2DiagramEditorInput)input).getInitialDiagramType();
					targetNamespace = ((Bpmn2DiagramEditorInput)input).getTargetNamespace();
				}
				// This was incorrectly constructed input, we ditch the old one and make a new and clean one instead
				// This code path comes in from the New File Wizard
				input = createNewDiagramEditorInput(diagramType, targetNamespace);
			}
		} catch (CoreException e) {
			Activator.showErrorWithLogging(e);
		}
		
		// add a listener so we get notified if the workbench is shutting down.
		// in this case we don't want to delete the temp file!
		addWorkbenchListener();
		setActiveEditor(this);
		
		// allow the runtime extension to construct custom tasks and whatever else it needs
		// custom tasks should be added to the current target runtime's custom tasks list
		// where they will be picked up by the toolpalette refresh.
		getTargetRuntime().getRuntimeExtension().initialize();
		
		super.init(site, input);
		addSelectionListener();
		addFileChangeListener();
		addMarkerChangeListener();
	}

	public Bpmn2Preferences getPreferences() {
		if (preferences==null) {
			assert(modelFile!=null);
			IProject project = modelFile.getProject();
			loadPreferences(project);
		}
		return preferences;
	}
	
	private void loadPreferences(IProject project) {
		preferences = Bpmn2Preferences.getInstance(project);
		preferences.load();
		preferences.getGlobalPreferences().addPropertyChangeListener(this);
	}

	/**
	 * ID for tabbed property sheets.
	 * 
	 * @return the contributor id
	 */
	@Override
	public String getContributorId() {
		return CONTRIBUTOR_ID;
	}

	public TargetRuntime getTargetRuntime(ITabDescriptorProvider tdp) {
		tabDescriptorProvider = tdp;
		return getTargetRuntime();
	}
	
	public TargetRuntime getTargetRuntime() {
		if (targetRuntime==null)
			targetRuntime = getPreferences().getRuntime(modelFile);
		return targetRuntime;
	}
	
	private void getModelPathFromInput(DiagramEditorInput input) {
		URI uri = input.getDiagram().eResource().getURI();
		String uriString = uri.trimFragment().toPlatformString(true);
		modelFile = BPMN2DiagramCreator.getModelFile(new Path(uriString));
	}

	/**
	 * Beware, creates a new input and changes this editor!
	 */
	private Bpmn2DiagramEditorInput createNewDiagramEditorInput(Bpmn2DiagramType diagramType, String targetNamespace)
			throws CoreException {
		IPath fullPath = modelFile.getFullPath();
		modelUri = URI.createPlatformResourceURI(fullPath.toString(), true);

		IFolder folder = BPMN2DiagramCreator.getTempFolder(fullPath);
		diagramFile = BPMN2DiagramCreator.getTempFile(fullPath,folder);

		// Create new temporary diagram file
		BPMN2DiagramCreator creator = new BPMN2DiagramCreator();
		creator.setDiagramFile(diagramFile);

		Bpmn2DiagramEditorInput input = creator.createDiagram(diagramType,targetNamespace,this);
		diagramUri = creator.getUri();

		return input;
	}

	private void saveModelFile() {
		modelHandler.save();
		((BasicCommandStack) getEditingDomain().getCommandStack()).saveIsDone();
		firePropertyChange(IEditorPart.PROP_DIRTY);
	}

	@Override
	protected void setInput(IEditorInput input) {
		super.setInput(input);
		
		// Hook a transaction exception handler so we can get diagnostics about EMF validation errors.
		getEditingDomainListener();
		
		BasicCommandStack basicCommandStack = (BasicCommandStack) getEditingDomain().getCommandStack();

		if (input instanceof DiagramEditorInput) {
			ResourceSet resourceSet = getEditingDomain().getResourceSet();
			getTargetRuntime().setResourceSet(resourceSet);
			
			Bpmn2ResourceImpl bpmnResource = (Bpmn2ResourceImpl) resourceSet.createResource(modelUri,
					Bpmn2ModelerResourceImpl.BPMN2_CONTENT_TYPE_ID);

			resourceSet.setURIConverter(new ProxyURIConverterImplExtension());
			resourceSet.eAdapters().add(editorAdapter = new DiagramEditorAdapter(this));

			modelHandler = ModelHandlerLocator.createModelHandler(modelUri, bpmnResource);
			ModelHandlerLocator.put(diagramUri, modelHandler);

			try {
				if (modelFile.exists()) {
					bpmnResource.load(null);
				} else {
					saveModelFile();
				}
			} catch (IOException e) {
				Status status = new Status(IStatus.ERROR, Activator.PLUGIN_ID, e.getMessage(), e);
				ErrorUtils.showErrorWithLogging(status);
			}
			basicCommandStack.execute(new RecordingCommand(getEditingDomain()) {

				@Override
				protected void doExecute() {
					importDiagram();
				}
			});
		}
		basicCommandStack.saveIsDone();
		basicCommandStack.flush();
		loadMarkers();
	}
	
	private void importDiagram() {
		// make sure this guy is active, otherwise it's not selectable
		Diagram diagram = getDiagramTypeProvider().getDiagram();
		IFeatureProvider featureProvider = getDiagramTypeProvider().getFeatureProvider();
		diagram.setActive(true);
		Bpmn2DiagramEditorInput input = (Bpmn2DiagramEditorInput) getEditorInput();
		Bpmn2DiagramType diagramType = input.getInitialDiagramType();
		String targetNamespace = input.getTargetNamespace();

		if (diagramType != Bpmn2DiagramType.NONE) {
			BPMNDiagram bpmnDiagram = modelHandler.createDiagramType(diagramType, targetNamespace);
			featureProvider.link(diagram, bpmnDiagram);
			saveModelFile();
		}
		
		DIImport di = new DIImport(this);
		di.setModelHandler(modelHandler);

		di.generateFromDI();

		// this needs to happen AFTER the diagram has been imported because we need
		// to be able to determine the diagram type from the file's contents in order
		// to build the right tool palette for the target runtime and model enablements.
		GFPaletteRoot pr = (GFPaletteRoot)getPaletteRoot();
		pr.updatePaletteEntries();
	}

	@Override
	protected PictogramElement[] getPictogramElementsForSelection() {
		// filter out invisible elements when setting selection
		PictogramElement[] pictogramElements = super.getPictogramElementsForSelection();
		if (pictogramElements==null)
			return null;
		ArrayList<PictogramElement> visibleList = new ArrayList<PictogramElement>();
		for (PictogramElement pe : pictogramElements) {
			if (pe.isVisible())
				visibleList.add(pe);
		}
		return visibleList.toArray(new PictogramElement[visibleList.size()]);
	}
	
	private void addWorkbenchListener() {
		if (workbenchListener==null) {
			workbenchListener = new IWorkbenchListener() {
				@Override
				public boolean preShutdown(IWorkbench workbench, boolean forced) {
					workbenchShutdown = true;
					return true;
				}

				@Override
				public void postShutdown(IWorkbench workbench) {
				}

			};
			PlatformUI.getWorkbench().addWorkbenchListener(workbenchListener);
		}
	}
	
    @Override
    public void gotoMarker(IMarker marker) {
        final EObject target = getTargetObject(marker);
        if (target == null) {
            return;
        }
        final PictogramElement pe = getDiagramTypeProvider().getFeatureProvider().getPictogramElementForBusinessObject(
                target);
        if (pe == null) {
            return;
        }
        selectPictogramElements(new PictogramElement[] {pe });
    }

    private void loadMarkers() {
        // read in the markers
        BPMN2ValidationStatusLoader vsl = new BPMN2ValidationStatusLoader(this);

        try {
            vsl.load(Arrays.asList(getModelFile().findMarkers(
            		BPMN2ProjectValidator.BPMN2_MARKER_ID, true, IResource.DEPTH_ZERO)));
        } catch (CoreException e) {
            Activator.logStatus(e.getStatus());
        }
    }
    
    private EObject getTargetObject(IMarker marker) {
        final String uriString = marker.getAttribute(EValidator.URI_ATTRIBUTE, null);
        final URI uri = uriString == null ? null : URI.createURI(uriString);
        if (uri == null) {
            return null;
        }
        return getEditingDomain().getResourceSet().getEObject(uri, false);
    }

	private void removeWorkbenchListener()
	{
		if (workbenchListener!=null) {
			PlatformUI.getWorkbench().removeWorkbenchListener(workbenchListener);
			workbenchListener = null;
		}
	}
	
	private void addSelectionListener() {
		if (selectionListener == null) {
			IWorkbenchPage page = getSite().getPage();
			selectionListener = new IPartListener2() {
				public void partActivated(IWorkbenchPartReference partRef) {
				}

				@Override
				public void partBroughtToTop(IWorkbenchPartReference partRef) {
					IWorkbenchPart part = partRef.getPart(false);
					if (part instanceof BPMN2MultiPageEditor) {
						BPMN2MultiPageEditor mpe = (BPMN2MultiPageEditor)part;
						setActiveEditor(mpe.designEditor);
					}
				}

				@Override
				public void partClosed(IWorkbenchPartReference partRef) {
				}

				@Override
				public void partDeactivated(IWorkbenchPartReference partRef) {
				}

				@Override
				public void partOpened(IWorkbenchPartReference partRef) {
				}

				@Override
				public void partHidden(IWorkbenchPartReference partRef) {
				}

				@Override
				public void partVisible(IWorkbenchPartReference partRef) {
				}

				@Override
				public void partInputChanged(IWorkbenchPartReference partRef) {
				}
			};
			page.addPartListener(selectionListener);
		}
	}

	private void removeSelectionListener()
	{
		if (selectionListener!=null) {
			getSite().getPage().removePartListener(selectionListener);
			selectionListener = null;
		}
	}

	private void addMarkerChangeListener() {
		if (markerChangeListener==null) {
			markerChangeListener = new BPMN2MarkerChangeListener(this);
	        modelFile.getWorkspace().addResourceChangeListener(markerChangeListener, IResourceChangeEvent.POST_BUILD);
		}
	}
	
	private void removeMarkerChangeListener() {
		if (markerChangeListener!=null) {
	        modelFile.getWorkspace().removeResourceChangeListener(markerChangeListener);
			markerChangeListener = null;
		}
	}
	
	public void setEditorTitle(final String title) {
		Display display = getSite().getShell().getDisplay();
		display.asyncExec(new Runnable() {
			public void run() {
				setPartName(title);
			}
		});
	}
	
	private void addFileChangeListener() {
		if (fileChangeListener==null) {
			fileChangeListener = new IFileChangeListener() {
				public void deleted(IPath filePath) {
					// close the editor if either the dummy diagramfile (in the .bpmn2 folder)
					// or the model file is deleted
					if (modelFile.getFullPath().equals(filePath) || diagramFile.getFullPath().equals(filePath)) {
						// Close the editor.
						closeEditor();
					}
				}
				public void moved(IPath oldFilePath, IPath newFilePath) {
					// handle file move/rename after the fact (i.e. newFile now exists, old file does not)
					if (modelFile.getFullPath().equals(oldFilePath) || diagramFile.getFullPath().equals(oldFilePath)) {
						reopenEditor(newFilePath);
					}
				}
			};
			Activator.getDefault().getResourceChangeListener().addListener(fileChangeListener);
		}
	}

	private void removeFileChangeListener() {
		if (fileChangeListener!=null) {
			Activator.getDefault().getResourceChangeListener().removeListener(fileChangeListener);
			fileChangeListener = null;
		}
	}
	
	public BPMN2EditingDomainListener getEditingDomainListener() {
		if (editingDomainListener==null) {
			TransactionalEditingDomainImpl editingDomain = (TransactionalEditingDomainImpl)getEditingDomain();
			if (editingDomain==null) {
				return null;
			}
			editingDomainListener = new BPMN2EditingDomainListener(this);

			Lifecycle domainLifeCycle = (Lifecycle) editingDomain.getAdapter(Lifecycle.class);
			domainLifeCycle.addTransactionalEditingDomainListener(editingDomainListener);
		}
		return editingDomainListener;
	}
	
	public BasicDiagnostic getDiagnostics() {
		return getEditingDomainListener().getDiagnostics();
	}
	
	@Override
	public Object getAdapter(Class required) {
		if (required==ITabDescriptorProvider.class) {
			if (tabDescriptorProvider==null) {
				IWorkbenchPage page = getEditorSite().getPage();
				String viewID = "org.eclipse.ui.views.PropertySheet";
				try {
					page.showView(viewID, null, IWorkbenchPage.VIEW_CREATE);
					page.showView(viewID, null,  IWorkbenchPage.VIEW_ACTIVATE);
				}
				catch (Exception e) {}
			}
			return tabDescriptorProvider;
		}
		if (required==TargetRuntime.class)
			return getTargetRuntime();
		if (required==Bpmn2Preferences.class)
			return getPreferences();
		if (required == IPropertySheetPage.class) {
			return new Bpmn2TabbedPropertySheetPage(this);
		}
		return super.getAdapter(required);
	}

	@Override
	public void dispose() {
		// clear ID mapping tables if no more instances of editor are active
		int instances = 0;
		IWorkbenchPage[] pages = getEditorSite().getWorkbenchWindow().getPages();
		for (IWorkbenchPage p : pages) {
			IEditorReference[] refs = p.getEditorReferences();
			instances += refs.length;
		}
		ModelUtil.clearIDs(modelHandler.getResource(), instances==0);
		getPreferences().getGlobalPreferences().removePropertyChangeListener(this);
		
		getResourceSet().eAdapters().remove(getEditorAdapter());
		removeSelectionListener();
		removeFileChangeListener();
		if (instances==0)
			setActiveEditor(null);
		
		super.dispose();
		ModelHandlerLocator.remove(modelUri);
		// get rid of temp files and folders, but NOT if the workbench is being shut down.
		// when the workbench is restarted, we need to have those temp files around!
		if (!workbenchShutdown)
			BPMN2DiagramCreator.dispose(diagramFile);
		removeWorkbenchListener();
		removeMarkerChangeListener();
		getPreferences().dispose();
	}

	public IFile getModelFile() {
		return modelFile;
	}

	public IFile getDiagramFile() {
		return diagramFile;
	}
	
	public ModelHandler getModelHandler() {
		return modelHandler;
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
		super.doSave(monitor);

		Resource resource = getResourceSet().getResource(modelUri, false);
		BPMN2ProjectValidator.validateOnSave(resource, monitor);
	}

	@Override
	public boolean isSaveAsAllowed() {
		return true;
	}
	
	@Override
	public void doSaveAs() {
		IFile oldFile = getModelFile();
		SaveAsDialog saveAsDialog = new SaveAsDialog(getSite().getShell());
		saveAsDialog.setOriginalFile(oldFile);
		saveAsDialog.create();
		if (saveAsDialog.open() == SaveAsDialog.CANCEL) {
			return;
		}
		IPath newFilePath = saveAsDialog.getResult();
		if (newFilePath == null){
			return;
		}
		
        IFile newFile = ResourcesPlugin.getWorkspace().getRoot().getFile(newFilePath);
        IWorkbenchPage page = getSite().getPage();
        
        try {
        	// Save the current(old) file
        	doSave(null);
        	// if new file exists, close its editor (if open) and delete the existing file
            if (newFile.exists()) {
    			IEditorPart editorPart = ResourceUtil.findEditor(page, newFile);
    			if (editorPart!=null)
	    			page.closeEditor(editorPart, false);
        		newFile.delete(true, null);
            }
            // make a copy
			oldFile.copy(newFilePath, true, null);
		} catch (CoreException e) {
			showErrorDialogWithLogging(e);
			return;
		}

        // open new editor
    	try {
			page.openEditor(new FileEditorInput(newFile), BPMN2Editor.EDITOR_ID);
		} catch (PartInitException e1) {
			showErrorDialogWithLogging(e1);
			return;
		}
    	
    	// and close the old editor
		IEditorPart editorPart = ResourceUtil.findEditor(page, oldFile);
		if (editorPart!=null)
			page.closeEditor(editorPart, false);
		
    	try {
			newFile.refreshLocal(IResource.DEPTH_ZERO,null);
		} catch (CoreException e) {
			showErrorDialogWithLogging(e);
			return;
		}
	}

	public void closeEditor() {
		Display.getDefault().asyncExec(new Runnable() {
			public void run() {
				boolean closed = getSite().getPage().closeEditor(BPMN2Editor.this, false);
				if (!closed){
					// If close editor fails, try again with explicit editorpart 
					// of the old file
					IFile oldFile = ResourcesPlugin.getWorkspace().getRoot().getFile(modelFile.getFullPath());
					IEditorPart editorPart = ResourceUtil.findEditor(getSite().getPage(), oldFile);
					closed = getSite().getPage().closeEditor(editorPart, false);
				}
			}
		});
	}
	
	protected void reopenEditor(final IPath newFilePath) {
		Display display = getSite().getShell().getDisplay();
		display.syncExec(new Runnable() {
			public void run() {
				boolean closed = getSite().getPage().closeEditor(BPMN2Editor.this, false);
				if (!closed){
					// If close editor fails, try again with explicit editorpart 
					// of the old file
					IFile oldFile = ResourcesPlugin.getWorkspace().getRoot().getFile(modelFile.getFullPath());
					IEditorPart editorPart = ResourceUtil.findEditor(getSite().getPage(), oldFile);
					closed = getSite().getPage().closeEditor(editorPart, false);
				}
				if (closed) {
					IFile renamedFile = ResourcesPlugin.getWorkspace().getRoot().getFile(newFilePath);
					try {
						getSite().getPage().openEditor(new FileEditorInput(renamedFile), EDITOR_ID);
					} catch (PartInitException e) {
						Status status = new Status(IStatus.ERROR, Activator.PLUGIN_ID, e.getMessage(), e);
						ErrorUtils.showErrorWithLogging(status);
					}
				}
			}
		});
	}

	// Show error dialog and log the error
	private void showErrorDialogWithLogging(Exception e) {
		Status status = new Status(IStatus.ERROR, Activator.PLUGIN_ID, e.getMessage(), e);
		ErrorUtils.showErrorWithLogging(status);
	}

	////////////////////////////////////////////////////////////////////////////////
	// Other handlers
	////////////////////////////////////////////////////////////////////////////////

	@Override
	public void selectionChanged(IWorkbenchPart part, ISelection selection) {
		// Graphiti understands multipage editors
		super.selectionChanged(part,selection); // Graphiti's DiagramEditorInternal
		// but apparently GEF doesn't
		updateActions(getSelectionActions()); // usually done in GEF's GraphicalEditor
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.util.IPropertyChangeListener#propertyChange(org.eclipse.jface.util.PropertyChangeEvent)
	 */
	@Override
	public void propertyChange(PropertyChangeEvent event) {
		if (event.getProperty().endsWith(Bpmn2Preferences.PREF_SHAPE_STYLE)) {
			getEditingDomain().getCommandStack().execute(new RecordingCommand(getEditingDomain()) {
				@Override
				protected void doExecute() {
					IPeService peService = Graphiti.getPeService();
					TreeIterator<EObject> iter = getDiagramTypeProvider().getDiagram().eAllContents();
					while (iter.hasNext()) {
						EObject o = iter.next();
						if (o instanceof PictogramElement) {
							PictogramElement pe = (PictogramElement)o;
							BaseElement be = BusinessObjectUtil.getFirstElementOfType(pe, BaseElement.class);
							if (be!=null) {
								TreeIterator<EObject> childIter = pe.eAllContents();
								while (childIter.hasNext()) {
									o = childIter.next();
									if (o instanceof GraphicsAlgorithm) {
										GraphicsAlgorithm ga = (GraphicsAlgorithm)o;
										if (peService.getPropertyValue(ga, Bpmn2Preferences.PREF_SHAPE_STYLE)!=null) {
											StyleUtil.applyStyle(ga, be);
										}
									}
			
								}
							}
						}
					}
				}
			});
		}
	}
}
