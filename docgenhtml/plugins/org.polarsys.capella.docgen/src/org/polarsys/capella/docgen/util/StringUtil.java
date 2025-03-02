/*******************************************************************************
 * Copyright (c) 2006, 2021 THALES GLOBAL SERVICES.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 *  
 * Contributors:
 *   Thales - initial API and implementation
 ******************************************************************************/
package org.polarsys.capella.docgen.util;

import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.text.BadLocationException;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.URIUtil;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceImpl;
import org.eclipse.sirius.business.api.image.ImageManager;
import org.eclipse.sirius.business.api.session.resource.AirdResource;
import org.eclipse.sirius.diagram.DSemanticDiagram;
import org.polarsys.capella.common.data.modellingcore.AbstractNamedElement;
import org.polarsys.capella.core.data.capellamodeller.util.CapellamodellerResourceImpl;
import org.polarsys.capella.docgen.Activator;
import org.polarsys.capella.shared.id.handler.IScope;
import org.polarsys.capella.shared.id.handler.IdManager;
import org.polarsys.kitalpha.doc.gen.business.core.util.DocGenHtmlConstants;
import org.polarsys.kitalpha.doc.gen.business.core.util.DocGenHtmlUtil;
import org.polarsys.kitalpha.doc.gen.business.core.util.EObjectLabelProviderHelper;
import org.polarsys.kitalpha.doc.gen.business.core.util.LabelProviderHelper;

public class StringUtil {
	private static final String ELEMENT_LINK_REGEX = "hlink://(.+)";
	private static final String REGEX = "<[\\s]*[aA][\\s]+href[\\s]*=[\\s]*\"([^>]+)\"[\\s]*>(.*?)<[\\s]*/[\\s]*[aA][\\s]*>";
	private static final String REGEX_FILEPATH = "<[aA][\\s]+href=\"(.+?)\">";
	private static final String ERROR_CPY = "Error during project relative file copy: {0}";
	private static final String ERROR_COPY_PART_2 = " referenced in the description of the element: {0} "; //$NON-NLS-1$
	private static final String ERROR_DECODE_FILE_LOCATION = "Error while decoding file location: {0}";
	private static final String ERROR_READ_FILE_LOCATION = "Error while reading decoded file location: {0}";
	private static final String WARNING_IMAGE_SERIALIZATION_FAILED = "Image serialization failed, default to keep img serialized value for element if id: {0}";
	private static final String WARNING_NETWORK_IMAGE_NO_TREATMENT = "Network images are left as is and not copied to the generated documentation: {0}. Element id: {1}";
	private static final String ERROR_STRING_PATH_URI_UNENCODE = "String to URI unencode failed with string: {0}";
	private static final String IMAGES_FOLDER = "/images/";
	private static final String ERROR_HYPERLINK_OBJECT_UNREACHABLE = "Hyperlink to object {0} in {1} cannot be reached";

	/**
	 * Transform all links added in an model element description to Html links
	 * 
	 * @param eObject
	 *            model element for which documentation is about to be generated
	 * @param input
	 *            <code> eObject </code> description
	 * @param projectName
	 *            project name wherein documentation is generated
	 * @param outputFolder
	 *            folder name wherein documentation is generated
	 * @return model element description with all links transformed to html links
	 */
	public static String transformAREFString(EObject eObject, String input, String projectName, String outputFolder) {

		if (input != null) {
			/**
			 * Manage Links to Capella model element and Sirius diagrams Links are
			 * translated to HTML link to generated HTML page
			 **/
			input = manageModelAndDiagramElementsLinks(input, eObject);

			/**
			 * Manage Links to relative image path. Image is copied in a sub folder of
			 * output folder and link copy is used
			 **/
			input = manageImages(eObject, input, projectName, outputFolder);

			/**
			 * Manage links to relative files path Relative path will be translated to
			 * absolute path.
			 */
			input = manageFileLinks(eObject, input, projectName, outputFolder);

			return input;
		}
		return "";
	}

