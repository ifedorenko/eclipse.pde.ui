/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.schema;

import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.core.ischema.ISchemaElement;
import org.eclipse.pde.internal.core.schema.SchemaElementReference;
import org.eclipse.pde.internal.core.schema.SchemaRootElement;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.FormEntryAdapter;
import org.eclipse.pde.internal.ui.parts.ComboPart;
import org.eclipse.pde.internal.ui.parts.FormEntry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.forms.FormColors;
import org.eclipse.ui.forms.widgets.FormToolkit;

public class SchemaRootElementDetails extends AbstractSchemaDetails {

	private SchemaRootElement fElement;
	private FormEntry fName;
	private ComboPart fDeprecated;
	private FormEntry fSuggestion;
	
	public SchemaRootElementDetails(ISchemaElement element, ElementSection section) {
		super(section, true);
		if (element instanceof SchemaElementReference)
			element = (SchemaRootElement)((SchemaElementReference)element).getReferencedObject();
		fElement = (SchemaRootElement)element;
	}

	public void createDetails(Composite parent) {
		FormToolkit toolkit = getManagedForm().getToolkit();
		Color foreground = toolkit.getColors().getColor(FormColors.TITLE);
		
		fName = new FormEntry(parent, toolkit, PDEUIMessages.SchemaDetails_name, SWT.NONE);

		Label label = toolkit.createLabel(parent, PDEUIMessages.SchemaDetails_deprecated);
		label.setForeground(foreground);
		fDeprecated = createComboPart(parent, toolkit, BOOLS, 2);

		fSuggestion = new FormEntry(parent, toolkit, PDEUIMessages.SchemaRootElementDetails_replacement, null, false, 6);
		
		setText(PDEUIMessages.SchemaElementDetails_title);
		setDecription(NLS.bind(PDEUIMessages.SchemaElementDetails_description, fElement.getName()));
	}

	public void updateFields() {
		if (fElement == null)
			return;
		fName.setValue(fElement.getName(), true);		
		fDeprecated.select(fElement.isDeprecated() ? 0 : 1);
		fSuggestion.setValue(fElement.getDeprecatedSuggestion(), true);
		
		fName.setEditable(isEditable());
		fDeprecated.setEnabled(isEditable());

		if (!fElement.isDeprecated()) {
			fSuggestion.getLabel().setEnabled(false);
			fSuggestion.getText().setEditable(false);
		} else {
			fSuggestion.setEditable(isEditable());
		}
	}

	public void hookListeners() {
		fName.setFormEntryListener(new FormEntryAdapter(this) {
			public void textValueChanged(FormEntry entry) {
				fElement.setName(fName.getValue());
				setDecription(NLS.bind(PDEUIMessages.SchemaElementDetails_description, fElement.getName()));
			}
		});
		fDeprecated.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				boolean deprecated = fDeprecated.getSelectionIndex() == 0;
				fElement.setDeprecatedProperty(deprecated);				
				fSuggestion.getLabel().setEnabled(deprecated);
				fSuggestion.getText().setEditable(deprecated);			
			}
		});
		fSuggestion.setFormEntryListener(new FormEntryAdapter(this) {
			public void textValueChanged(FormEntry entry) {
				fElement.setDeprecatedSuggestion(fSuggestion.getValue());
			}
		});
	}
}
