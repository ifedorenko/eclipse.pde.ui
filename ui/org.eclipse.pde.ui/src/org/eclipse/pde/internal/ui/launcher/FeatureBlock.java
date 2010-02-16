/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.launcher;

import java.util.*;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jface.viewers.*;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.core.ifeature.*;
import org.eclipse.pde.internal.core.util.VersionUtil;
import org.eclipse.pde.internal.launching.launcher.BundleLauncherHelper;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.elements.DefaultContentProvider;
import org.eclipse.pde.internal.ui.util.SWTUtil;
import org.eclipse.pde.launching.IPDELauncherConstants;
import org.eclipse.pde.ui.launcher.AbstractLauncherTab;
import org.eclipse.pde.ui.launcher.PluginsTab;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.osgi.framework.Version;

public class FeatureBlock {

	class FeatureTableLabelProvider extends PDELabelProvider {
		public Image getColumnImage(Object obj, int index) {
			return index == 0 ? super.getColumnImage(obj, index) : null;
		}

		public String getColumnText(Object obj, int index) {
			switch (index) {
				case COLUMN_FEATURE_NAME :
					return super.getObjectText((IFeatureModel) fFeatureModelMap.get(obj), false);
				case COLUMN_FEATURE_VERSION :
					return (String) fFeatureVersionMap.get(obj);
				case COLUMN_FEATURE_LOCATION :
					String featureLocation = (String) fFeatureLocationMap.get(obj);
					return getLocationText(featureLocation);
				case COLUMN_PLUGIN_RESOLUTION :
					String pluginResolution = (String) fPluginResolutionMap.get(obj);
					return getLocationText(pluginResolution);
				default :
					return ""; //$NON-NLS-1$
			}
		}
	}

	class Listener extends SelectionAdapter {

		public void widgetSelected(SelectionEvent e) {
			Object source = e.getSource();
			if (source == fSelectAllButton) {
				handleSelectAll(true);
			} else if (source == fDeselectAllButton) {
				handleSelectAll(false);
			} else if (source == fAddRequiredFeaturesButton) {
				handleAddRequired();
			} else if (source == fDefaultsButton) {
				handleRestoreDefaults();
			} else if (source == fDefaultFeatureLocationCombo) {
				handleDefaultChange();
			} else if (source instanceof TableColumn) {
				handleColumn((TableColumn) source, 0);
			}
			fTab.updateLaunchConfigurationDialog();
		}

		private void handleAddRequired() {
			if (fViewer.getCheckedElements() != null && fViewer.getCheckedElements().length > 0) {
				Object[] featureModelIDs = fViewer.getCheckedElements();
				ArrayList requiredFeatureList = new ArrayList();

				for (int i = 0; i < featureModelIDs.length; i++) {
					requiredFeatureList.add(featureModelIDs[i]);
					getFeatureDependencies((IFeatureModel) fFeatureModelMap.get(featureModelIDs[i]), requiredFeatureList);
				}
				fViewer.setCheckedElements(requiredFeatureList.toArray());
			}
		}

		private void getFeatureDependencies(IFeatureModel model, ArrayList requiredFeatureList) {
			IFeature feature = model.getFeature();
			IFeatureImport[] featureImports = feature.getImports();
			for (int i = 0; i < featureImports.length; i++) {
				if (featureImports[i].getType() == IFeatureImport.FEATURE) {
					addFeature(requiredFeatureList, featureImports[i].getId());
				}
			}

			IFeatureChild[] featureIncludes = feature.getIncludedFeatures();
			for (int i = 0; i < featureIncludes.length; i++) {
				addFeature(requiredFeatureList, featureIncludes[i].getId());
			}
		}

		private void addFeature(ArrayList requiredFeatureList, String id) {
			IFeatureModel model;
			if (!requiredFeatureList.contains(id)) {
				model = (IFeatureModel) fFeatureModelMap.get(id);
				if (model != null) {
					requiredFeatureList.add(id);
					getFeatureDependencies(model, requiredFeatureList);
				}
			}
		}