	protected static String getResourceCopyError(EObject element, String resourcePath) {
		StringBuilder result = new StringBuilder();
		if (element != null) {
			StringBuilder elementFQN = getElementFQN(element);
			String part1 = MessageFormat.format(ERROR_CPY, resourcePath);
			String part2 = MessageFormat.format(ERROR_COPY_PART_2, elementFQN);
			result.append(part1);
			result.append(part2);
		} else {
			result.insert(0, ERROR_CPY);
		}
		return result.toString();
	}

	private static StringBuilder getElementFQN(EObject element) {
		StringBuilder elementFQN = new StringBuilder();
		String elementLabel = LabelProviderHelper.getText(element);
		if (!EObjectLabelProviderHelper.EMPTY_STRING.equals(elementLabel)) {
			elementFQN.append(elementLabel);
			EObject currentElement = element;
			while (currentElement.eContainer() != null) {
				currentElement = currentElement.eContainer();
				elementFQN.insert(0, "::"); //$NON-NLS-1$
				elementFQN.insert(0, LabelProviderHelper.getText(currentElement));
			}
		}
		return elementFQN;
	}

	/**
	 * Transforms links to model elements or diagrams to html links. This links
	 * format is <code>hlink://Target ID/</code>.
	 * 
	 * @param input
	 *            <code> eObject </code> description
	 * @param eObject
	 *            model element for which documentation is about to be generated
	 * @return model element description (<code>input</code>) with links to model or
	 *         diagram elements are transformed to html links
	 */
	private static String manageModelAndDiagramElementsLinks(String input, EObject eObject) {
		Pattern pattern = Pattern.compile(REGEX, Pattern.DOTALL);
		Matcher matcher = pattern.matcher(input);

		while (matcher.find()) {
			if (matcher.groupCount() == 2) {
				final String akB = "<a href=\"";
				final String akE = "</a>";
				final String link = akB + switchToDocPath(matcher.group(1), eObject) + "\">"
						+ matcher.group(2) + akE;
				input = input.replace(matcher.group(0), link);
			}
		}
		return input;
	}

	/**
	 * Transform link to file using relative path to absolute path.
	 * 
	 * @param eObject
	 *            model element for which documentation is about to be generated
	 * @param input
	 *            <code> eObject </code> description
	 * @param projectName
	 *            project name wherein documentation is generated
	 * @param outputFolder
	 *            folder name wherein documentation is generated
	 * @return model element description (<code>input</code>) with file relative
	 *         paths transformed to file absolute paths
	 */
	private static String manageFileLinks(EObject eObject, String input, String projectName, String outputFolder) {
		if (input.trim().length() == 0) {
			return input;
		}

		Pattern pattern = Pattern.compile(REGEX_FILEPATH, Pattern.DOTALL);
		Matcher matcher = pattern.matcher(input);
		final IPath parentSrcFolder = new Path(eObject.eResource().getURI().segment(1));
		final IPath parentTargetFolderPath = new Path(projectName).append(outputFolder);
		while (matcher.find()) {
			// we check that we really have a link to a file (absolute or relative) and not
			// a link to URL, model element, diagram...
			String firstMatchGroup = matcher.group(1);
			if (firstMatchGroup.startsWith("file://") || firstMatchGroup.startsWith("local://")) {
				input = manageFileLink(eObject, input, parentSrcFolder, parentTargetFolderPath, firstMatchGroup);
			}
		}

		return input;
	}

