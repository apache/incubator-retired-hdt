/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.hdt.ui.wizards.api2;

import java.io.IOException;
import java.util.ArrayList;

import org.apache.hdt.ui.ImageLibrary;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.ui.IJavaElementSearchConstants;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jdt.ui.wizards.NewTypeWizardPage;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.SelectionDialog;

/**
 * Pre-fills the new MapReduce driver class with a template.
 * 
 */

public class NewDriverWizardPage extends NewTypeWizardPage {
  private Button isCreateMapMethod;

  private Text reducerText;

  private Text mapperText;

  private final boolean showContainerSelector;

  public NewDriverWizardPage() {
    this(true);
  }

  public NewDriverWizardPage(boolean showContainerSelector) {
    super(true, "MapReduce Driver");

    this.showContainerSelector = showContainerSelector;
    setTitle("MapReduce Driver");
    setDescription("Create a new MapReduce driver (new API).");
    setImageDescriptor(ImageLibrary.get("wizard.driver.new"));
  }

  public void setSelection(IStructuredSelection selection) {
    initContainerPage(getInitialJavaElement(selection));
    initTypePage(getInitialJavaElement(selection));
  }

  @Override
  /**
   * Creates the new type using the entered field values.
   */
  public void createType(IProgressMonitor monitor) throws CoreException,
      InterruptedException {
    super.createType(monitor);
  }

  @Override
  protected void createTypeMembers(final IType newType, ImportsManager imports,
      final IProgressMonitor monitor) throws CoreException {
    super.createTypeMembers(newType, imports, monitor);
    imports.addImport("org.apache.hadoop.fs.Path");
    imports.addImport("org.apache.hadoop.io.Text");
    imports.addImport("org.apache.hadoop.io.IntWritable");
    imports.addImport("org.apache.hadoop.mapreduce.Job");
    imports.addImport("org.apache.hadoop.mapreduce.lib.input.FileInputFormat");
    imports.addImport("org.apache.hadoop.mapreduce.lib.output.FileOutputFormat");

    /**
     * TODO(jz) - move most code out of the runnable
     */
    getContainer().getShell().getDisplay().syncExec(new Runnable() {
      public void run() {

        String method = "public static void main(String[] args) throws IOException, InterruptedException, ClassNotFoundException {\n";
        method += "  Job job = new Job();\n\n";
        method += "  job.setJarByClass( ... );\n\n";
        method += "  job.setJobName( \"a nice name\"  );\n\n";

        method += "  FileInputFormat.setInputPaths(job, new Path(args[0]));\n";
        method += "  FileOutputFormat.setOutputPath(job, new Path(args[1]));\n\n";
        
        if (mapperText.getText().length() > 0) {
          method += "  job.setMapperClass(" + mapperText.getText()
              + ".class);\n\n";
        } else {
          method += "  // TODO: specify a mapper\njob.setMapperClass( ... );\n\n";
        }
        if (reducerText.getText().length() > 0) {
          method += "  job.setReducerClass(" + reducerText.getText()
              + ".class);\n\n";
        } else {
          method += "  // TODO: specify a reducer\njob.setReducerClass( ... );\n\n";
        }

        method += "  job.setOutputKeyClass(Text.class);\n";
    	method += "  job.setOutputValueClass(IntWritable.class);\n\n";
        
        method += "  boolean success = job.waitForCompletion(true);\n";
        method += "  System.exit(success ? 0 : 1);\n\t};";

        try {
          newType.createMethod(method, null, false, monitor);
        } catch (JavaModelException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
      }
    });
  }

  public void createControl(Composite parent) {
    // super.createControl(parent);

    initializeDialogUnits(parent);
    Composite composite = new Composite(parent, SWT.NONE);
    GridLayout layout = new GridLayout();
    layout.numColumns = 4;
    composite.setLayout(layout);

    createContainerControls(composite, 4);

    createPackageControls(composite, 4);
    createSeparator(composite, 4);
    createTypeNameControls(composite, 4);

    createSuperClassControls(composite, 4);
    createSuperInterfacesControls(composite, 4);
    createSeparator(composite, 4);

    createMapperControls(composite);
    createReducerControls(composite);

    if (!showContainerSelector) {
      setPackageFragmentRoot(null, false);
      setSuperClass("java.lang.Object", false);
      setSuperInterfaces(new ArrayList(), false);
    }

    setControl(composite);

    setFocus();
    handleFieldChanged(CONTAINER);

    // setSuperClass("org.apache.hadoop.mapred.MapReduceBase", true);
    // setSuperInterfaces(Arrays.asList(new String[]{
    // "org.apache.hadoop.mapred.Mapper" }), true);
  }

  @Override
  protected void handleFieldChanged(String fieldName) {
    super.handleFieldChanged(fieldName);

    validate();
  }

  private void validate() {
    if (showContainerSelector) {
      updateStatus(new IStatus[] { fContainerStatus, fPackageStatus,
          fTypeNameStatus, fSuperClassStatus, fSuperInterfacesStatus });
    } else {
      updateStatus(new IStatus[] { fTypeNameStatus, });
    }
  }

  private void createMapperControls(Composite composite) {
    this.mapperText = createBrowseClassControl(composite, "Ma&pper:",
        "&Browse...", "org.apache.hadoop.mapreduce.Mapper", "Mapper Selection");
  }

  private void createReducerControls(Composite composite) {
    this.reducerText = createBrowseClassControl(composite, "&Reducer:",
        "Browse&...", "org.apache.hadoop.mapreduce.Reducer", "Reducer Selection");
  }

  private Text createBrowseClassControl(final Composite composite,
      final String string, String browseButtonLabel,
      final String baseClassName, final String dialogTitle) {
    Label label = new Label(composite, SWT.NONE);
    GridData data = new GridData(GridData.FILL_HORIZONTAL);
    label.setText(string);
    label.setLayoutData(data);

    final Text text = new Text(composite, SWT.SINGLE | SWT.BORDER);
    GridData data2 = new GridData(GridData.FILL_HORIZONTAL);
    data2.horizontalSpan = 2;
    text.setLayoutData(data2);

    Button browse = new Button(composite, SWT.NONE);
    browse.setText(browseButtonLabel);
    GridData data3 = new GridData(GridData.FILL_HORIZONTAL);
    browse.setLayoutData(data3);
    browse.addListener(SWT.Selection, new Listener() {
      public void handleEvent(Event event) {
        IType baseType;
        try {
          baseType = getPackageFragmentRoot().getJavaProject().findType(
              baseClassName);

          // edit this to limit the scope
          SelectionDialog dialog = JavaUI.createTypeDialog(
              composite.getShell(), new ProgressMonitorDialog(composite
                  .getShell()), SearchEngine.createHierarchyScope(baseType),
              IJavaElementSearchConstants.CONSIDER_CLASSES, false);

          dialog.setMessage("&Choose a type:");
          dialog.setBlockOnOpen(true);
          dialog.setTitle(dialogTitle);
          dialog.open();

          if ((dialog.getReturnCode() == Window.OK)
              && (dialog.getResult().length > 0)) {
            IType type = (IType) dialog.getResult()[0];
            text.setText(type.getFullyQualifiedName());
          }
        } catch (JavaModelException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
      }
    });

    if (!showContainerSelector) {
      label.setEnabled(false);
      text.setEnabled(false);
      browse.setEnabled(false);
    }

    return text;
  }
}
