package debugrecorder.views;


import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Date;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
//import org.eclipse.ui.editors.text.TextEditor;
//import org.eclipse.ui.editors.text.TextEditor;
import org.eclipse.ui.part.*;
//import org.eclipse.ui.texteditor.AbstractTextEditor;
//import org.eclipse.ui.texteditor.AbstractTextEditor;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.ui.actions.SelectionConverter;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Image;
import org.eclipse.jface.action.*;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.ui.*;
import org.eclipse.swt.SWT;



/**
 * This sample class demonstrates how to plug-in a new
 * workbench view. The view shows data obtained from the
 * model. The sample creates a dummy model on the fly,
 * but a real implementation would connect to the model
 * available either in this or another plug-in (e.g. the workspace).
 * The view is connected to the model using a content provider.
 * <p>
 * The view uses a label provider to define how model
 * objects should be presented in the view. Each
 * view can present the same model objects using
 * different labels and icons, if needed. Alternatively,
 * a single label provider can be shared between views
 * in order to ensure that objects of the same type are
 * presented in the same way everywhere.
 * <p>
 */

@SuppressWarnings("restriction")
public class SuspiciousFiles extends ViewPart implements IPartListener2{

	/**
	 * The ID of the view as specified by the extension.
	 */
	public static final String ID = "debugrecorder.views.SuspiciousFiles";

	private TableViewer viewer;
	private Action load;
	private Action prev;
	private Action next;
	private Action doubleClickAction;

	/*
	 * The content provider class is responsible for
	 * providing objects to the view. It can wrap
	 * existing objects in adapters or simply return
	 * objects as-is. These objects may be sensitive
	 * to the current input of the view, or ignore
	 * it and always show the same content 
	 * (like Task List, for example).
	 */
	 
	class ViewContentProvider implements IStructuredContentProvider 
	{
		public void inputChanged(Viewer v, Object oldInput, Object newInput) 
		{
		}
		public void dispose() 
		{
		}
		public Object[] getElements(Object parent) 
		{
			return m_map.Statements.toArray();
		}
	}
	
	
	class ViewLabelProvider extends LabelProvider implements ITableLabelProvider 
	{
		public String getColumnText(Object obj, int index) 
		{
			FileInfo info = (FileInfo) obj;
			switch (index) 
			{
				case 0:
					return info.FileName;
				case 1:
					return String.valueOf(info.Rank);
				case 2:
					return String.valueOf(info.Suspiciousness);
				default:
					throw new RuntimeException("Invalid column setup.");
			}
		}
		
		public Image getColumnImage(Object obj, int index) 
		{
			return null;
		}
		public Image getImage(String image) 
		{
			return PlatformUI.getWorkbench().getSharedImages().getImage(image);
		}
	}
	
	class NameSorter extends ViewerSorter {}

	/**
	 * The constructor.
	 */
	public SuspiciousFiles() {}

	/**
	 * This is a callback that will allow us
	 * to create the viewer and initialize it.
	 */
	public void createPartControl(Composite parent) 
	{
		viewer = new TableViewer(parent, /*SWT.MULTI |*/ SWT.H_SCROLL | SWT.V_SCROLL);
		
		createColumns(viewer);

		viewer.setContentProvider(new ViewContentProvider());
		viewer.setLabelProvider(new ViewLabelProvider());

		//viewer.setSorter(new NameSorter());
		viewer.setInput(getViewSite());

		
		// Create the help context id for the viewer's control
		PlatformUI.getWorkbench().getHelpSystem().setHelp(viewer.getControl(), "DebugRecorder.viewer");
		makeActions();
		//hookContextMenu();
		hookDoubleClickAction();
		contributeToActionBars();
		
		IWorkbenchWindow workbenchWindow = getSite().getWorkbenchWindow();
        workbenchWindow.getPartService().addPartListener(this);
//        AbstractTextEditor part = (TextEditor)workbenchWindow.getActivePage().getActiveEditor();
//        workbenchWindow.getActivePage().addSelectionListener(this);
		// logging
		m_log = debugrecorder.Activator.getDefault().getLog();
	}
	ILog m_log;
	public void Log(String message)
	{
		if( m_log != null )
		{
			Status status = new Status(0, "edu.gatech.cc.debugrecorder", message);
			m_log.log(status);
		}
	}
	
	// This will create the columns for the table
	private void createColumns(TableViewer viewer) 
	{

		String[] titles = { "Suspicious File", "Rank", "Suspiciousness" };
		int[] bounds = { 200, 100, 100 };

		for (int i = 0; i < titles.length; i++) {
			TableViewerColumn column = new TableViewerColumn(viewer, SWT.NONE);
			column.getColumn().setText(titles[i]);
			column.getColumn().setWidth(bounds[i]);
			column.getColumn().setResizable(true);
			column.getColumn().setMoveable(true);
		}
		Table table = viewer.getTable();
		table.setHeaderVisible(true);
		table.setLinesVisible(true);
	}