	private static String manageFileLink(EObject eObject, String input, final IPath parentSrcFolder,
			final IPath parentTargetFolderPath, String firstMatchGroup) {
		final ILog logger = org.polarsys.capella.docgen.Activator.getDefault().getLog();

		String id = eObject.eGet(eObject.eClass().getEStructuralFeature("id")).toString();
		id = id.replace("-", "");
		
		// Use an inline creation of HTMLDocument to get correct html content
		String decodedFirstMatchGroup = decodeHtmlFilePath(firstMatchGroup, logger);
		if (decodedFirstMatchGroup == null) {
			return input;
		}

		IPath patha = new Path(decodedFirstMatchGroup.replaceAll("%20", " "));
		String iconName = id + "/" + patha.lastSegment();
		StringBuilder iconSourcePath = new StringBuilder();
		// if absolute path
		if (decodedFirstMatchGroup.startsWith("file://")) {
			for (String segment : patha.segments()) {
				iconSourcePath.append("/").append(segment);
			}
		} else {
			// relative path. We need to remove "local://" from matcher.group(1) => we
			// starts with character n�8
			IPath path = parentSrcFolder
					.append(decodedFirstMatchGroup.substring(8, decodedFirstMatchGroup.length()));
			IFile iconFile = ResourcesPlugin.getWorkspace().getRoot().getFile(path);
			if (iconFile.exists()) {
				iconSourcePath.append(iconFile.getLocationURI().getPath());
			} else {
				logger.log(new Status(IStatus.ERROR, Activator.PLUGIN_ID,
						getResourceCopyError(eObject, path.toString())));
				return input;
			}
		}

		IFolder parentTargetFolder = ResourcesPlugin.getWorkspace().getRoot().getFolder(parentTargetFolderPath);
		input = input.replace(firstMatchGroup, "./files/" + iconName + "\"");
		try {
			ImageHelper.INSTANCE.copyProjectImageToSystemLocation(iconSourcePath.toString(),
					parentTargetFolder.getLocationURI().getPath() + "/files/" + iconName);
		} catch (Exception e) {
			logger.log(new Status(IStatus.ERROR, Activator.PLUGIN_ID,
					getResourceCopyError(eObject, iconSourcePath.toString()), e));
		}
		return input;
	}

