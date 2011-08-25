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
package org.eclipse.pde.core.spi;

import java.util.Collection;
import java.util.Map;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;

/**
 * Resolves dynamically generated bundle classpath entries in the context of a java project.
 * 
 * <p>
 * Generally, dynamically generated bundle classpath entries are not present under project
 * source tree but included in the bundle as part build process. During development time such bundle classpath entries
 * can be resolved to an external jar files or workspace resources. Resolution of the
 * same entry may change over time, similarly to how Plug-in Dependencies classpath container can switch between 
 * external bundles and workspace projects.
 * </p>
 * 
 * <p>
 * A resolver is declared as an extension (<code>org.eclipse.pde.core.bundleClasspathResolvers</code>). This 
 * extension has the following attributes: 
 * <ul>
 * <li><code>nature</code> specified nature of the projects this resolver is registered for.</li>
 * <li><code>class</code> specifies the fully qualified name of the Java class that implements 
 *     <code>IBundleClasspathResolver</code>.</li>
 * </ul>
 * </p>
 * <p> 
 * The resolver is consulted when dynamically generated bundle is added to OSGi runtime launch and when looking up 
 * sources from the bundle.
 * </p>
 * 
 * @since 4.0
 */
public interface IBundleClasspathResolver {

	/**
	 * Returns possibly empty bundle classpath map for dynamically generated bundle classpath entries.
	 * 
	 * The map key is relative path (from bundle root) of a dynamically generated bundle classpath entry. The map value
	 * is a collection of locations the bundle entry is generated from. Each locatoin is is the absolute path to the JAR 
	 * (or root folder), and in case it refers to an external library, then there is no associated resource in the 
	 * workbench.
	 * 
	 * @param javaProject the java project
	 * @return possibly empty classpath map
	 * 
	 * @see IClasspathEntry
	 */
	public Map/*<IPath, Collection<IPath>>*/getClasspathMap(IJavaProject javaProject);

	/**
	 * Returns possibly empty source lookup path for dynamically generated bundle classpath entries.
	 * 
	 * @param javaProject the java project
	 * @return possible empty source lookup path
	 */
	public Collection/*<IRuntimeClasspathEntry>*/getSourceLookupPath(IJavaProject javaProject);
}
