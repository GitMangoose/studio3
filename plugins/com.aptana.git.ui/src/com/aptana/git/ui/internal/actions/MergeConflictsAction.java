package com.aptana.git.ui.internal.actions;

import org.eclipse.compare.CompareUI;
import org.eclipse.compare.ITypedElement;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.team.core.history.IFileRevision;
import org.eclipse.team.internal.ui.history.FileRevisionTypedElement;
import org.eclipse.team.ui.synchronize.SaveableCompareEditorInput;

import com.aptana.git.core.GitPlugin;
import com.aptana.git.core.model.ChangedFile;
import com.aptana.git.core.model.GitCommit;
import com.aptana.git.core.model.GitRepository;
import com.aptana.git.ui.GitUIPlugin;
import com.aptana.git.ui.actions.GitAction;
import com.aptana.git.ui.internal.history.GitCompareFileRevisionEditorInput;

@SuppressWarnings("restriction")
public class MergeConflictsAction extends GitAction
{

	@Override
	public void run()
	{
		IResource[] resources = getSelectedResources();
		if (resources == null || resources.length != 1)
			return;
		IResource blah = resources[0];
		if (blah.getType() != IResource.FILE)
			return;
		GitRepository repo = GitRepository.getAttached(blah.getProject());
		if (repo == null)
			return;
		String name = repo.getChangedFileForResource(blah).getPath();
		IFile file = (IFile) blah;
		try
		{
			IPath copyPath = file.getFullPath().addFileExtension("conflict");
			IFile copy = ResourcesPlugin.getWorkspace().getRoot().getFile(copyPath);
			if (!copy.exists())
			{
				// We create a copy of the failed merged file with the markers as generated by Git
				file.copy(copyPath, true, new NullProgressMonitor());
				// Then we replace the working file's contents by the contents pre-merge
				final IFileRevision baseFile = GitPlugin.revisionForCommit(new GitCommit(repo, ":2"), name);
				IStorage storage = baseFile.getStorage(new NullProgressMonitor());
				file.setContents(storage.getContents(), true, true, new NullProgressMonitor());
			}
		}
		catch (CoreException e)
		{
			GitUIPlugin.logError(e);
			return;
		}
		// Now we use the pre-merge file and compare against the merging version.
		ITypedElement base = SaveableCompareEditorInput.createFileElement(file);
		final IFileRevision nextFile = GitPlugin.revisionForCommit(new GitCommit(repo, ":3"), name);
		final ITypedElement next = new FileRevisionTypedElement(nextFile);
		final GitCompareFileRevisionEditorInput in = new GitCompareFileRevisionEditorInput(base, next, null);
		CompareUI.openCompareEditor(in);
	}

	@Override
	public boolean isEnabled()
	{
		IResource[] resources = getSelectedResources();
		if (resources == null || resources.length != 1)
			return false;
		IResource blah = resources[0];
		if (blah.getType() != IResource.FILE)
			return false;
		GitRepository repo = GitRepository.getAttached(blah.getProject());
		if (repo == null)
			return false;
		ChangedFile file = repo.getChangedFileForResource(blah);
		if (file == null)
			return false;
		return file.hasUnmergedChanges();
	}
}