	/**
	 * Copy image linked by relative path to documentation generation folder and
	 * change it relative path to the new copy path
	 * 
	 * @param eObject
	 *            model element for which documentation is about to be generated
	 * @param input
	 *            <code> eObject </code> description
	 * @param projectName
	 *            project name wherein documentation is generated
	 * @param outputFolder
	 *            folder name wherein documentation is generated
	 * @return model element description (<code>input</code>) with image relative
	 *         paths transformed to local copy absolute paths
	 */
	private static String manageImages(EObject eObject, String input, String projectName, String outputFolder) {
		if (input.trim().isEmpty()) {
			return input;
		}
		// Pattern to match <img[...]> html content
		Pattern pattern = Pattern.compile(ImageManager.HTML_IMAGE_PATH_PATTERN, Pattern.DOTALL);
		Matcher matcher = pattern.matcher(input);

		final IPath parentSrcFolder = new Path(eObject.eResource().getURI().segment(1));
		final IPath parentTargetFolderPath = new Path(projectName).append(outputFolder);
		IFolder parentTargetFolder = ResourcesPlugin.getWorkspace().getRoot().getFolder(parentTargetFolderPath);

		// List used to store unique file names in the generation folder
		List<String> imageFileNames = new ArrayList<String>();
		final ILog logger = org.polarsys.capella.docgen.Activator.getDefault().getLog();
		String id = eObject.eGet(eObject.eClass().getEStructuralFeature("id")).toString();
		id = DocGenHtmlUtil.getValidFileName(id);

		while (matcher.find()) {
			if (matcher.groupCount() == 1) {
				String firstMatchGroup = matcher.group(1);
				// As we do a replace all, we may have already serialized/copied some files so
				// we check to avoid work duplication
				if (input.contains(firstMatchGroup)) {
					String iconName = "";
					boolean isDataImage = firstMatchGroup.startsWith(ImageHelper.DATA_IMAGE_PREFIX);
					if (isDataImage) {
						// Serialize image in target folder: "[dogen_output]/object_id/folder
						iconName = ImageHelper.INSTANCE.serializeImageInTargetFolder(firstMatchGroup,
								parentTargetFolder.getLocationURI().getPath() + IMAGES_FOLDER, id, imageFileNames,
								logger);
						if (iconName == null) {
							// Image serialization has failed, we will keep current image
							logger.log(new Status(IStatus.WARNING, Activator.PLUGIN_ID,
									MessageFormat.format(WARNING_IMAGE_SERIALIZATION_FAILED,
											eObject.eGet(eObject.eClass().getEStructuralFeature("id")).toString()),
									new Exception()));
						}
					} else {
						// Use an inline creation of HTMLDocument to get correct html content
						String decodedFirstMatchGroup = decodeHtmlFilePath(firstMatchGroup, logger);
						if (decodedFirstMatchGroup == null) {
							continue;
						}
						
						IPath patha = new Path(decodedFirstMatchGroup);
						String lastSegmentFileName = patha.removeFileExtension().lastSegment();
						iconName = id + "/" + lastSegmentFileName;
						String iconSourcePath = "";

						if (decodedFirstMatchGroup.startsWith("http") || decodedFirstMatchGroup.startsWith("https")) {
							logger.log(new Status(IStatus.WARNING, Activator.PLUGIN_ID,
									MessageFormat.format(WARNING_NETWORK_IMAGE_NO_TREATMENT, decodedFirstMatchGroup,
											eObject.eGet(eObject.eClass().getEStructuralFeature("id")).toString())));
							continue;
						}
						if (decodedFirstMatchGroup.startsWith("//") || decodedFirstMatchGroup.startsWith("\\\\")) {
							// Absolute path may be serialized with %20 string for spaces
							// We should thus decode them for the upcoming copy
							iconSourcePath = unencodeURIString(decodedFirstMatchGroup, logger);
							iconName = unencodeURIString(iconName, logger);
						} else if (patha.isAbsolute()) {
							for (String segment : patha.removeFileExtension().segments()) {
								iconSourcePath += "/" + segment;
							}
							iconSourcePath += "." + patha.getFileExtension();
							// Absolute path may be serialized with %20 string for spaces
							// We should thus decode them for the upcoming copy
							iconSourcePath = unencodeURIString(iconSourcePath, logger);
							iconName = unencodeURIString(iconName, logger);
						} else {
						    IPath path = new Path(decodedFirstMatchGroup);
						    // Check if the path is a workspace path
                            IFile iconFile = ResourcesPlugin.getWorkspace().getRoot().getFile(path);
						    if (!iconFile.exists()) {
						        path = parentSrcFolder.append(decodedFirstMatchGroup);
						        iconFile = ResourcesPlugin.getWorkspace().getRoot().getFile(path);
						    }
							if (iconFile.exists()) {
								iconSourcePath = iconFile.getLocationURI().getPath();
							} else {
								logger.log(new Status(IStatus.ERROR, Activator.PLUGIN_ID,
										getResourceCopyError(eObject, path.toString())));
								continue;
							}
						}
						// Ensure unique file name
						iconName = ImageHelper.INSTANCE.getUniqueFileName(iconName, imageFileNames);
						iconName += "." + patha.getFileExtension();

						try {
							// Copy file
							ImageHelper.INSTANCE.copyProjectImageToSystemLocation(iconSourcePath,
									parentTargetFolder.getLocationURI().getPath() + IMAGES_FOLDER + iconName);
						} catch (Exception e) {
							logger.log(new Status(IStatus.ERROR, Activator.PLUGIN_ID,
									getResourceCopyError(eObject, iconSourcePath), e));
						}
					}

					input = input.replace(firstMatchGroup, "." + IMAGES_FOLDER + iconName);
				}
			}
		}
		return input;
	}

	/**
	 * Method to unencode a String that will be used as URI to a file. Removes %20
	 * from string.
	 * 
	 * @param stringToUnencode
	 * @param logger
	 * @return
	 */
	private static String unencodeURIString(String stringToUnencode, ILog logger) {
		URI uri;
		try {
			uri = new URI(stringToUnencode);
			stringToUnencode = URIUtil.toUnencodedString(uri);
		} catch (URISyntaxException e) {
			logger.log(new Status(IStatus.WARNING, Activator.PLUGIN_ID,
					MessageFormat.format(ERROR_STRING_PATH_URI_UNENCODE, stringToUnencode), e));
		}
		return stringToUnencode;
	}

