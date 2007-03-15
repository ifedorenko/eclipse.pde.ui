/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.views.dependencies;

import org.eclipse.jface.viewers.IContentProvider;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.pde.internal.ui.wizards.ListUtil;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;

public class DependenciesViewListPage extends DependenciesViewPage {

	/**
	 * 
	 */
	public DependenciesViewListPage(DependenciesView view,
			IContentProvider contentProvider) {
		super(view, contentProvider);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ui.view.DependenciesViewPage#createViewer(org.eclipse.swt.widgets.Composite)
	 */
	protected StructuredViewer createViewer(Composite parent) {
		Table table = new Table(parent, SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);

		fViewer = new TableViewer(table);
		fViewer.setContentProvider(fContentProvider);
		final DependenciesLabelProvider labelProvider = new DependenciesLabelProvider(
				false);
		fViewer.setLabelProvider(labelProvider);
		fViewer.getControl().addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
				labelProvider.dispose();
			}
		});
		fViewer.setComparator(ListUtil.PLUGIN_COMPARATOR);

		return fViewer;
	}
}
