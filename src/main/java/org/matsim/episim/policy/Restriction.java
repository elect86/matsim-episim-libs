package org.matsim.episim.policy;

import com.typesafe.config.Config;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.episim.model.FaceMask;

import java.util.HashMap;
import java.util.Map;

/**
 * Represent the current restrictions on an activity type.
 */
public final class Restriction {

	private static final Logger log = LogManager.getLogger(Restriction.class);

	/**
	 * Percentage of activities still performed.
	 * Not defined if NaN.
	 */
	private double remainingFraction;

	/**
	 * Exposure during this activity.
	 * Not defined if NaN.
	 */
	private double exposure;

	/**
	 * Persons are required to wear a mask with this or more effective type.
	 * Not defined if null.
	 */
	private FaceMask requireMask;

	/**
	 * Compliance rate for masks. Overwrites global parameter if set.
	 * Can be NaN to be undefined.
	 */
	private double complianceRate;

	/**
	 * Constructor.
	 */
	private Restriction(double remainingFraction, double exposure, FaceMask requireMask, double complianceRate) {
		if (remainingFraction < 0 || remainingFraction > 1)
			throw new IllegalArgumentException("remainingFraction must be between 0 and 1 but is=" + remainingFraction);
		if (exposure < 0)
			throw new IllegalArgumentException("exposure must be larger than 0, but is=" + exposure);

		this.remainingFraction = remainingFraction;
		this.exposure = exposure;
		this.requireMask = requireMask;
		this.complianceRate = complianceRate;
	}

	/**
	 * Restriction that allows everything.
	 */
	public static Restriction none() {
		return new Restriction(1d, 1d, FaceMask.NONE, Double.NaN);
	}

	/**
	 * Restriction only reducing the {@link #remainingFraction}.
	 */
	public static Restriction of(double remainingFraction) {
		return new Restriction(remainingFraction, Double.NaN, null, Double.NaN);
	}

	/**
	 * See {@link #of(double, double, FaceMask)}.
	 */
	public static Restriction of(double remainingFraction, FaceMask mask) {
		return new Restriction(remainingFraction, Double.NaN, mask, Double.NaN);
	}

	/**
	 * Instantiate a restriction.
	 */
	public static Restriction of(double remainingFraction, double exposure, FaceMask mask) {
		return new Restriction(remainingFraction, exposure, mask, Double.NaN);
	}

	/**
	 * Creates a restriction with required mask.
	 */
	public static Restriction ofMask(FaceMask mask) {
		return new Restriction(Double.NaN, Double.NaN, mask, Double.NaN);
	}

	/**
	 * Creates a restriction with required mask and compliance rate.
	 */
	public static Restriction ofMask(FaceMask mask, double complianceRate) {
		return new Restriction(Double.NaN, Double.NaN, mask, complianceRate);
	}

	/**
	 * Creates a restriction with only exposure set.
	 */
	public static Restriction ofExposure(double exposure) {
		return new Restriction(Double.NaN, exposure, null, Double.NaN);
	}


	/**
	 * Creates a restriction from a config entry.
	 */
	public static Restriction fromConfig(Config config) {
		return new Restriction(
				config.getDouble("fraction"),
				config.getDouble("exposure"),
				config.getIsNull("mask") ? null : config.getEnum(FaceMask.class, "mask"),
				config.getDouble("compliance")
		);
	}

	/**
	 * Creates a copy of a restriction.
	 */
	static Restriction clone(Restriction restriction) {
		return new Restriction(restriction.remainingFraction, restriction.exposure, restriction.requireMask, restriction.complianceRate);
	}


	/**
	 * This method is also used to write the restriction to csv.
	 */
	@Override
	public String toString() {
		return String.format("%.2f_%s", remainingFraction, requireMask);
	}

	/**
	 * Set restriction values from other restriction update.
	 */
	void update(Restriction r) {
		// All values may be optional and are only set if present
		if (!Double.isNaN(r.getRemainingFraction()))
			setRemainingFraction(r.getRemainingFraction());

		if (!Double.isNaN(r.getExposure()))
			setExposure(r.getExposure());

		if (r.getRequireMask() != null)
			setRequireMask(r.getRequireMask());

		if (!Double.isNaN(r.getComplianceRate()))
			setComplianceRate(r.getComplianceRate());

	}

	/**
	 * Merges another restrictions into this one. Will fail if any attribute would be overwritten.
	 *
	 * @see #asMap()
	 */
	Restriction merge(Map<String, Object> r) {

		double otherRf = (double) r.get("fraction");
		double otherE = (double) r.get("exposure");
		double otherComp = (double) r.get("compliance");
		FaceMask otherMask = r.get("mask") == null ? null : FaceMask.valueOf((String) r.get("mask"));

		if (!Double.isNaN(remainingFraction) && !Double.isNaN(otherRf) && remainingFraction != otherRf)
			log.warn("Duplicated remainingFraction " + remainingFraction + " and " + otherRf);
		else if (Double.isNaN(remainingFraction))
			remainingFraction = otherRf;

		if (!Double.isNaN(exposure) && !Double.isNaN(otherE) && exposure != otherE)
			log.warn("Duplicated exposure " + exposure + " and " + otherE);
		else if (Double.isNaN(exposure))
			exposure = otherE;

		if (requireMask != null && otherMask != null && requireMask != otherMask)
			log.warn("Duplicated mask " + requireMask + " and " + otherMask);
		else if (requireMask == null)
			requireMask = otherMask;

		if (!Double.isNaN(complianceRate) && !Double.isNaN(otherComp) && complianceRate != otherComp)
			log.warn("Duplicated complianceRate " + complianceRate + " and " + otherComp);
		else if (Double.isNaN(complianceRate))
			complianceRate = otherComp;

		return this;
	}

	public double getRemainingFraction() {
		return remainingFraction;
	}

	void setRemainingFraction(double remainingFraction) {
		this.remainingFraction = remainingFraction;
	}

	public double getExposure() {
		return exposure;
	}

	public void setExposure(double exposure) {
		this.exposure = exposure;
	}

	public FaceMask getRequireMask() {
		return requireMask;
	}

	public void setRequireMask(FaceMask requireMask) {
		this.requireMask = requireMask;
	}

	public double getComplianceRate() {
		return complianceRate;
	}

	void setComplianceRate(double complianceRate) {
		this.complianceRate = complianceRate;
	}

	void fullShutdown() {
		remainingFraction = 0d;
	}

	void open() {
		remainingFraction = 1d;
		requireMask = FaceMask.NONE;
	}

	Map<String, Object> asMap() {
		Map<String, Object> map = new HashMap<>();
		map.put("fraction", remainingFraction);
		map.put("exposure", exposure);
		map.put("mask", requireMask != null ? requireMask.name() : null);
		map.put("compliance", complianceRate);
		return map;
	}

}
