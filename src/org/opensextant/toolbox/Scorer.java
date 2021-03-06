/**
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
package org.opensextant.toolbox;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.opensextant.placedata.Geocoord;
import org.opensextant.placedata.Place;
import org.opensextant.placedata.PlaceCandidate;
import org.opensextant.placedata.PlaceEvidence;

/**
 * The Scorer attempts to quantify (using a range between -1.0 and 1.0) the
 * similarity between a Place Candidate which has been found in a document with
 * matches from the gazetteer. The score consists of a weighted average of four
 * one dimensional similarity measurements:
 * <ul>
 * <li>The similarity between the name in the document and the name in the
 * gazetteer</li>
 * <li>The geometric distance between a geocoord associated with the name in the
 * document and the geocoordinate of the name in the gazetteer</li>
 * <li>The similarity/compatibility of feature type information associated with
 * the name in the document and the type of the feature in the gazetteer.</li>
 * <li>The similarity/compatibility between the admin structure associated with
 * the name in the document and and the structure of the feature in the
 * gazetteer.</li>
 * </ul>
 */
public class Scorer {
	// TODO Need deterministic technique to estimate weights
	// weight of apriori info
	double biasWeight = 0.1;
	// weights of the four evidence information types
	double nameWeight = 0.01;
	double geocoordWeight = 1.90;
	double featureTypeWeight = 0.5;
	double adminStructureWeight = 1.5;
	// the sum of the component weights (for normalizing the total score)
	double totalWeight = nameWeight + geocoordWeight + featureTypeWeight + adminStructureWeight;
	// document level country and admin1 evidence
	List<PlaceEvidence> docEvidList = new ArrayList<PlaceEvidence>();
	// the minimum value returned by scoreEvidence to avoid multiplying
	// everything by zero
	static final double MIN_EVIDENCE = .00001;
	// a flag value to indicate no evidence was found
	static final String NO_EVIDENCE = "NO_EVIDENCE";
	// name similarity scores
	static final double NAME_SIMILARITY_CASEINSENS = 0.90;
	static final double NAME_SIMILARITY_FLOOR = 0.50;
	// geocoord similarity thresholds and score
	double geoInnerDist = 55.0; // kilometers
	double geoOuterDist = 280.0; // kilometers
	double geoNearScore = 1.0;
	double geoMidScore = 0.5;
	double geoFarScore = -1.0;
	// feature class/code similarity scores
	double featureTypeClassScore = 0.8;
	double featureTypeConfusedScore = 0.2;

	static Pattern adminLikePattern = Pattern.compile("AdminRegion|Area|PopulatedPlace");
	static Pattern adminPattern = Pattern.compile("AdminRegion");
	static Pattern spotPattern = Pattern.compile("SpotFeature");
	static Pattern geoPrefixPattern = Pattern.compile("^Geo.featureType.");

	// admin structure similarity scores
	double countryOnlyScore = 0.9;
	double countryDifferentAdminScore = .75;

	/**
	 * Score each of the place candidates
	 * 
	 * @param pcList
	 */
	public void score(List<PlaceCandidate> pcList) {
		// for each PC in the list
		for (PlaceCandidate pc : pcList) {
			// for each Place on the PC
			for (Place p : pc.getPlaces()) {
				double prior = biasWeight * p.getIdBias();
				double evidenceScore = scoreEvidence(p, pc.getEvidence());
				double totalScore = (prior + evidenceScore) / 2.0;
				pc.setPlaceScore(p, totalScore);
			}
		}
		// empty out the document evidence list
		this.docEvidList.clear();
	}

	/**
	 * calculate the total score of a place versus all the evidence
	 * 
	 * @param p
	 * @param evidList
	 * @return
	 */
	private double scoreEvidence(Place p, List<PlaceEvidence> evidList) {
		double prodScore = 1.0;
		double sumScore = 0.0;
		// TODO if there is evidence but no country and admin evidence
		// use document level evidence
		// if no evidence from candidate, use document level evidence

		List<PlaceEvidence> tmpEvidList = evidList;
		if (evidList.isEmpty()) {
			if (!docEvidList.isEmpty()) {

				tmpEvidList = docEvidList;
			} else {
				// no local or document level evidence
				return 0.0;
			}
		}
		// for each bit of evidence
		for (PlaceEvidence ev : tmpEvidList) {
			// compare the evidence to the place
			double singleScore = score(ev, p);
			// running product of the scores
			prodScore = prodScore * singleScore;
			// running sum of the scores
			sumScore = sumScore + singleScore;
		}
		return sumScore / tmpEvidList.size();
	}