	private void contributeToActionBars() {
		IActionBars bars = getViewSite().getActionBars();
		fillLocalPullDown(bars.getMenuManager());
		fillLocalToolBar(bars.getToolBarManager());
	}

	private void fillLocalPullDown(IMenuManager manager) {
		manager.add(load);
		manager.add(new Separator());
		manager.add(prev);
		manager.add(next);
	}

	
	private void fillLocalToolBar(IToolBarManager manager) {
		manager.add(load);
		manager.add(prev);
		manager.add(next);
	}

	public String m_selected = "";
	public FilesMap m_map = new FilesMap();
	
	private void loadFile(String path)
	{
		try 
		{
			BufferedReader reader = new BufferedReader(new FileReader(path));
			String line = null;
			int lineCount = 0;
			
			String projectName = reader.readLine();
			String projectPath = getProjectPath(projectName);
			if( projectPath != null )
			{
				m_map.m_basePath = projectPath;
				while( (line=reader.readLine()) != null )
				{
					FileInfo info = FileInfo.ImportFileInfo(line);
					if( info != null )
					{
						m_map.Statements.add(info);
					}
					else
					{
						System.out.println("Could not process line # " + lineCount);
					}
					lineCount++;
				}
			}
			else
			{
				showMessage("Could not load the project: " + projectName);
			}
			
			reader.close();
		} 
		catch (FileNotFoundException e) 
		{
			e.printStackTrace();
		}
		catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	
	private String getProjectPath(String name)
	{
		final IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
//		return ResourcesPlugin.getWorkspace().getRoot().getLocation().toString() + "/" + name;
		for (int j = 0; j < projects.length; j++) 
		{
			IProject project = projects[j];
			if( project != null )
			{
				if( project.getName().equals(name))
				{
					
					return project.getLocation().toString();
					//return project.getRawLocation().toString();
				}
			}
		}
		return null;
	}
		
	
	private void makeActions() {

		// Load
		load = new Action() {
			public void run() 
			{
				m_selected = "";
				viewer.getTable().removeAll();
//				String active = getActiveProject();
//				if( active == null )
//				{
//					showMessage("Could not get active project.  Make sure you have a project active first");
//					return;
//				}
				
				//m_map.m_basePath = active;
				
				try
				{
					Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
				    FileDialog fd = new FileDialog(shell, SWT.OPEN);
			        fd.setText("Open");
			        //fd.setFilterPath("C:/");
			        String[] filterExt = { "*.xml"};
			        fd.setFilterExtensions(filterExt);
			        m_selected = fd.open();

					   // };
					//}.start();
					
					// Block until user picks file.
					//while(  m_selected.equals( "" ) ){}
				
			        m_map.Statements.clear();
					loadFile(m_selected);
					
					for( FileInfo info : m_map.Statements )
					{
						viewer.add(info);
					}
					
				}
				catch( Exception ex )
				{
			        showMessage(ex.getMessage());
				}
			}
		};
		load.setText("Load statements");
		load.setToolTipText("Load statements for project");
		load.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().
			getImageDescriptor(ISharedImages.IMG_OBJ_FILE));

		
		// Previous
		prev = new Action() {
			public void run() 
			{
				int index = viewer.getTable().getSelectionIndex();
				index--;
				if( index >= 0 )
				{
					viewer.getTable().setSelection(index);
					
					// go
					ISelection selection = viewer.getSelection();
					Object obj = ((IStructuredSelection)selection).getFirstElement();
					FileInfo info = (FileInfo)obj;
					m_map.NavigateToFile(info);
					
					Date d = new Date(System.currentTimeMillis());
					Log("[Prev];"+index+";"+info.FileName+";"+d.toString());
				}
			}
		};
		prev.setText("Previous Statement");
		prev.setToolTipText("Goto previous statement");
		prev.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().
			getImageDescriptor(ISharedImages.IMG_TOOL_BACK));
		
