package com.redhat.chartgeneration.config;

import com.redhat.chartgeneration.common.FieldSelector;
import com.redhat.chartgeneration.graphcalc.GraphCalculation;

public class LineConfigRule {
	private String labelPattern;
	private String seriesLabelFormat;
	private FieldSelector labelField;
	private FieldSelector xField;
	private FieldSelector yField;
	private GraphCalculation calculation;
	private boolean showLines;
	private boolean showBars;
	private String unit;
	private boolean showUnit;

	public LineConfigRule() {

	}

	public LineConfigRule(String labelPattern, String seriesLabelFormat,
			String unit, FieldSelector labelField, FieldSelector xField,
			FieldSelector yField, GraphCalculation calculation) {
		this(labelPattern, seriesLabelFormat, unit, labelField, xField, yField,
				calculation, true, false, false);
	}

	public LineConfigRule(String labelPattern, String seriesLabelFormat,
			String unit, FieldSelector labelField, FieldSelector xField,
			FieldSelector yField, GraphCalculation calculation,
			boolean showLines, boolean showBars, boolean showUnit) {
		this.labelPattern = labelPattern;
		this.seriesLabelFormat = seriesLabelFormat;
		this.labelField = labelField;
		this.xField = xField;
		this.yField = yField;
		this.calculation = calculation;
		this.showBars = showBars;
		this.showLines = showLines;
		this.unit = unit;
		this.showUnit = showUnit;
	}

	public String getLabelPattern() {
		return labelPattern;
	}

	public void setLabelPattern(String labelPattern) {
		this.labelPattern = labelPattern;
	}

	public String getSeriesLabelFormat() {
		return seriesLabelFormat;
	}

	public void setSeriesLabelFormat(String seriesLabelFormat) {
		this.seriesLabelFormat = seriesLabelFormat;
	}

	public FieldSelector getLabelField() {
		return labelField;
	}

	public void setLabelField(FieldSelector labelField) {
		this.labelField = labelField;
	}

	public FieldSelector getXField() {
		return xField;
	}

	public void setXField(FieldSelector xField) {
		this.xField = xField;
	}

	public FieldSelector getYField() {
		return yField;
	}

	public void setYField(FieldSelector yField) {
		this.yField = yField;
	}

	public GraphCalculation getCalculation() {
		return calculation;
	}

	public void setCalculation(GraphCalculation calculation) {
		this.calculation = calculation;
	}

	public boolean isShowLines() {
		return showLines;
	}

	public void setShowLines(boolean showLines) {
		this.showLines = showLines;
	}

	public boolean isShowBars() {
		return showBars;
	}

	public void setShowBars(boolean showBars) {
		this.showBars = showBars;
	}

	public String getUnit() {
		return unit;
	}

	public void setUnit(String unit) {
		this.unit = unit;
	}

	public boolean isShowUnit() {
		return showUnit;
	}

	public void setShowUnit(boolean showUnit) {
		this.showUnit = showUnit;
	}

}
