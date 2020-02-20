/**
 * Copyright (c) 2020 Source Auditor Inc.
 *
 * SPDX-License-Identifier: Apache-2.0
 * 
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package org.spdx.jsonstore;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;

import org.spdx.jsonstore.MultiFormatStore.Format;
import org.spdx.jsonstore.MultiFormatStore.Verbose;
import org.spdx.library.InvalidSPDXAnalysisException;
import org.spdx.library.model.SpdxDocument;
import org.spdx.library.model.SpdxModelFactory;
import org.spdx.library.model.SpdxSnippet;
import org.spdx.library.model.pointer.SinglePointer;
import org.spdx.library.model.pointer.StartEndPointer;
import org.spdx.utility.compare.SpdxCompareException;
import org.spdx.utility.compare.SpdxComparer;

import junit.framework.TestCase;

/**
 * @author Gary O'Neall
 *
 */
public class MultiFormatStoreTest extends TestCase {
	
	static final String JSON_FILE_PATH = "testResources" + File.separator + "SPDXJSONExample-v2.0.json";

	/* (non-Javadoc)
	 * @see junit.framework.TestCase#setUp()
	 */
	protected void setUp() throws Exception {
		super.setUp();
	}

	/* (non-Javadoc)
	 * @see junit.framework.TestCase#tearDown()
	 */
	protected void tearDown() throws Exception {
		super.tearDown();
	}

	/**
	 * Test method for {@link org.spdx.jsonstore.MultiFormatStore#serialize(java.lang.String, java.io.OutputStream)} and {@link org.spdx.jsonstore.MultiFormatStore#deSerialize(java.io.InputStream)}.
	 * @throws IOException 
	 * @throws InvalidSPDXAnalysisException 
	 * @throws SpdxCompareException 
	 */
	public void testDeSerializeSerializeJson() throws InvalidSPDXAnalysisException, IOException, SpdxCompareException {
		File jsonFile = new File(JSON_FILE_PATH);
		// Compact
		MultiFormatStore inputStore = new MultiFormatStore(Format.JSON_PRETTY);
		try (InputStream input = new FileInputStream(jsonFile)) {
			inputStore.deSerialize(input, false);
		}
		String documentUri = inputStore.getDocumentUris().get(0);
		SpdxDocument inputDocument = new SpdxDocument(inputStore, documentUri, null, false);
		List<String> verify = inputDocument.verify();
		assertEquals(0, verify.size());
		// test Overwrite
		try (InputStream input = new FileInputStream(jsonFile)) {
			try {
				inputStore.deSerialize(input, false);
				fail("Input was overwritten when overwrite was set to false");
			} catch(InvalidSPDXAnalysisException ex) {
				// expected
			}
		}
		// Deserialize
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		inputStore.serialize(documentUri, outputStream);
		ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
		MultiFormatStore outputStore = new MultiFormatStore(Format.JSON_PRETTY);
		outputStore.deSerialize(inputStream, false);
		SpdxDocument compareDocument = new SpdxDocument(outputStore, documentUri, null, false);
		verify = inputDocument.verify();
		assertEquals(0, verify.size());
		verify = compareDocument.verify();
		assertEquals(0, verify.size());
		SpdxModelFactory.getElements(inputStore, documentUri, null, SpdxSnippet.class).forEach(element -> {
			SpdxSnippet snippet = (SpdxSnippet)element;
			try {
				SinglePointer sp1 = snippet.getByteRange().getStartPointer();
				SinglePointer sp2 = snippet.getByteRange().getEndPointer();
				Optional<StartEndPointer> sep = snippet.getLineRange();
				SinglePointer sp3 = null;
				SinglePointer sp4 = null;
				if (sep.isPresent()) {
					sp3 = sep.get().getStartPointer();
					sp4 = sep.get().getEndPointer();
				}
				List<String> sVerify = snippet.verify();
//				assertEquals(0, sVerify.size());
			} catch (InvalidSPDXAnalysisException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		});
		SpdxComparer comparer = new SpdxComparer();
		comparer.compare(inputDocument, compareDocument);
		assertFalse(comparer.isDifferenceFound());
		assertTrue(inputDocument.equivalent(compareDocument));
	}

}