	/**
	 * File path being taken from an HTML document may be encoded. It may thus be
	 * necessary to decode this content if we need to get real file paths for
	 * examples.
	 * 
	 * @param filePath
	 *            The file path to decode.
	 * @param loger
	 *            An ILog instance for exceptions management.
	 * @return A decoded String.
	 */
	private static String decodeHtmlFilePath(String filePath, ILog logger) {
		try {
			HTMLDocument doc = new HTMLDocument();
			new HTMLEditorKit().read(new StringReader("<html><body>" + filePath), doc, 0);
			String decodedFirstMatchGroup = doc.getText(1, doc.getLength()).trim();
			return decodedFirstMatchGroup;
		} catch (BadLocationException e) {
			logger.log(new Status(IStatus.ERROR, Activator.PLUGIN_ID,
					MessageFormat.format(ERROR_DECODE_FILE_LOCATION, filePath), e));
		} catch (IOException e) {
			logger.log(new Status(IStatus.ERROR, Activator.PLUGIN_ID,
					MessageFormat.format(ERROR_READ_FILE_LOCATION, filePath), e));
		}
		return null;
	}

	/**
	 * Manage a link to a Model element or a diagram
	 * 
	 * @param group
	 *            text containing a link to handle
	 * @param object
	 *            capella object
	 * @return <code>group</code> with links to model elements replaced by html
	 *         links
	 */
	private static String switchToDocPath(String group, EObject object) {
		/**
		 * Handle links to model or diagram elements
		 */
		Pattern pattern = Pattern.compile(ELEMENT_LINK_REGEX);
		Matcher matcher = pattern.matcher(group);
		String id = null; 
		
		if (matcher.find() && matcher.groupCount() == 1) {
			id = matcher.group(1);
			if (id != null && id.trim().length() > 0 && id.contains("/")) {
				id = id.substring(0, id.length() - 1);
			}
		}
		
		if (id != null) {
			return buildResourceRepresentationString(object, id);
		}

		return group;
	}

	private static String buildResourceRepresentationString(EObject object, String id) {
		StringBuilder stringBuilder = new StringBuilder();
		Resource resource = object.eResource();
		EObject eObject = resource.getEObject(id);
		// If the object is not found in the current resource, we look for
		// it in all loaded resources of the ResourceSet
		if (eObject == null) {
			ResourceSet rs = resource.getResourceSet();
			final String searchId = id;
			eObject = rs.getResources().stream().filter(iResource -> {
				EObject eObj = null;
				if (iResource instanceof CapellamodellerResourceImpl || iResource instanceof AirdResource) {
					eObj = iResource.getEObject(searchId);
				}
				return eObj != null;
			}).findAny().orElse(new ResourceImpl()).getEObject(id);
		}

		// If the object is still not found, so we use the Capella API.
		if (eObject == null) {
			final ResourceSet rs = resource.getResourceSet();
			eObject = IdManager.getInstance().getEObject(id, new IScope() {
				@Override
				public List<Resource> getResources() {
					return rs.getResources();
				}
			});
		}

		if (eObject != null) {
    		if (eObject instanceof DSemanticDiagram) {
    			stringBuilder.append(CapellaServices.getPathFromElement(((DSemanticDiagram) eObject).getTarget()));
    			stringBuilder.append("#");
    			stringBuilder.append(CapellaServices.getDiagramUid((DSemanticDiagram) eObject));
    		} else {
    			stringBuilder.append(CapellaServices.getPathFromElement(eObject));
    		} 
		} else {
		    final ILog logger = org.polarsys.capella.docgen.Activator.getDefault().getLog();
		    StringBuilder elementLabel = getElementFQN(object);
		    logger.log(new Status(IStatus.ERROR, Activator.PLUGIN_ID, MessageFormat.format(ERROR_HYPERLINK_OBJECT_UNREACHABLE, id, elementLabel)));
		}
		return stringBuilder.toString();
	}

	protected static String getPath(Resource resource, String id) {
		String newID = id.replaceAll("#", "");
		EObject eObject = resource.getEObject(newID);
		if (eObject != null) {
			String fileName = DocGenHtmlCapellaUtil.SERVICE.getFileName(eObject);
			String modelName = DocGenHtmlUtil.getModelName(eObject);
			return "../" + modelName + "/" + DocGenHtmlUtil.getValidFileName(fileName) + "."
					+ DocGenHtmlConstants.HTML_FILE_EXTENSION;
		}
		return "#";
	}