	/**
	 * The scoring function which calculates the similarity between a place and
	 * a single piece of evidence
	 * 
	 * @param ev
	 * @param place
	 * @return
	 */
	private double score(PlaceEvidence ev, Place place) {
		double nameScore = scoreName(ev, place);
		double geocoordScore = scoreGeocoord(ev, place);
		double featureTypeScore = scoreFeatureType(ev, place);
		double adminStructureScore = scoreAdminStructure(ev, place);
		// calculate the total score from the weighted component scores

		return (nameWeight * nameScore + geocoordWeight * geocoordScore + featureTypeWeight * featureTypeScore
				+ adminStructureWeight * adminStructureScore) / totalWeight;
	}

	/* ---------------------Name Similarity ------------------ */
	/**
	 * Compare the name as found in the document with the name in the candidate
	 * as found in the gazetteer Exact match =1.0 Exact match (ignoring case) =
	 * <NAME_SIMILARITY_CASEINSENS> .... Any condition not caught in the above =
	 * <NAME_SIMILARITY_FLOOR> Range = 0.0 -> 1.0 (no negative score)
	 */
	private double scoreName(PlaceEvidence evidence, Place place) {
		// the two names to be compared
		String evidenceName = evidence.getPlaceName();
		String gazetteerName = place.getPlaceName();
		double weight = evidence.getWeight();
		if (evidenceName == null) {
			return 0.0;
		}
		// full credit for exact match
		if (evidenceName.equals(gazetteerName)) {
			return 1.0 * weight;
		}
		// partial credit for case differences
		if (evidenceName.equalsIgnoreCase(gazetteerName)) {
			return NAME_SIMILARITY_CASEINSENS * weight;
		}
		// TODO other similarity conditions between case insensitive and the
		// floor? Diacritics? other phonetic?
		// floor value
		if (weight > 0.0) {
			return NAME_SIMILARITY_FLOOR * weight;
		} else {
			return MIN_EVIDENCE; // no opinion on double negative
		}
	}

	/* ---------------------Geocoord Similarity ------------------ */
	/**
	 * Compare the geocoord evidence from the document with the location
	 * (geocoord) of the candidate as found in the gazetteer. Comparison is
	 * based on dist between evidence and gazetteer place. Range = -1.0 -> 1.0
	 * NOTE: these thresholds really should be modulated by FeatureType rather
	 * than be absolutes
	 * 
	 * @param evidence
	 * @param place
	 * @return
	 */
	private double scoreGeocoord(PlaceEvidence evidence, Place place) {
		// the two geocoords to compare
		Geocoord evidenceGeo = evidence.getGeocoord();
		Geocoord gazetteerGeo = place.getGeocoord();
		double weight = evidence.getWeight();
		// no evidence zero score
		if (evidenceGeo == null) {
			return MIN_EVIDENCE;
		}
		// distance between candidate and evidence (in kms)
		double dist = gazetteerGeo.distance(evidenceGeo);
		// non-null coords, check the thresholds
		// if near
		if (dist < geoInnerDist) {
			return geoNearScore * weight;
		}
		// if mid range
		if (dist > geoInnerDist && dist < geoOuterDist) {
			return geoMidScore * weight;
		}
		// must be far away
		return MIN_EVIDENCE; // no opinion on double negative
	}

