/*
 * Created on Oct 6, 2003
 */
package org.eclipse.pde.internal.ui.feature;

import java.lang.reflect.*;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.action.*;
import org.eclipse.jface.dialogs.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.core.*;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.core.isite.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.ui.*;

/**
 * @author melhem
 */
public class BuildSiteAction implements IObjectActionDelegate, IPreferenceConstants {
	
	private ISiteBuildModel fBuildModel;
	private IFile fSiteXML;
	

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IObjectActionDelegate#setActivePart(org.eclipse.jface.action.IAction, org.eclipse.ui.IWorkbenchPart)
	 */
	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IActionDelegate#run(org.eclipse.jface.action.IAction)
	 */
	public void run(IAction action) {
		if (fBuildModel == null)
			return;
		ISiteBuildFeature[] sbFeatures = fBuildModel.getSiteBuild()
				.getFeatures();
		if (sbFeatures.length == 0)
			return;
		BuildSiteOperation op = new BuildSiteOperation(sbFeatures, fSiteXML
				.getProject(), fBuildModel);
		ProgressMonitorDialog pmd = new ProgressMonitorDialog(PDEPlugin
				.getActiveWorkbenchShell());
		try {
			pmd.run(true, true, op);
		} catch (InvocationTargetException e) {
			MessageDialog.openError(PDEPlugin.getActiveWorkbenchShell(),
				PDEPlugin.getResourceString("SiteBuild.errorDialog"),
				PDEPlugin.getResourceString("SiteBuild.errorMessage"));
		} catch (InterruptedException e) {
		}
	}
		
	public void selectionChanged(IAction action, ISelection selection) {
		fBuildModel = null;
		if (selection instanceof IStructuredSelection) {
			Object obj = ((IStructuredSelection) selection).getFirstElement();
			if (obj != null && obj instanceof IFile) {
				fSiteXML = (IFile)obj;
				IProject project = fSiteXML.getProject();
				IWorkspaceModelManager manager =
					PDECore.getDefault().getWorkspaceModelManager();
				IResource buildFile =
					project.findMember(
						new Path(PDECore.SITEBUILD_DIR).append(
							PDECore.SITEBUILD_PROPERTIES));
				if (buildFile != null && buildFile instanceof IFile) {
					manager.connect(buildFile, this);
					fBuildModel = (ISiteBuildModel) manager.getModel(buildFile, this);
					try {
						fBuildModel.load();
					} catch (CoreException e) {
					}
					manager.disconnect(buildFile, this);
				}
			}
		}
	}
	
}