		private void handleColumn(TableColumn tc, int sortDirn) {
			fTable.setSortColumn(tc);
			if (sortDirn == 0) {
				switch (fTable.getSortDirection()) {
					case SWT.DOWN :
						sortDirn = SWT.UP;
						break;
					case SWT.UP :
					default :
						sortDirn = SWT.DOWN;
				}
			}
			fTable.setSortDirection(sortDirn);
			int sortOrder = sortDirn == SWT.UP ? 1 : -1;
			int sortColumn = ((Integer) tc.getData(COLUMN_ID)).intValue();
			fViewer.setSorter(new TableSorter(sortColumn, sortOrder));
			saveSortOrder();
		}

		private void handleRestoreDefaults() {
			fDefaultFeatureLocationCombo.setText(getLocationText(LOCATION_WORKSPACE));
			fDefaultPluginResolutionCombo.setText(getLocationText(LOCATION_WORKSPACE));
			fViewer.setInput(getFeatures(LOCATION_WORKSPACE));
			fViewer.refresh(true);
		}

		private void handleSelectAll(boolean state) {
			fViewer.setAllChecked(state);
			updateCounter();
			fViewer.refresh(true);
		}

		private void handleDefaultChange() {
			String defaultLocation = getLocationConstant(fDefaultFeatureLocationCombo.getText());
			for (Iterator iterator = fFeatureModelMap.keySet().iterator(); iterator.hasNext();) {
				String id = (String) iterator.next();
				String location = (String) fFeatureLocationMap.get(id);
				if (LOCATION_DEFAULT.equalsIgnoreCase(location)) {
					IFeatureModel model = null;
					if (LOCATION_WORKSPACE.equalsIgnoreCase(defaultLocation)) {
						model = (IFeatureModel) fWorkspaceFeatureMap.get(id);
					} else {
						model = (IFeatureModel) fExternalFeatureMap.get(id);
					}
					if (model != null) {
						fFeatureModelMap.put(id, model);
						fFeatureVersionMap.put(id, model.getFeature().getVersion());
						fViewer.refresh(id, true);
					}
				}
			}
		}
	}

	private class LocationCellModifier implements ICellModifier {

		public boolean canModify(Object id, String property) {
			if (PROPERTY_LOCATION.equalsIgnoreCase(property)) {
				if (fWorkspaceFeatureMap.containsKey(id) && fExternalFeatureMap.containsKey(id)) {
					return fViewer.getChecked(id);
				}

			} else if (PROPERTY_RESOLUTION.equalsIgnoreCase(property)) {
				return true;
			}
			return false;
		}

		public Object getValue(Object id, String property) {
			if (PROPERTY_LOCATION.equalsIgnoreCase(property) || PROPERTY_RESOLUTION.equalsIgnoreCase(property)) {
				String location = (String) fFeatureLocationMap.get(id);

				if (LOCATION_DEFAULT.equalsIgnoreCase(location)) {
					return new Integer(0);
				} else if (LOCATION_WORKSPACE.equalsIgnoreCase(location)) {
					return new Integer(1);
				} else if (LOCATION_EXTERNAL.equalsIgnoreCase(location)) {
					return new Integer(2);
				}
			}

			return new Integer(0);
		}

