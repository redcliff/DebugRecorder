package debugrecorder.views;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.texteditor.ITextEditor;

public class FilesMap 
{
	public List<FileInfo> Statements = new ArrayList<FileInfo>();
	
	public String m_basePath = "";

	public void NavigateToFile(FileInfo info)
	{
		String path = m_basePath + "/src/" + info.FileName.substring(0, info.FileName.length() - 5).replace(".", "/") + ".java";
		NavigateToFile(path, 0);
	}
	
	public void NavigateToFile(FileInfo info, String project)
	{
		String path = project + "/src";
		
		String[] file = info.FileName.split("\\.");
//		String path = m_basePath;
		for(int i = 0; i < file.length - 1; i++)
			path = path + "/" + file[i];
		path = path + "." + file[file.length - 1];
		NavigateToFile(path, 0);
	}
	
	
	private void NavigateToFile(String file, int line)
	{
		File fileToOpen = new File(file);

		if (fileToOpen.exists() && fileToOpen.isFile()) 
		{
			IFileStore fileStore = EFS.getLocalFileSystem().getStore(fileToOpen.toURI());
			IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();

			try 
			{
				IEditorPart p = IDE.openEditorOnFileStore( page, fileStore );
				if( p instanceof ITextEditor )
				{
					ITextEditor editor = (ITextEditor)p;
					IDocument document = editor.getDocumentProvider().getDocument(p.getEditorInput());
					try
					{
						editor.selectAndReveal(document.getLineOffset(line),document.getLineLength(line));
					}
					catch (BadLocationException e)
					{
						e.printStackTrace();
					}
				}
				// IJavaElement editorCU=EditorUtility.getEditorInputJavaElement(editor, false);
			} 
			catch ( PartInitException e ) 
			{
				//Put your exception handler here if you wish to
			}
		} 
		else 
		{
			//Do something if the file does not exist
		}
	}
	

}
