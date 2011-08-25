/*******************************************************************************
 * Copyright (c) 2011 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core;

import java.util.ArrayList;
import java.util.List;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.*;
import org.eclipse.pde.core.spi.IBundleClasspathResolver;

public class ClasspathContainerResolverManager {

	private static final String POINT_ID = "org.eclipse.pde.core.bundleClasspathResolvers"; //$NON-NLS-1$
	private static final String ATT_NATURE = "nature"; //$NON-NLS-1$
	private static final String ATT_CLASS = "class"; //$NON-NLS-1$

	public IBundleClasspathResolver[] getBundleClasspathResolvers(IProject project) {
		List result = new ArrayList();

		IExtensionRegistry registry = Platform.getExtensionRegistry();
		IConfigurationElement[] elements = registry.getConfigurationElementsFor(POINT_ID);
		for (int i = 0; i < elements.length; i++) {
			String attrNature = elements[i].getAttribute(ATT_NATURE);
			try {
				if (project.isNatureEnabled(attrNature)) {
					result.add(elements[i].createExecutableExtension(ATT_CLASS));
				}
			} catch (CoreException e) {
				PDECore.log(e.getStatus());
			}
		}

		return (IBundleClasspathResolver[]) result.toArray(new IBundleClasspathResolver[result.size()]);
	}

}