		public void modify(Object item, String property, Object value) {
			String id = (String) ((TableItem) item).getData();
			int comboIndex = ((Integer) value).intValue();
			String location = null;
			HashMap map = null;
			switch (comboIndex) {
				case 0 :
					location = LOCATION_DEFAULT;
					if (LOCATION_WORKSPACE.equalsIgnoreCase(getLocationConstant(fDefaultFeatureLocationCombo.getText()))) {
						map = fWorkspaceFeatureMap;
					} else {
						map = fExternalFeatureMap;
					}
					break;
				case 1 :
					location = LOCATION_WORKSPACE;
					map = fWorkspaceFeatureMap;
					break;
				case 2 :
					location = LOCATION_EXTERNAL;
					map = fExternalFeatureMap;
			}

			if (PROPERTY_LOCATION.equalsIgnoreCase(property)) {
				fFeatureLocationMap.put(id, location);
				IFeatureModel newModel = (IFeatureModel) map.get(id);
				fFeatureVersionMap.put(id, newModel.getFeature().getVersion());
				fFeatureModelMap.put(id, newModel);
			} else if (PROPERTY_RESOLUTION.equalsIgnoreCase(property)) {
				fPluginResolutionMap.put(id, location);

			} else {
				return; // nothing to do
			}

			fViewer.refresh(id, true);
			fTab.updateLaunchConfigurationDialog();
		}
	}

	class PluginContentProvider extends DefaultContentProvider implements IStructuredContentProvider {

		public Object[] getElements(Object input) {
			return (String[]) input;
		}
	}

	private class TableSorter extends ViewerSorter {
		int sortColumn;
		int sortOrder;

		public TableSorter(int sortColumn, int sortOrder) {
			this.sortColumn = sortColumn;
			this.sortOrder = sortOrder;
		}

		/* (non-Javadoc)
		 * @see org.eclipse.jface.viewers.ViewerComparator#compare(org.eclipse.jface.viewers.Viewer, java.lang.Object, java.lang.Object)
		 */
		public int compare(Viewer viewer, Object e1, Object e2) {
			FeatureTableLabelProvider labelProvider = (FeatureTableLabelProvider) fViewer.getLabelProvider();
			return sortOrder * super.compare(viewer, labelProvider.getColumnText(e1, sortColumn), labelProvider.getColumnText(e2, sortColumn));
		}

	}

	private static final int COLUMN_FEATURE_NAME = 0;
	private static final int COLUMN_FEATURE_VERSION = 1;
	private static final int COLUMN_FEATURE_LOCATION = 2;
	private static final int COLUMN_PLUGIN_RESOLUTION = 3;

	private static final String COLUMN_ID = "columnID"; //$NON-NLS-1$

	private static final String LOCATION_DEFAULT = "Default"; //$NON-NLS-1$
	private static final String LOCATION_EXTERNAL = "External"; //$NON-NLS-1$
	private static final String LOCATION_WORKSPACE = "Workspace"; //$NON-NLS-1$
	private static final String PROPERTY_LOCATION = "location"; //$NON-NLS-1$
	private static final String PROPERTY_RESOLUTION = "resolution"; //$NON-NLS-1$

	private Label fCounter;
	private Button fAddRequiredFeaturesButton;
	private Button fDefaultsButton;
	private Button fSelectAllButton;
	private Button fDeselectAllButton;

	private Combo fDefaultFeatureLocationCombo;
	private Combo fDefaultPluginResolutionCombo;

	private HashMap fExternalFeatureMap;
	private HashMap fFeatureLocationMap;
	private HashMap fPluginResolutionMap;
	private HashMap fWorkspaceFeatureMap;
	private HashMap fFeatureVersionMap;
	private HashMap fFeatureModelMap;
	private ILaunchConfiguration fLaunchConfig;
	private Listener fListener;
	private AbstractLauncherTab fTab;
	private Table fTable;
	private CheckboxTableViewer fViewer;

	public FeatureBlock(PluginsTab pluginsTab) {
		Assert.isNotNull(pluginsTab);
		fTab = pluginsTab;
	}

	private Button createButton(Composite composite, String text, int style) {
		Button button = new Button(composite, style);
		button.setText(text);
		button.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
		SWTUtil.setButtonDimensionHint(button);
		button.addSelectionListener(fListener);
		return button;
	}

