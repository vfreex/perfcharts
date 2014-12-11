package com.redhat.chartgeneration.configtemplate;

import java.util.ArrayList;
import java.util.List;

import com.redhat.chartgeneration.common.FieldSelector;
import com.redhat.chartgeneration.common.IndexFieldSelector;
import com.redhat.chartgeneration.config.AxisMode;
import com.redhat.chartgeneration.config.LineConfigRule;
import com.redhat.chartgeneration.config.LineGraphConfig;
import com.redhat.chartgeneration.graphcalc.AverageCalculation;
import com.redhat.chartgeneration.graphcalc.SumBySeriesCalculation;

public class NMONDiskIOChartTemplate extends BaseChartTemplateWithInterval {

	@Override
	public LineGraphConfig generateGraphConfig() {
		int interval = getInterval();
		FieldSelector timestampField = new IndexFieldSelector(1);
		FieldSelector labelField = getLabelField();
		FieldSelector diskIOField = new IndexFieldSelector(2);
		List<LineConfigRule> rules = new ArrayList<LineConfigRule>();
		rules.add(new LineConfigRule("^DISKREAD-(.+)$", "Read-$1", "KiB/s",
				labelField, timestampField, diskIOField,
				new AverageCalculation(interval)));
		rules.add(new LineConfigRule("^DISKREAD-(.+)$", "Total Read", "KiB/s",
				labelField, timestampField, diskIOField, new SumBySeriesCalculation(
						labelField, interval)));
		rules.add(new LineConfigRule("^DISKWRITE-(.+)$", "Write-$1", "KiB/s",
				labelField, timestampField, diskIOField,
				new AverageCalculation(interval)));
		rules.add(new LineConfigRule("^DISKWRITE-(.+)$", "Total Write", "KiB/s",
				labelField, timestampField, diskIOField, new SumBySeriesCalculation(
						labelField, interval)));
		return createConfig("Disk IO over Time / (KiB/s)", "time", "KiB/s", rules, AxisMode.TIME);
	}

}