	/* ---------------------Feature Type Similarity ------------------ */
	/**
	 * Compare the similarity/compatibility of the feature type evidence as
	 * found in the document with the feature type info from the place in the
	 * gazetteer. Range = -1.0 <-> 1.0
	 * 
	 * @param evidence
	 * @param place
	 * @return
	 */
	private double scoreFeatureType(PlaceEvidence evidence, Place place) {

		// get the two sets of feature type info (code and class)
		String evidenceFClass = evidence.getFeatureClass();
		if (evidenceFClass != null) {
			evidenceFClass = geoPrefixPattern.matcher(evidenceFClass).replaceFirst("");

		}
		String evidenceFCode = evidence.getFeatureCode();
		double weight = evidence.getWeight();

		String gazetteerFClass = place.getFeatureClass();
		if (gazetteerFClass != null) {
			gazetteerFClass = geoPrefixPattern.matcher(gazetteerFClass).replaceFirst("");
		}
		String gazetteerFCode = place.getFeatureCode();
		// no evidence, zero score
		if (evidenceFClass == null) {
			return MIN_EVIDENCE;
		}
		if (evidenceFCode == null) {
			evidenceFCode = NO_EVIDENCE;
		}
		// perfect match
		if (evidenceFClass.equals(gazetteerFClass) && evidenceFCode.equals(gazetteerFCode)) {
			return 1.0 * weight;
		}
		// class level match, no code level info
		if (evidenceFClass.equals(gazetteerFClass) && NO_EVIDENCE.equals(evidenceFCode)) {
			return featureTypeClassScore * weight;
		}
		// partial credit for the commonly confused classes of A,L and P
		if (adminLikePattern.matcher(evidenceFClass).matches() && adminLikePattern.matcher(gazetteerFClass).matches()) {
			return featureTypeConfusedScore * weight;
		}
		// partial credit for places used to describe spot features e.g.
		// "the Washington office"
		if (spotPattern.matcher(evidenceFClass).matches() && adminLikePattern.matcher(gazetteerFClass).matches()) {
			return featureTypeConfusedScore * weight;
		}

		// floor value for "important" gazetteer entries
		if (adminPattern.matcher(gazetteerFClass).matches()) {
			return featureTypeConfusedScore * weight;
		}
		// must be incompatible class/code
		if (weight > 0.0) {
			return MIN_EVIDENCE;
		} else {
			return MIN_EVIDENCE; // no opinion on double negative
		}
	}

	/* ---------------------Admin Structure Similarity ------------------ */
	/**
	 * Compare the similarity/consistency of the administrative structure
	 * evidence found in the document with that of the place from the gazetteer.
	 * Range = -1.0 -> 1.0
	 * 
	 * @param evidence
	 * @param place
	 * @return
	 */
	private double scoreAdminStructure(PlaceEvidence evidence, Place place) {
		// get the two sets of admin info (country and admin1)
		String evidenceCountry = evidence.getCountryCode();
		String evidenceAdm1 = evidence.getAdmin1();
		String gazetteerCountry = place.getCountryCode();
		String gazetteerAdm1 = place.getAdmin1();
		double weight = evidence.getWeight();
		// no evidence, zero score
		if (evidenceCountry == null) {
			return MIN_EVIDENCE;
		}
		if (evidenceAdm1 == null) {
			evidenceAdm1 = NO_EVIDENCE;
		}
		// perfect match
		if (evidenceCountry.equals(gazetteerCountry) && evidenceAdm1.equals(gazetteerAdm1)) {
			return 1.0 * weight;
		}
		// country match, no admin1 evidence
		if (evidenceCountry.equals(gazetteerCountry) && NO_EVIDENCE.equals(evidenceAdm1)) {
			return countryOnlyScore * weight;
		}
		// country match, incompatible admin1 evidence
		if (evidenceCountry.equals(gazetteerCountry) && !evidenceAdm1.equals(gazetteerAdm1)) {
			return countryDifferentAdminScore * weight;
		}
		// there should never be a case where the adm1 is correct but the
		// country is not
		// must be incompatible
		if (weight > 0.0) {
			return MIN_EVIDENCE;
		} else {
			return MIN_EVIDENCE; // no opinion on double negative
		}
	}

	public void setDocumentLevelEvidence(List<PlaceEvidence> docEvidList) {
		this.docEvidList = docEvidList;
	}
}