	private void createButtonContainer(Composite parent, int vOffset) {
		Composite composite = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.marginHeight = layout.marginWidth = 0;
		layout.marginTop = vOffset;
		composite.setLayout(layout);
		composite.setLayoutData(new GridData(GridData.FILL_VERTICAL));

		fSelectAllButton = createButton(composite, PDEUIMessages.AdvancedLauncherTab_selectAll, SWT.PUSH);
		fDeselectAllButton = createButton(composite, PDEUIMessages.AdvancedLauncherTab_deselectAll, SWT.PUSH);

		fAddRequiredFeaturesButton = createButton(composite, PDEUIMessages.FeatureBlock_addRequiredFeatues, SWT.PUSH);
		fDefaultsButton = createButton(composite, PDEUIMessages.AdvancedLauncherTab_defaults, SWT.PUSH);
		//fShowFilterButton = createButton(composite, PDEUIMessages.FeatureBlock_showSelected, SWT.CHECK);
		//fShowFilterButton.addSelectionListener(fListener);
		//GridData filterButtonGridData = new GridData(GridData.FILL_BOTH | GridData.VERTICAL_ALIGN_END);
		//fShowFilterButton.setLayoutData(filterButtonGridData);

		fCounter = new Label(composite, SWT.NONE);
		fCounter.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_END));
		updateCounter();
	}

	private void createCheckBoxTable(Composite parent, int span, int indent) {
		fViewer = CheckboxTableViewer.newCheckList(parent, SWT.BORDER | SWT.FULL_SELECTION | SWT.MULTI);
		fTable = fViewer.getTable();

		GridData gd = new GridData(GridData.FILL_BOTH);
		gd.horizontalSpan = span;
		gd.horizontalIndent = indent;
		fTable.setLayoutData(gd);

		TableColumn column1 = new TableColumn(fTable, SWT.LEFT);
		column1.setText(PDEUIMessages.FeatureBlock_features);
		column1.setWidth(250);
		column1.addSelectionListener(fListener);
		column1.setData(COLUMN_ID, new Integer(0));

		TableColumn column2 = new TableColumn(fTable, SWT.LEFT);
		column2.setText(PDEUIMessages.FeatureBlock_version);
		column2.setWidth(100);
		column2.addSelectionListener(fListener);
		column2.setData(COLUMN_ID, new Integer(1));

		TableColumn column3 = new TableColumn(fTable, SWT.LEFT);
		column3.setText(PDEUIMessages.FeatureBlock_featureLocation);
		column3.setWidth(100);
		column3.addSelectionListener(fListener);
		column3.setData(COLUMN_ID, new Integer(2));

		TableColumn column4 = new TableColumn(fTable, SWT.LEFT);
		column4.setText(PDEUIMessages.FeatureBlock_pluginResolution);
		column4.setWidth(100);
		column4.addSelectionListener(fListener);
		column4.setData(COLUMN_ID, new Integer(3));

		fTable.setHeaderVisible(true);

		fViewer.setLabelProvider(new FeatureTableLabelProvider());
		fViewer.setContentProvider(new PluginContentProvider());
		fViewer.setInput(getFeatures(LOCATION_WORKSPACE));

		fViewer.addCheckStateListener(new ICheckStateListener() {

			public void checkStateChanged(CheckStateChangedEvent event) {
				String id = (String) event.getElement();
				if (event.getChecked() == false) {
					fFeatureLocationMap.put(id, LOCATION_DEFAULT);
					fPluginResolutionMap.put(id, LOCATION_DEFAULT);
					IFeatureModel model = (IFeatureModel) fWorkspaceFeatureMap.get(id);
					if (model == null) {
						model = (IFeatureModel) fExternalFeatureMap.get(id);
					}
					fFeatureModelMap.put(id, model);
					fFeatureVersionMap.put(id, model.getFeature().getVersion());
				}
				fViewer.refresh(id, true);
				fTab.updateLaunchConfigurationDialog();
			}
		});

		String[] items = new String[] {PDEUIMessages.FeatureBlock_default, PDEUIMessages.FeatureBlock_workspaceBefore, PDEUIMessages.FeatureBlock_externalBefore};
		fViewer.setCellEditors(new CellEditor[] {null, null, new ComboBoxCellEditor(fTable, items), new ComboBoxCellEditor(fTable, items)});
		fViewer.setColumnProperties(new String[] {null, null, PROPERTY_LOCATION, PROPERTY_RESOLUTION});

		fViewer.setCellModifier(new LocationCellModifier());
		fViewer.addDoubleClickListener(new IDoubleClickListener() {

			public void doubleClick(DoubleClickEvent event) {
				ISelection selection = event.getSelection();
				if (selection == null || !(selection instanceof IStructuredSelection)) {
					return;
				}
				fViewer.editElement(((IStructuredSelection) selection).getFirstElement(), 2);
			}
		});

		TableViewerColumn tvc = new TableViewerColumn(fViewer, column3);
		tvc.setLabelProvider(new CellLabelProvider() {

			public void update(ViewerCell cell) {
				String id = (String) cell.getElement();
				Display display = fTable.getDisplay();
				Color gray = display.getSystemColor(SWT.COLOR_WIDGET_LIGHT_SHADOW);
				Color white = display.getSystemColor(SWT.COLOR_WHITE);
				if (fWorkspaceFeatureMap.containsKey(id) && fExternalFeatureMap.containsKey(id) && fViewer.getChecked(id)) {
					cell.setBackground(white);
				} else {
					cell.setBackground(gray);
				}
				FeatureTableLabelProvider provider = (FeatureTableLabelProvider) fViewer.getLabelProvider();
				cell.setText(provider.getColumnText(id, COLUMN_FEATURE_LOCATION));
			}
		});
	}

	public void createControl(Composite parent, int span, int indent) {
		fListener = new Listener();

		Label label = SWTFactory.createLabel(parent, PDEUIMessages.FeatureBlock_defaultFeatureLocation, 1);
		GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
		gd.horizontalIndent = indent;
		label.setLayoutData(gd);
		fDefaultFeatureLocationCombo = SWTFactory.createCombo(parent, SWT.READ_ONLY | SWT.BORDER, 1, GridData.HORIZONTAL_ALIGN_BEGINNING, new String[] {PDEUIMessages.FeatureBlock_workspaceBefore, PDEUIMessages.FeatureBlock_externalBefore});

		label = SWTFactory.createLabel(parent, PDEUIMessages.FeatureBlock_defaultPluginResolution, 1);
		fDefaultPluginResolutionCombo = SWTFactory.createCombo(parent, SWT.READ_ONLY | SWT.BORDER, 1, GridData.HORIZONTAL_ALIGN_BEGINNING, new String[] {PDEUIMessages.FeatureBlock_workspaceBefore, PDEUIMessages.FeatureBlock_externalBefore});

		fDefaultFeatureLocationCombo.addSelectionListener(fListener);
		fDefaultPluginResolutionCombo.addSelectionListener(fListener);
		createCheckBoxTable(parent, span - 1, indent);
		createButtonContainer(parent, 10);
	}

	public void dispose() {
		PDEPlugin.getDefault().getLabelProvider().disconnect(this);
	}

	private Object getFeatures(String defaultLocation) {
		FeatureModelManager fmm = new FeatureModelManager();

		fWorkspaceFeatureMap = new HashMap();
		fExternalFeatureMap = new HashMap();
		fFeatureLocationMap = new HashMap();
		fPluginResolutionMap = new HashMap();
		fFeatureVersionMap = new HashMap();
		fFeatureModelMap = new HashMap();

		IFeatureModel[] workspaceModels = fmm.getWorkspaceModels();
		for (int i = 0; i < workspaceModels.length; i++) {
			String id = workspaceModels[i].getFeature().getId();
			fWorkspaceFeatureMap.put(id, workspaceModels[i]);
			fFeatureLocationMap.put(id, LOCATION_DEFAULT);
			fPluginResolutionMap.put(id, LOCATION_DEFAULT);
			fFeatureVersionMap.put(id, workspaceModels[i].getFeature().getVersion());
			fFeatureModelMap.put(id, workspaceModels[i]);
		}
		fmm.shutdown();

		ExternalFeatureModelManager efmm = new ExternalFeatureModelManager();
		efmm.startup();
		IFeatureModel[] externalModels = efmm.getModels();
		for (int i = 0; i < externalModels.length; i++) {
			String id = externalModels[i].getFeature().getId();
			fExternalFeatureMap.put(id, externalModels[i]);
			if (LOCATION_EXTERNAL.equalsIgnoreCase(defaultLocation) || (LOCATION_WORKSPACE.equalsIgnoreCase(defaultLocation) && !fWorkspaceFeatureMap.containsKey(id))) {
				fFeatureLocationMap.put(id, LOCATION_DEFAULT);
				fPluginResolutionMap.put(id, LOCATION_DEFAULT);
				fFeatureVersionMap.put(id, externalModels[i].getFeature().getVersion());
				fFeatureModelMap.put(id, externalModels[i]);
			}
		}
		efmm.shutdown();
		return fFeatureModelMap.keySet().toArray(new String[fFeatureModelMap.size()]);
	}

	private String getLocationConstant(String value) {
		if (PDEUIMessages.FeatureBlock_workspaceBefore.equalsIgnoreCase(value)) {
			return LOCATION_WORKSPACE;
		} else if (PDEUIMessages.FeatureBlock_externalBefore.equalsIgnoreCase(value)) {
			return LOCATION_EXTERNAL;
		}
		return value;
	}

	private String getLocationText(String location) {
		if (LOCATION_DEFAULT.equalsIgnoreCase(location)) {
			return PDEUIMessages.FeatureBlock_default;
		} else if (LOCATION_WORKSPACE.equalsIgnoreCase(location)) {
			return PDEUIMessages.FeatureBlock_workspaceBefore;
		} else if (LOCATION_EXTERNAL.equalsIgnoreCase(location)) {
			return PDEUIMessages.FeatureBlock_externalBefore;
		}
		return ""; //$NON-NLS-1$
	}

	private IFeatureModel getFeatureModel(String id) {
		String version = (String) fFeatureVersionMap.get(id);
		Version featureVersion = Version.parseVersion(version);
		IFeatureModel model = (IFeatureModel) fExternalFeatureMap.get(id);
		Version modelVersion = Version.parseVersion(model.getFeature().getVersion());
		if (VersionUtil.isEquivalentTo(featureVersion, modelVersion)) {
			return model;
		}

		model = (IFeatureModel) fWorkspaceFeatureMap.get(id);
		modelVersion = Version.parseVersion(model.getFeature().getVersion());
		if (VersionUtil.isEquivalentTo(featureVersion, modelVersion)) {
			return model;
		}
		return null;
	}

	public void initialize() throws CoreException {
		initializeFrom(fLaunchConfig);
	}

	public void initializeFrom(ILaunchConfiguration config) throws CoreException {
		if (fLaunchConfig != null && fLaunchConfig.equals(config)) {
			// Do nothing
			return;
		}

		fLaunchConfig = config;
		String defaultLocation = config.getAttribute(IPDELauncherConstants.FEATURE_DEFAULT_LOCATION, LOCATION_WORKSPACE);
		fDefaultFeatureLocationCombo.setText(getLocationText(defaultLocation));
		String pluginResolution = config.getAttribute(IPDELauncherConstants.FEATURE_PLUGIN_RESOLUTION, LOCATION_WORKSPACE);
		fDefaultPluginResolutionCombo.setText(getLocationText(pluginResolution));
		fViewer.setInput(getFeatures(defaultLocation));

		ArrayList selectedFeatureList = BundleLauncherHelper.getFeatureMaps(config, fFeatureVersionMap, fFeatureLocationMap, fPluginResolutionMap);

		for (int index = 0; index < selectedFeatureList.size(); index++) {
			String id = (String) selectedFeatureList.get(index);
			fFeatureModelMap.put(id, getFeatureModel(id));
			fViewer.setChecked(id, true);
		}

		// If the workspace plug-in state has changed (project closed, etc.) the launch config needs to be updated without making the tab dirty
		if (fLaunchConfig.isWorkingCopy()) {
			savePluginState((ILaunchConfigurationWorkingCopy) fLaunchConfig);
		}

		updateCounter();
		fTab.updateLaunchConfigurationDialog();

		PDEPreferencesManager prefs = new PDEPreferencesManager(IPDEUIConstants.PLUGIN_ID);
		int index = prefs.getInt(IPreferenceConstants.FEATURE_SORT_COLUMN);
		TableColumn column = fTable.getColumn(index == 0 ? COLUMN_FEATURE_LOCATION : index - 1);
		fListener.handleColumn(column, prefs.getInt(IPreferenceConstants.FEATURE_SORT_ORDER));
		fViewer.refresh(true);
	}

	public void performApply(ILaunchConfigurationWorkingCopy config) {
		config.setAttribute(IPDELauncherConstants.SHOW_SELECTED_ONLY, false);
		savePluginState(config);
		saveSortOrder();
		updateCounter();
	}

	private void savePluginState(ILaunchConfigurationWorkingCopy config) {
		StringBuffer featuresEntry = new StringBuffer();
		if (fViewer.getCheckedElements() != null && fViewer.getCheckedElements().length > 0) {
			Object[] selectedFeatureModels = fViewer.getCheckedElements();
			Arrays.sort(selectedFeatureModels); // So that Tab is not marked dirty due to Sorting changes
			for (int i = 0; i < selectedFeatureModels.length; i++) {
				String id = (String) selectedFeatureModels[i];
				String location = (String) fFeatureLocationMap.get(id);
				String resolution = (String) fPluginResolutionMap.get(id);
				String version = (String) fFeatureVersionMap.get(id);
				String value = BundleLauncherHelper.writeFeatureEntry(id, version, location, resolution);
				featuresEntry.append(value);
			}
		}
		config.setAttribute(IPDELauncherConstants.SELECTED_FEATURES, featuresEntry.length() == 0 ? (String) null : featuresEntry.toString());
		config.setAttribute(IPDELauncherConstants.FEATURE_DEFAULT_LOCATION, getLocationConstant(fDefaultFeatureLocationCombo.getText()));
		config.setAttribute(IPDELauncherConstants.FEATURE_PLUGIN_RESOLUTION, getLocationConstant(fDefaultPluginResolutionCombo.getText()));

	}

	private void saveSortOrder() {
		PDEPreferencesManager prefs = new PDEPreferencesManager(IPDEUIConstants.PLUGIN_ID);
		TableColumn column = fTable.getSortColumn();
		int index = column == null ? 0 : ((Integer) fTable.getSortColumn().getData(COLUMN_ID)).intValue();
		prefs.setValue(IPreferenceConstants.FEATURE_SORT_COLUMN, index + 1);
		int sortOrder = column == null ? 0 : fTable.getSortDirection();
		prefs.setValue(IPreferenceConstants.FEATURE_SORT_ORDER, sortOrder);
		prefs.savePluginPreferences();
	}

	public void setDefaults(ILaunchConfigurationWorkingCopy config) {
		config.setAttribute(IPDELauncherConstants.SHOW_SELECTED_ONLY, false);
	}

	private void updateCounter() {
		if (fCounter != null) {
			int checked = fViewer.getCheckedElements().length;
			int total = fFeatureLocationMap.keySet().size();
			fCounter.setText(NLS.bind(PDEUIMessages.AbstractPluginBlock_counter, new Integer(checked), new Integer(total)));
		}
	}
}