		// Next		
		next = new Action() {
			public void run() 
			{
				int index = viewer.getTable().getSelectionIndex();
				index++;
				if( index < viewer.getTable().getItemCount() )
				{
					viewer.getTable().setSelection(index);
					
					// go
					ISelection selection = viewer.getSelection();
					Object obj = ((IStructuredSelection)selection).getFirstElement();
					FileInfo info = (FileInfo)obj;
					m_map.NavigateToFile(info);

					Date d = new Date(System.currentTimeMillis());
					Log("[Next];"+index+";"+info.FileName+";"+d.toString());
				}
			}
		};
		next.setText("Next Statement");
		next.setToolTipText("Goto next statement");
		next.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().
				getImageDescriptor(ISharedImages.IMG_TOOL_FORWARD));
		
		doubleClickAction = new Action() {
			public void run() 
			{
				ISelection selection = viewer.getSelection();
				Object obj = ((IStructuredSelection)selection).getFirstElement();
				FileInfo info = (FileInfo)obj;
				

//				String project = getActiveProject();
//				String project = getCurrentProject();
//				Log(project);
				m_map.NavigateToFile(info/*, project*/);
			
				int i = viewer.getTable().getSelectionIndex();
				
				Date d = new Date(System.currentTimeMillis());
				Log("[Double Click];"+i+";"+info.FileName+";"+d.toString());
			}
		};
	}

	private void hookDoubleClickAction() {
		viewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent event) {
				doubleClickAction.run();
			}
		});
	}
	private void showMessage(String message) {
		MessageDialog.openInformation(
			viewer.getControl().getShell(),
			"Suspicious Files",
			message);
	}

	/**
	 * Passing the focus request to the viewer's control.
	 */
	public void setFocus() {
		viewer.getControl().setFocus();
	}

	@Override
	public void partBroughtToTop(IWorkbenchPartReference partRef) {
		// TODO Auto-generated method stub
		if(partRef instanceof IEditorReference)
		{
			IEditorPart  editorPart =getSite().getWorkbenchWindow().getActivePage().getActiveEditor();
			
			if(editorPart  != null)
			{    
				IFileEditorInput input = (IFileEditorInput)editorPart.getEditorInput();
				IFile file = input.getFile();    
				Log("[Brought to top]:" + file.getFullPath());
			}
			
		}
	}

	@Override
	public void partClosed(IWorkbenchPartReference partRef) {
		// TODO Auto-generated method stub
		if(partRef instanceof IEditorReference)
		{
			IEditorPart  editorPart =getSite().getWorkbenchWindow().getActivePage().getActiveEditor();
			
			if(editorPart  != null)
			{    
				IFileEditorInput input = (IFileEditorInput)editorPart.getEditorInput();
				IFile file = input.getFile();    
				Log("[Closed]:" + file.getFullPath());
			}
			
		}
	}

	@Override
	public void partDeactivated(IWorkbenchPartReference partRef) {
		// TODO Auto-generated method stub
		if(partRef instanceof IEditorReference)
		{
			IEditorPart  editorPart =getSite().getWorkbenchWindow().getActivePage().getActiveEditor();
			
			if(editorPart  != null)
			{    
				IFileEditorInput input = (IFileEditorInput)editorPart.getEditorInput();
				IFile file = input.getFile();    
				Log("[Deactivated]:" + file.getFullPath());
			}
			
		}
	}

	@Override
	public void partOpened(IWorkbenchPartReference partRef) {
		// TODO Auto-generated method stub
		if(partRef instanceof IEditorReference)
		{
			IEditorPart  editorPart =getSite().getWorkbenchWindow().getActivePage().getActiveEditor();
			
			if(editorPart != null)
			{    
				IFileEditorInput input = (IFileEditorInput)editorPart.getEditorInput();
				IFile file = input.getFile();    
				Log("[Opened]:" + file.getFullPath());
			}
			
		}
	}

	@Override
	public void partHidden(IWorkbenchPartReference partRef) {
		// TODO Auto-generated method stub
	}

	@Override
	public void partVisible(IWorkbenchPartReference partRef) {
		
	}

	@Override
	public void partInputChanged(IWorkbenchPartReference partRef) {
		// TODO Auto-generated method stub
	}

	@Override
	public void partActivated(IWorkbenchPartReference partRef) {
		// TODO Auto-generated method stub
		if(partRef instanceof IEditorReference)
		{
			EditorPart  editorPart =(EditorPart)getSite().getWorkbenchWindow().getActivePage().getActiveEditor();
			if(editorPart  != null)
			{    
				IFileEditorInput input = (IFileEditorInput)editorPart.getEditorInput();
				IFile file = input.getFile();    
				Log("[Activated]:" + file.getFullPath());
				
				if(editorPart instanceof JavaEditor)
				{
//					String name = editorPart.getClass().getName();
					JavaEditor texteditor = (JavaEditor)editorPart;
					final StyledText widget = texteditor.getViewer().getTextWidget();
					widget.addListener(SWT.KeyUp, listener);
					widget.addListener(SWT.MouseUp, listener);
				}
			}
			
		}
	}

	private final Listener listener = new Listener() {

	    // This listener used to provide compatibility with Eclipse 3.4, otherwise
	    // org.eclipse.swt.custom.CaretListener might be a better option (Eclipse
	    // * 3.5+).

	    @Override
	    public void handleEvent(Event event) {
	    	checkStart();
	    }
	  };
	  private void checkStart(final IWorkbenchPart activePart) {
		    if (!(activePart instanceof JavaEditor)) {
		      return;
		    }

		    PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {
		      @Override
		      public void run() {
		        IJavaElement element = null;
		        IType type = null;
		        try {
		          element = SelectionConverter.getElementAtOffset((JavaEditor) activePart);
		          type = SelectionConverter.getTypeAtOffset((JavaEditor) activePart);
//		          SelectionConverter.
		          if (element != null) {
		            Log("[Element]:" + type.getElementType() + ":" + element.getElementType() +":" + element.getElementName());
		          }
		        } catch (JavaModelException e) {
		          // Nothing we can do.
		          System.err.println(getClass().getSimpleName() + " - checkStart: "
		              + e.getMessage());
		        }
		      }
		    });
		  }
	  private void checkStart() {
		      checkStart(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActivePart());
//		    }
		  }
}