/*
 Copyright 2009-2013 The MITRE Corporation.
   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at
       http://www.apache.org/licenses/LICENSE-2.0
   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 * **************************************************************************
 *                          NOTICE
 * This software was produced for the U. S. Government under Contract No.
 * W15P7T-12-C-F600, and is subject to the Rights in Noncommercial Computer
 * Software and Noncommercial Computer Software Documentation Clause
 * 252.227-7014 (JUN 1995)
 *
 * (c) 2012 The MITRE Corporation. All Rights Reserved.
 * **************************************************************************
 **/
package org.opensextant.regex.geo;

import java.util.Comparator;

import org.opensextant.regex.PostProcessorBase;
import org.opensextant.regex.RegexAnnotation;

/**
 * The Class GeoPostProcessor.<br>
 * This class extends PostProcessorBase to provide a PostProcessor that selects
 * the "best" Geocoord annotation, where "best" means the Geocoord annotation
 * with the most likely interpretation as determined by the GeocoordComparator .
 */
public class GeoPostProcessor extends PostProcessorBase {

	@Override
	public Comparator<RegexAnnotation> getComparator() {
		return new GeocoordComparator();
	}

}