	/**
	 * Transform a list of string to html bullet list
	 * <p>
	 * Transform a List of string to a string containing the html bullet list
	 * </p>
	 * 
	 * @param list_p
	 * @return
	 */
	public static String stringListToBulette(Collection<String> list_p) {
		// Buffer declaration
		StringBuilder stringBuilder = new StringBuilder();
		if (list_p.isEmpty()) {
			stringBuilder.append(CapellaServices.NONE);
		} else {
			stringBuilder.append(CapellaServices.UL_OPEN);
			// For each element of the list
			for (String str : list_p) {
				buletteItem(stringBuilder, str);
			}
			stringBuilder.append(CapellaServices.UL_CLOSE);
		}
		// Return the buffer
		return stringBuilder.toString();
	}

	public static String stringListToBulette(EList<? extends AbstractNamedElement> list_elts, String projectName,
			String outputFolder) {
		// Buffer declaration
		StringBuilder stringBuilder = new StringBuilder();
		if (list_elts.isEmpty()) {
			stringBuilder.append(CapellaServices.NONE);
		} else {
			stringBuilder.append(CapellaServices.UL_OPEN);
			// For each element of the list
			for (AbstractNamedElement elt : list_elts) {
				// Add the open bullet tag to the buffer
				stringBuilder.append(CapellaServices.LI_OPEN);
				// Add the content of the bullet to the buffer
				stringBuilder.append(CapellaServices.getImageLinkFromElement(elt, projectName, outputFolder));
				stringBuilder.append(CapellaServices.SPACE);
				stringBuilder.append(CapellaServices.getHyperlinkFromElement(elt));
				// Add the close bullet tag to the buffer
				stringBuilder.append(CapellaServices.LI_CLOSE);
			}
			stringBuilder.append(CapellaServices.UL_CLOSE);
		}
		// Return the buffer
		return stringBuilder.toString();
	}

	private static void buletteItem(StringBuilder buffer, String str) {
		// Add the open bullet tag to the buffer
		buffer.append(CapellaServices.LI_OPEN);
		// Add the content of the bullet to the buffer
		buffer.append(str);
		// Add the close bullet tag to the buffer
		buffer.append(CapellaServices.LI_CLOSE);
	}

	/**
	 * Build a 2 columns html table. The first table contains the keys of the map
	 * and the second contains the values of the map
	 * 
	 * @param map
	 *            data of the table
	 * @param firstColTitle
	 *            the title of the first column
	 * @param secondColTitle
	 *            the title of the second column
	 * @return html table string
	 */
	public static String mapToHTMLTable(Map<String, String> map, String firstColTitle, String secondColTitle) {

		// To generate header one of the first or second title must be set
		boolean generateTableHeader = (firstColTitle != null && !firstColTitle.isEmpty())
				|| (secondColTitle != null && !secondColTitle.isEmpty());

		firstColTitle = firstColTitle == null ? "" : firstColTitle;
		secondColTitle = secondColTitle == null ? "" : secondColTitle;

		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append("<table>"); //$NON-NLS-1$
		if (generateTableHeader) {
			stringBuilder.append("<tr>").append("<th>"); //$NON-NLS-1$ //$NON-NLS-2$
			stringBuilder.append(firstColTitle).append("</th>"); // End first col //$NON-NLS-1$

			stringBuilder.append("<th>"); //$NON-NLS-1$
			stringBuilder.append(secondColTitle);
			stringBuilder.append("</th></tr>"); // End second col and the first row //$NON-NLS-1$
		}

		// Generate the table content
		for (Entry<String, String> e : map.entrySet()) {
			stringBuilder.append("<tr>"); //$NON-NLS-1$
			stringBuilder.append("<td>").append(e.getKey()).append("</td><td>").append(e.getValue()).append("</td>"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			stringBuilder.append("</tr>"); //$NON-NLS-1$
		}
		stringBuilder.append("</table>"); //$NON-NLS-1$

		return stringBuilder.toString();
	}

	/**
	 * @param stringSet
	 *            must be not null
	 * @return true if the stringSet contains a not empty or null value string,
	 *         otherwise false
	 */
	public static boolean containsNotEmptyString(Collection<String> stringSet) {

		for (String string : stringSet) {
			if (string != null && !string.isEmpty()) {
				return true;
			}
		}
		return false;
	}
}
