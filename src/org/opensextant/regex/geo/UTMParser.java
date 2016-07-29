/*
 *
 *  Copyright 2009-2013 The MITRE Corporation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * **************************************************************************
 *                          NOTICE
 * This software was produced for the U. S. Government under Contract No.
 * W15P7T-12-C-F600, and is subject to the Rights in Noncommercial Computer
 * Software and Noncommercial Computer Software Documentation Clause
 * 252.227-7014 (JUN 1995)
 *
 * (c) 2012 The MITRE Corporation. All Rights Reserved.
 * **************************************************************************
 */
package org.opensextant.regex.geo;

import java.util.Map;

import org.opensextant.geodesy.UTM;

/**
 * *.
 * 
 * @author ubaldino
 */
public class UTMParser {

	/**
	 * Log object private static final Logger LOGGER =
	 * LoggerFactory.getLogger(UTMParser.class);
	 */
	public static final char UTM_NORTH = 'N';
	public static final char UTM_SOUTH = 'S';

	private UTMParser() {
	}

	public static UTM parseUTM(Map<String, String> elements) {

		String z = elements.get("UTMZone");
		String z1 = elements.get("UTMZoneZZ"); // 0-5\d
		String z2 = elements.get("UTMZoneZ"); // \d
		if (z == null) {
			z = z1 != null ? z1 : z2;
		}
		if (z == null) {
			return null;
		}
		int zz = Integer.parseInt(z);
		String b = elements.get("UTMBand");
		if (b == null) {
			return null;
		}
		// TODO: is 'n' valid for UTM band?
		char h = b.charAt(0);
		if (h != UTM_NORTH && h != UTM_SOUTH) {
			h = UTM.getHemisphere(h);
		}
		String easting = elements.get("UTMEasting");
		String northing = elements.get("UTMNorthing");
		Integer eastingValue = Integer.parseInt(easting);
		Integer northingValue = Integer.parseInt(northing);
		return new UTM(zz, h, eastingValue.doubleValue(), northingValue.doubleValue());
	}
}
