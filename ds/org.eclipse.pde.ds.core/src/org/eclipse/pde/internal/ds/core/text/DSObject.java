/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Rafael Oliveira Nóbrega <rafael.oliveira@gmail.com> - bug 223738
 *******************************************************************************/
package org.eclipse.pde.internal.ds.core.text;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.pde.core.IModel;
import org.eclipse.pde.internal.core.text.DocumentObject;
import org.eclipse.pde.internal.core.text.IDocumentElementNode;
import org.eclipse.pde.internal.ds.core.IDSConstants;
import org.eclipse.pde.internal.ds.core.IDSRoot;

/**
 * All modeled objects of a declarative services xml file must extend from this
 * abstract class.
 * 
 * @since 3.4
 * @see CtXHelpModel
 * @see DSDocumentFactory
 */
public abstract class DSObject extends DocumentObject implements IDSConstants, Serializable {

	private static final long serialVersionUID = 1L;

	/**
	 * Constructs the DSObject and initializes its attributes.
	 * 
	 * @param model The model to associate with this DSObject
	 * @param tagName The xml tag name for this object
	 */
	public DSObject(DSModel model, String tagName) {
		super(model, tagName);
	}

	/**
	 * @return the children of the object or an empty List if none exist.
	 */
	public List getChildren() {
		//Create a copy of the child list instead of 
		//returning the list itself. That way, our list
		//of children cannot be altered from outside
		ArrayList list = new ArrayList();

		// Add children of this topic
		IDocumentElementNode[] childNodes = getChildNodes();
		if (childNodes.length > 0) {
			for (int i = 0; i < childNodes.length; ++i) {
				if (childNodes[i] instanceof DSObject) {
					list.add(childNodes[i]);
				}
			}
		}

		return list;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.text.DocumentElementNode#getAttributeIndent()
	 */
	protected String getAttributeIndent() {
		return " "; //$NON-NLS-1$
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.text.DocumentElementNode#getContentIndent()
	 */
	protected String getContentIndent() {
		return ""; //$NON-NLS-1$
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.text.DocumentElementNode#isLeafNode()
	 */
	public boolean isLeafNode() {
		return !canBeParent();
	}

	/**
	 * @return true iff this object is capable of containing children.
	 */
	public abstract boolean canBeParent();

	public abstract boolean canAddChild(int objectType);

	public abstract boolean canAddSibling(int objectType);

	//	public abstract boolean canAddSibling(int objectType);

	public void addChild(DSObject newObject, DSObject targetSibling, boolean insertBefore) {
		if (canAddChild(newObject.getType())) {
			int currentIndex = indexOf(targetSibling);
			if (!insertBefore) {
				currentIndex++;
			}
			addChildNode(newObject, currentIndex, true);
		}
	}

	public void addChild(DSObject newObject) {
		if (canAddChild(newObject.getType())) {
			addChildNode(newObject, true);
		}
	}

	/**
	 * @return the root model object that is an ancestor to this object.
	 */
	public DSModel getModel() {
		final IModel sharedModel = getSharedModel();
		if (sharedModel instanceof DSModel) {
			return (DSModel) sharedModel;
		}
		return null;
	}

	/**
	 * @return the root element that is an ancestor to this object.
	 */
	public IDSRoot getRoot() {
		final DSModel model = getModel();
		if (model != null) {
			return model.getDSRoot();
		}
		return null;
	}

	/**
	 * @return the identifier for this object to be used when displaying the element to the user
	 */
	public abstract String getName();

	/**
	 * Get the concrete type of this object, must be one of the TYPE constants defined in IDSConstants.
	 * @see IDSConstants
	 */
	public abstract int getType();

	/**
	 * @return the parent of this object, or <code>null</code> if there is no parent.
	 */
	public DSObject getParent() {
		IDocumentElementNode parent = getParentNode();
		return parent instanceof DSObject ? (DSObject) parent : null;
	}

	/**
	 * Check if the object is a direct or indirect descendant
	 * of the object parameter.
	 * 
	 * @param obj The object to find in this object's ancestry
	 * @return true iff obj is an ancestor of this object
	 */
	public boolean descendsFrom(DSObject obj) {
		if (this.equals(obj)) {
			return true;
		}
		if (getParent() != null && obj.canBeParent()) {
			return getParent().descendsFrom(obj);
		}
		return false;
	}

	/**
	 * @param DSObject the child used to locate a sibling
	 * @return the object preceding the specified one in the list of children
	 */
	public DSObject getPreviousSibling(DSObject DSObject) {
		return (DSObject) getPreviousSibling(DSObject, DSObject.class);
	}

	/**
	 * @param DSObject the child used to locate a sibling
	 * @return the object proceeding the specified one in the list of children
	 */
	public DSObject getNextSibling(DSObject DSObject) {
		return (DSObject) getNextSibling(DSObject, DSObject.class);
	}

	/**
	 * @return true iff a this object can be removed
	 */
	public boolean canBeRemoved() {
		if (getType() == TYPE_ROOT) { //Semantic Rule: The root element can never be removed
			return false;
		}
		return true;
	}

	public void removeChild(DSObject object) {
		if (object.canBeRemoved()) {
			removeChildNode(object, true);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.toc.TocObject#moveChild(org.eclipse.pde.internal.core.toc.TocObject, int)
	 */
	public void moveChild(DSObject object, int newRelativeIndex) {
		moveChildNode(object, newRelativeIndex, true);
	}

}